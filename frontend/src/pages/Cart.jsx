import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Minus, Plus, Tag, Trash2, X } from 'lucide-react'
import api, { errMsg } from '../api'
import { CapMark, colorFor, EmptyState } from '../components/ui'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { useAsync, useStoreConfig } from '../lib/hooks'
import { estimate, inr } from '../lib/format'

export const getCoupon = () => { try { return JSON.parse(sessionStorage.getItem('cs_coupon')) } catch { return null } }

const couponSavings = (offer, subtotal) => {
  const raw = offer.discountType === 'PERCENT'
    ? subtotal * Number(offer.discountValue) / 100
    : Number(offer.discountValue)
  return Math.min(raw, Number(offer.maxDiscount ?? raw), subtotal)
}

export default function Cart() {
  const { cart, setQty, remove } = useCart()
  const { user } = useAuth()
  const cfg = useStoreConfig()
  const toast = useToast()
  const nav = useNavigate()
  const [code, setCode] = useState('')
  const [coupon, setCoupon] = useState(getCoupon())
  const availableCoupons = useAsync(
    () => user ? api.get('/coupons/available').then((r) => r.data) : Promise.resolve([]),
    [user?.id, cart.subtotal],
  )

  if (cart.items.length === 0)
    return <EmptyState title="Your cart is empty" text="Add a few caps and they will show up here."><Link to="/products" className="btn-primary">Browse caps</Link></EmptyState>

  const applyCoupon = async (e, selectedCode = code) => {
    e?.preventDefault()
    try {
      const { data } = await api.post('/coupons/validate', { code: selectedCode })
      const c = { code: data.code, discount: Number(data.discount) }
      sessionStorage.setItem('cs_coupon', JSON.stringify(c))
      setCoupon(c)
      setCode('')
      toast(`Coupon ${c.code} applied`)
    } catch (err) { toast(errMsg(err), 'error') }
  }
  const removeCoupon = () => { sessionStorage.removeItem('cs_coupon'); setCoupon(null) }

  const discount = coupon && user ? coupon.discount : 0
  const est = estimate(cart.subtotal, discount, cfg)

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="mb-6 text-3xl font-extrabold">Your cart</h1>
      <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
        <ul className="space-y-3">
          {cart.items.map((i) => (
            <li key={i.key} className="card flex gap-4 p-3">
              <Link to={`/products/${i.productPublicId}`} className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded bg-twill">
                {i.imageUrl ? <img src={i.imageUrl} alt="" className="h-full w-full object-cover" /> : <CapMark className="w-3/4" color={colorFor(i.productName)} />}
              </Link>
              <div className="flex min-w-0 flex-1 flex-col">
                <Link to={`/products/${i.productPublicId}`} className="truncate font-bold hover:underline">{i.productName}</Link>
                <p className="text-sm text-ink-soft">{[i.color, i.size].filter(Boolean).join(' / ')}</p>
                <p className="text-sm">{inr(i.unitPrice)} each</p>
                <div className="mt-auto flex items-center justify-between pt-2">
                  <div className="flex items-center rounded-md border-2 border-twill-dark">
                    <button className="p-1.5" onClick={() => setQty(i, i.quantity - 1)} aria-label="Decrease"><Minus size={14} /></button>
                    <span className="w-8 text-center text-sm font-bold">{i.quantity}</span>
                    <button className="p-1.5" disabled={i.quantity >= i.availableStock} onClick={() => setQty(i, i.quantity + 1)} aria-label="Increase"><Plus size={14} /></button>
                  </div>
                  <b>{inr(i.lineTotal)}</b>
                  <button onClick={() => remove(i)} className="p-1.5 text-ink-faint hover:text-brim" aria-label={`Remove ${i.productName}`}><Trash2 size={18} /></button>
                </div>
              </div>
            </li>
          ))}
        </ul>

        <aside className="card h-fit space-y-3 p-5">
          <h2 className="text-lg font-bold">Order summary</h2>
          {user ? (
            coupon ? (
              <div className="flex items-center justify-between rounded-md bg-moss-light px-3 py-2 text-sm font-bold text-moss">
                <span className="flex items-center gap-2"><Tag size={14} />{coupon.code}</span>
                <button onClick={removeCoupon} aria-label="Remove coupon"><X size={16} /></button>
              </div>
            ) : (
              <form onSubmit={applyCoupon} className="flex gap-2">
                <input className="input" placeholder="Coupon code" value={code} onChange={(e) => setCode(e.target.value)} aria-label="Coupon code" />
                <button className="btn-outline btn-sm" disabled={!code.trim()}>Apply</button>
              </form>
            )
          ) : <p className="text-xs text-ink-faint">Log in to use a coupon and see your business pricing.</p>}
          {user && !coupon && (
            <section className="space-y-2" aria-label="Available coupons">
              <h3 className="text-xs font-bold uppercase tracking-wide text-ink-soft">Available coupons</h3>
              {availableCoupons.loading ? <p className="text-xs text-ink-faint">Checking coupons…</p>
                : availableCoupons.error ? <p className="text-xs text-brim">{availableCoupons.error}</p>
                  : availableCoupons.data?.length ? availableCoupons.data.map((offer) => (
                    <div key={offer.code} className="flex items-center justify-between gap-2 rounded-md border border-dashed border-ink/20 p-2">
                      <div className="min-w-0 text-xs">
                        <p className="font-extrabold">{offer.code} · Save up to {inr(couponSavings(offer, cart.subtotal))}</p>
                        {offer.description && <p className="text-ink-soft">{offer.description}</p>}
                      </div>
                      <button type="button" className="btn-outline btn-sm shrink-0" onClick={() => { setCode(offer.code); applyCoupon(null, offer.code) }}>Use</button>
                    </div>
                  )) : <p className="text-xs text-ink-faint">No coupons currently available for this cart.</p>}
            </section>
          )}
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between"><dt>Subtotal</dt><dd>{inr(cart.subtotal)}</dd></div>
            {discount > 0 && <div className="flex justify-between text-moss"><dt>Discount</dt><dd>−{inr(discount)}</dd></div>}
            <div className="flex justify-between"><dt>GST ({cfg?.gstRatePercent ?? 5}%)</dt><dd>{inr(est.tax)}</dd></div>
            <div className="flex justify-between"><dt>Shipping</dt><dd>{est.shipping === 0 ? 'Free' : inr(est.shipping)}</dd></div>
            <div className="seam-line" />
            <div className="flex justify-between text-base font-extrabold"><dt>Total</dt><dd>{inr(est.total)}</dd></div>
          </dl>
          {cfg && est.shipping > 0 && <p className="text-xs text-ink-faint">Free shipping on orders above {inr(cfg.freeShippingAbove)}.</p>}
          <button className="btn-primary w-full" onClick={() => (user ? nav('/checkout') : nav('/login', { state: { from: '/checkout' } }))}>
            {user ? 'Proceed to checkout' : 'Log in to checkout'}
          </button>
          <Link to="/products" className="block text-center text-sm font-bold underline underline-offset-4">Continue shopping</Link>
        </aside>
      </div>
    </div>
  )
}
