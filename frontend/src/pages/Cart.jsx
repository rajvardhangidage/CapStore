import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, Lock, Minus, Plus, Tag, Trash2, X } from 'lucide-react'
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

  const freeAbove = cfg?.freeShippingAbove
  const taxable = Math.max(cart.subtotal - discount, 0)
  const toFree = freeAbove != null ? Math.max(Number(freeAbove) - taxable, 0) : null

  return (
    <div className="page py-10">
      <header className="mb-8 flex items-end justify-between gap-4 border-b border-tertiary pb-6">
        <div>
          <p className="eyebrow mb-2">{cart.totalQuantity} {cart.totalQuantity === 1 ? 'item' : 'items'}</p>
          <h1 className="page-title">Your cart</h1>
        </div>
        <Link to="/products" className="link hidden text-sm sm:inline">Continue shopping</Link>
      </header>
      <div className="grid gap-10 lg:grid-cols-[1fr_380px]">
        <div>
          {toFree != null && (
            <div className="mb-6 rounded-md bg-primary-soft p-4">
              <p className="text-sm font-medium text-primary">
                {toFree > 0 ? <>Add <b>{inr(toFree)}</b> more for free shipping</> : 'You’ve unlocked free shipping'}
              </p>
              <div className="mt-2.5 h-1.5 overflow-hidden rounded-full bg-white" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(Math.min(taxable / Number(freeAbove || 1), 1) * 100)} aria-label="Progress to free shipping">
                <div className="h-full rounded-full bg-primary transition-all duration-500" style={{ width: `${Math.min(taxable / Number(freeAbove || 1), 1) * 100}%` }} />
              </div>
            </div>
          )}
          <ul className="divide-y divide-tertiary border-y border-tertiary">
            {cart.items.map((i) => (
              <li key={i.key} className="flex gap-4 py-5 sm:gap-6">
                <Link to={`/products/${i.productPublicId}`} className="flex h-28 w-24 shrink-0 items-center justify-center overflow-hidden rounded-md bg-twill sm:h-32 sm:w-28">
                  {i.imageUrl ? <img src={i.imageUrl} alt="" className="h-full w-full object-cover" /> : <CapMark className="w-3/4" color={colorFor(i.productName)} />}
                </Link>
                <div className="flex min-w-0 flex-1 flex-col">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <Link to={`/products/${i.productPublicId}`} className="line-clamp-2 font-medium hover:text-primary">{i.productName}</Link>
                      {[i.color, i.size].some(Boolean) && <p className="mt-0.5 text-sm text-ink-soft">{[i.color, i.size].filter(Boolean).join(' / ')}</p>}
                      <p className="mt-0.5 text-sm text-ink-faint">{inr(i.unitPrice)} each</p>
                    </div>
                    <p className="shrink-0 font-semibold">{inr(i.lineTotal)}</p>
                  </div>
                  <div className="mt-auto flex items-center justify-between pt-3">
                    <div className="flex h-10 items-center rounded-sm border border-tertiary">
                      <button className="flex h-full w-9 items-center justify-center hover:text-primary disabled:opacity-40" disabled={i.quantity <= 1} onClick={() => setQty(i, i.quantity - 1)} aria-label={`Decrease quantity of ${i.productName}`}><Minus size={14} /></button>
                      <span className="w-8 text-center text-sm font-semibold" aria-live="polite">{i.quantity}</span>
                      <button className="flex h-full w-9 items-center justify-center hover:text-primary disabled:opacity-40" disabled={i.quantity >= i.availableStock} onClick={() => setQty(i, i.quantity + 1)} aria-label={`Increase quantity of ${i.productName}`}><Plus size={14} /></button>
                    </div>
                    <button onClick={() => remove(i)} className="flex items-center gap-1.5 text-sm text-ink-faint hover:text-brim" aria-label={`Remove ${i.productName}`}><Trash2 size={16} /><span className="hidden sm:inline">Remove</span></button>
                  </div>
                  {i.quantity >= i.availableStock && <p className="mt-2 text-xs text-warn">Maximum available quantity in your cart.</p>}
                </div>
              </li>
            ))}
          </ul>
        </div>

        <aside className="h-fit space-y-5 rounded-md bg-twill p-6 lg:sticky lg:top-24">
          <h2 className="font-display text-xl font-semibold uppercase">Order summary</h2>
          {user ? (
            coupon ? (
              <div className="flex items-center justify-between rounded-sm border border-moss/20 bg-moss-light px-3 py-2.5 text-sm font-medium text-moss">
                <span className="flex items-center gap-2"><Tag size={14} />{coupon.code} applied</span>
                <button onClick={removeCoupon} aria-label="Remove coupon" className="hover:text-brim"><X size={16} /></button>
              </div>
            ) : (
              <form onSubmit={applyCoupon} className="flex gap-2">
                <input className="input uppercase placeholder:normal-case" placeholder="Coupon code" value={code} onChange={(e) => setCode(e.target.value)} aria-label="Coupon code" />
                <button className="btn-outline" disabled={!code.trim()}>Apply</button>
              </form>
            )
          ) : <p className="text-sm text-ink-soft"><Link to="/login" state={{ from: '/cart' }} className="link">Log in</Link> to use a coupon.</p>}
          {user && !coupon && (availableCoupons.loading || availableCoupons.error || availableCoupons.data?.length > 0) && (
            <section className="space-y-2" aria-label="Available coupons">
              <h3 className="eyebrow">Available coupons</h3>
              {availableCoupons.loading ? <div className="skeleton h-14" />
                : availableCoupons.error ? <p className="text-xs text-brim">{availableCoupons.error}</p>
                  : availableCoupons.data.map((offer) => (
                    <div key={offer.code} className="flex items-center justify-between gap-3 rounded-sm border border-dashed border-primary/30 bg-white p-3">
                      <div className="min-w-0 text-xs">
                        <p className="font-semibold text-primary">{offer.code}</p>
                        <p className="text-ink-soft">Save up to {inr(couponSavings(offer, cart.subtotal))}{offer.description ? ` · ${offer.description}` : ''}</p>
                      </div>
                      <button type="button" className="btn-outline btn-sm shrink-0" onClick={() => { setCode(offer.code); applyCoupon(null, offer.code) }}>Apply</button>
                    </div>
                  ))}
            </section>
          )}
          <dl className="space-y-2.5 border-t border-tertiary pt-5 text-sm">
            <div className="flex justify-between"><dt className="text-ink-soft">Subtotal</dt><dd>{inr(cart.subtotal)}</dd></div>
            {discount > 0 && <div className="flex justify-between text-moss"><dt>Discount</dt><dd>−{inr(discount)}</dd></div>}
            <div className="flex justify-between"><dt className="text-ink-soft">GST ({cfg?.gstRatePercent ?? 5}%)</dt><dd>{inr(est.tax)}</dd></div>
            <div className="flex justify-between"><dt className="text-ink-soft">Shipping</dt><dd>{est.shipping === 0 ? <span className="font-medium text-moss">Free</span> : inr(est.shipping)}</dd></div>
            <div className="flex justify-between border-t border-tertiary pt-3 text-lg font-semibold"><dt>Estimated total</dt><dd>{inr(est.total)}</dd></div>
          </dl>
          <button className="btn-primary btn-lg w-full" onClick={() => (user ? nav('/checkout') : nav('/login', { state: { from: '/checkout' } }))}>
            {user ? 'Checkout' : 'Log in to checkout'} <ArrowRight size={18} />
          </button>
          <p className="flex items-center justify-center gap-1.5 text-xs text-ink-faint"><Lock size={12} /> Secure payments by Razorpay</p>
        </aside>
      </div>
    </div>
  )
}
