import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { errMsg } from '../api'
import { Lock, Plus } from 'lucide-react'
import { CapMark, colorFor, EmptyState, ErrorBox, Field, PageLoader, Spinner } from '../components/ui'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { useDialog, useToast } from '../context/ToastContext'
import { useAsync, useStoreConfig } from '../lib/hooks'
import { estimate, inr } from '../lib/format'
import { startPayment } from '../lib/pay'
import { getCoupon } from './Cart'

export const emptyAddress = { label: '', name: '', phone: '', line1: '', line2: '', city: '', state: '', pincode: '' }

export function AddressFields({ value, onChange }) {
  const f = (k) => ({ className: 'input', value: value[k] || '', onChange: (e) => onChange({ ...value, [k]: e.target.value }) })
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <Field label="Full name"><input {...f('name')} autoComplete="name" required /></Field>
      <Field label="Phone (10 digits)"><input {...f('phone')} inputMode="numeric" maxLength={10} autoComplete="tel" required /></Field>
      <div className="sm:col-span-2"><Field label="Address line 1"><input {...f('line1')} autoComplete="address-line1" required /></Field></div>
      <div className="sm:col-span-2"><Field label="Address line 2 (optional)"><input {...f('line2')} autoComplete="address-line2" /></Field></div>
      <Field label="City"><input {...f('city')} autoComplete="address-level2" required /></Field>
      <Field label="State"><input {...f('state')} autoComplete="address-level1" required /></Field>
      <Field label="Pincode"><input {...f('pincode')} inputMode="numeric" maxLength={6} autoComplete="postal-code" required /></Field>
      <Field label="Label (Home, Office…)"><input {...f('label')} /></Field>
    </div>
  )
}

export default function Checkout() {
  const { cart, refresh } = useCart()
  const { user } = useAuth()
  const cfg = useStoreConfig()
  const toast = useToast()
  const { confirm } = useDialog()
  const nav = useNavigate()
  const addresses = useAsync(() => api.get('/me/addresses').then((r) => r.data))
  const [sel, setSel] = useState(null) // address id or 'new'
  const [form, setForm] = useState({ ...emptyAddress, name: user?.name || '', phone: user?.phone || '' })
  const [save, setSave] = useState(true)
  const [busy, setBusy] = useState(false)
  const coupon = getCoupon()

  useEffect(() => {
    if (addresses.data && sel === null) setSel(addresses.data.length ? (addresses.data.find((a) => a.defaultAddress) || addresses.data[0]).id : 'new')
  }, [addresses.data, sel])

  if (addresses.error) return <ErrorBox message={addresses.error} onRetry={addresses.reload} />
  if (addresses.loading || sel === null) return <PageLoader />
  if (cart.items.length === 0) return <EmptyState title="Your cart is empty"><Link to="/products" className="btn-primary">Browse caps</Link></EmptyState>

  const discount = coupon?.discount || 0
  const est = estimate(cart.subtotal, discount, cfg)

  const placeOrder = async (e) => {
    e.preventDefault()
    setBusy(true)
    let order
    try {
      let a = sel === 'new' ? form : addresses.data.find((x) => x.id === sel)
      if (sel === 'new' && save) await api.post('/me/addresses', { ...form, defaultAddress: addresses.data.length === 0 })
      const checkoutRequestKey = sessionStorage.getItem('cs_checkout_request_key') || crypto.randomUUID()
      sessionStorage.setItem('cs_checkout_request_key', checkoutRequestKey)
      const { data } = await api.post('/orders', {
        shipName: a.name, shipPhone: a.phone, shipLine1: a.line1, shipLine2: a.line2 || null,
        shipCity: a.city, shipState: a.state, shipPincode: a.pincode,
        checkoutRequestKey, couponCode: coupon?.code || null,
      })
      order = data
    } catch (err) {
      toast(errMsg(err), 'error')
      setBusy(false)
      return
    }
    if (order.paymentStatus === 'PAID') {
      sessionStorage.removeItem('cs_checkout_request_key')
      nav(`/order-success/${order.publicId}`, { replace: true })
      setBusy(false)
      return
    }
    try {
      const paid = await startPayment(order, user, cfg?.storeName, confirm)
      sessionStorage.removeItem('cs_checkout_request_key')
      sessionStorage.removeItem('cs_coupon')
      await refresh()
      nav(`/order-success/${paid.publicId}`, { replace: true })
    } catch (err) {
      if (err.code === 'PAYMENT_FAILED_CANCELLED') {
        sessionStorage.removeItem('cs_checkout_request_key')
        await refresh()
        toast('Payment failed. The order was cancelled and the items are back in your cart.', 'error')
      } else {
        toast(err.message === 'cancelled' ? 'Payment not completed. Your order is saved, so you can pay from your orders page.' : errMsg(err), 'error')
      }
      nav(`/orders/${order.publicId}`, { replace: true })
    }
  }

  const tile = (on) => `flex cursor-pointer gap-3 rounded-md border p-4 transition-colors ${on ? 'border-primary bg-primary-soft/50 ring-1 ring-primary' : 'border-tertiary hover:border-ink/40'}`

  return (
    <div className="page py-10">
      <header className="mb-8 border-b border-tertiary pb-6">
        <ol className="mb-3 flex items-center gap-2 text-xs font-medium uppercase tracking-[0.08em] text-ink-faint" aria-label="Checkout steps">
          <li><Link to="/cart" className="hover:text-primary">Cart</Link></li><li aria-hidden="true">/</li>
          <li className="text-primary" aria-current="step">Delivery</li><li aria-hidden="true">/</li>
          <li>Payment</li>
        </ol>
        <h1 className="page-title">Checkout</h1>
      </header>
      <form onSubmit={placeOrder} className="grid gap-10 lg:grid-cols-[1fr_380px]">
        <div className="space-y-4">
          <h2 className="flex items-center gap-3 text-lg font-semibold"><span className="flex h-7 w-7 items-center justify-center rounded-full bg-primary text-xs text-white">1</span>Delivery address</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            {addresses.data.map((a) => (
              <label key={a.id} className={tile(sel === a.id)}>
                <input type="radio" name="addr" checked={sel === a.id} onChange={() => setSel(a.id)} className="mt-1" />
                <span className="text-sm leading-6">
                  <span className="flex items-center gap-2"><b>{a.name}</b>{a.label && <span className="rounded-full bg-twill px-2 py-0.5 text-[11px] font-medium uppercase tracking-[0.04em] text-ink-soft">{a.label}</span>}</span>
                  <span className="block text-ink-soft">{a.line1}{a.line2 ? `, ${a.line2}` : ''}<br />{a.city}, {a.state} {a.pincode}<br />{a.phone}</span>
                </span>
              </label>
            ))}
            <label className={`${tile(sel === 'new')} items-center`}>
              <input type="radio" name="addr" checked={sel === 'new'} onChange={() => setSel('new')} />
              <span className="flex items-center gap-2 text-sm font-medium"><Plus size={16} />Use a new address</span>
            </label>
          </div>
          {sel === 'new' && (
            <div className="animate-fade-up space-y-4 rounded-md border border-tertiary p-5">
              <AddressFields value={form} onChange={setForm} />
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={save} onChange={(e) => setSave(e.target.checked)} /> Save this address for next time</label>
            </div>
          )}
        </div>
        <aside className="h-fit space-y-5 rounded-md bg-twill p-6 lg:sticky lg:top-24">
          <h2 className="font-display text-xl font-semibold uppercase">Summary <span className="text-ink-faint">({cart.totalQuantity})</span></h2>
          <ul className="max-h-60 space-y-3 overflow-y-auto text-sm">
            {cart.items.map((i) => (
              <li key={i.key} className="flex items-center gap-3">
                <span className="relative flex h-14 w-12 shrink-0 items-center justify-center overflow-hidden rounded-sm bg-white">
                  {i.imageUrl ? <img src={i.imageUrl} alt="" className="h-full w-full object-cover" /> : <CapMark className="w-3/4" color={colorFor(i.productName)} />}
                  <span className="badge absolute -right-1 -top-1 min-w-[18px] justify-center px-1 text-[10px]">{i.quantity}</span>
                </span>
                <span className="min-w-0 flex-1"><span className="line-clamp-1 font-medium">{i.productName}</span><span className="text-xs text-ink-faint">{[i.color, i.size].filter(Boolean).join(' / ')}</span></span>
                <span className="shrink-0">{inr(i.lineTotal)}</span>
              </li>
            ))}
          </ul>
          <dl className="space-y-2.5 border-t border-tertiary pt-5 text-sm">
            <div className="flex justify-between"><dt className="text-ink-soft">Subtotal</dt><dd>{inr(cart.subtotal)}</dd></div>
            {discount > 0 && <div className="flex justify-between text-moss"><dt>Coupon {coupon.code}</dt><dd>−{inr(discount)}</dd></div>}
            <div className="flex justify-between"><dt className="text-ink-soft">GST</dt><dd>{inr(est.tax)}</dd></div>
            <div className="flex justify-between"><dt className="text-ink-soft">Shipping</dt><dd>{est.shipping === 0 ? <span className="font-medium text-moss">Free</span> : inr(est.shipping)}</dd></div>
            <div className="flex justify-between border-t border-tertiary pt-3 text-lg font-semibold"><dt>Total</dt><dd>{inr(est.total)}</dd></div>
          </dl>
          <p className="text-xs text-ink-faint">The final amount is confirmed when your order is placed.</p>
          <button className="btn-primary btn-lg w-full" disabled={busy}>{busy ? <><Spinner className="h-4 w-4" /> Processing…</> : <><Lock size={16} /> Pay {inr(est.total)}</>}</button>
          <p className="text-center text-xs text-ink-faint">UPI, cards, netbanking & wallets via Razorpay.</p>
        </aside>
      </form>
    </div>
  )
}
