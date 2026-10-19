import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api, { errMsg } from '../api'
import { useAuth } from './AuthContext'
import { useToast } from './ToastContext'

const CartCtx = createContext(null)
export const useCart = () => useContext(CartCtx)

const GUEST = 'cs_guest_cart'
const readGuest = () => { try { return JSON.parse(localStorage.getItem(GUEST) || '[]') } catch { return [] } }

const fromServer = (c) => ({
  items: c.items.map((i) => ({ ...i, key: 'i' + i.itemId })),
  totalQuantity: c.totalQuantity,
  subtotal: Number(c.subtotal),
})

export function CartProvider({ children }) {
  const { user } = useAuth()
  const toast = useToast()
  const [server, setServer] = useState({ items: [], totalQuantity: 0, subtotal: 0 })
  const [guest, setGuest] = useState(readGuest)
  const [busy, setBusy] = useState(false)

  useEffect(() => { localStorage.setItem(GUEST, JSON.stringify(guest)) }, [guest])

  const refresh = useCallback(async () => {
    if (!user) return
    try { setServer(fromServer((await api.get('/cart')).data)) } catch { /* ignore */ }
  }, [user])

  // On login: merge the guest cart into the account cart, then load it
  useEffect(() => {
    if (!user) { setServer({ items: [], totalQuantity: 0, subtotal: 0 }); return }
    ;(async () => {
      const pending = readGuest()
      if (pending.length) {
        for (const g of pending) {
          try { await api.post('/cart/items', { variantId: g.variantId, quantity: g.quantity }) } catch { /* out of stock etc. */ }
        }
        setGuest([])
      }
      await refresh()
    })()
  }, [user, refresh])

  const guestView = useMemo(() => {
    const items = guest.map((g) => ({ ...g, key: 'g' + g.variantId, lineTotal: g.unitPrice * g.quantity }))
    return { items, totalQuantity: items.reduce((s, i) => s + i.quantity, 0), subtotal: items.reduce((s, i) => s + i.lineTotal, 0) }
  }, [guest])

  const cart = user ? server : guestView

  const add = async (product, variant, quantity = 1) => {
    setBusy(true)
    try {
      if (user) {
        setServer(fromServer((await api.post('/cart/items', { variantId: variant.id, quantity })).data))
      } else {
        setGuest((g) => {
          const ex = g.find((x) => x.variantId === variant.id)
          const qty = Math.min((ex?.quantity || 0) + quantity, variant.stockQty)
          const row = {
            variantId: variant.id, productId: product.id, productName: product.name, sku: variant.sku, size: variant.size,
            color: variant.color, imageUrl: product.imageUrl, availableStock: variant.stockQty,
            unitPrice: Number(variant.price ?? product.basePrice), quantity: qty,
          }
          return ex ? g.map((x) => (x.variantId === variant.id ? row : x)) : [...g, row]
        })
      }
      toast('Added to cart')
    } catch (e) { toast(errMsg(e), 'error') } finally { setBusy(false) }
  }

  const setQty = async (item, quantity) => {
    try {
      if (user) setServer(fromServer((await api.put(`/cart/items/${item.itemId}`, { quantity })).data))
      else setGuest((g) => g.map((x) => (x.variantId === item.variantId ? { ...x, quantity: Math.max(1, Math.min(quantity, x.availableStock)) } : x)))
    } catch (e) { toast(errMsg(e), 'error') }
  }

  const remove = async (item) => {
    try {
      if (user) setServer(fromServer((await api.delete(`/cart/items/${item.itemId}`)).data))
      else setGuest((g) => g.filter((x) => x.variantId !== item.variantId))
    } catch (e) { toast(errMsg(e), 'error') }
  }

  return <CartCtx.Provider value={{ cart, add, setQty, remove, refresh, busy }}>{children}</CartCtx.Provider>
}
