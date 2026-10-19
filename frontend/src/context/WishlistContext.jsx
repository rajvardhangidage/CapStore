import { createContext, useContext, useEffect, useState } from 'react'
import api, { errMsg } from '../api'
import { useAuth } from './AuthContext'
import { useToast } from './ToastContext'

const Ctx = createContext({ ids: new Set(), toggle: () => {} })
export const useWishlist = () => useContext(Ctx)

export function WishlistProvider({ children }) {
  const { user } = useAuth()
  const toast = useToast()
  const [ids, setIds] = useState(new Set())

  useEffect(() => {
    if (!user) { setIds(new Set()); return }
    api.get('/wishlist').then((r) => setIds(new Set(r.data.map((p) => p.id)))).catch(() => {})
  }, [user])

  const toggle = async (productId) => {
    if (!user) { toast('Log in to save caps to your wishlist', 'error'); return }
    const has = ids.has(productId)
    try {
      if (has) await api.delete(`/wishlist/${productId}`)
      else await api.post(`/wishlist/${productId}`)
      setIds((s) => { const n = new Set(s); has ? n.delete(productId) : n.add(productId); return n })
    } catch (e) { toast(errMsg(e), 'error') }
  }
  return <Ctx.Provider value={{ ids, toggle }}>{children}</Ctx.Provider>
}
