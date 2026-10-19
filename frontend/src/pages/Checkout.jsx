import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { errMsg } from '../api'
import { EmptyState, Field, PageLoader } from '../components/ui'
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

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="mb-6 text-3xl font-extrabold">Checkout</h1>
      <form onSubmit={placeOrder} className="grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <h2 className="text-lg font-bold">Delivery address</h2>
          {addresses.data.map((a) => (
            <label key={a.id} className={`card flex cursor-pointer gap-3 p-4 ${sel === a.id ? 'ring-2 ring-ink' : ''}`}>
              <input type="radio" name="addr" checked={sel === a.id} onChange={() => setSel(a.id)} className="mt-1" />
              <span className="text-sm"><b>{a.name}</b> {a.label && <em className="text-ink-faint">({a.label})</em>}<br />{a.line1}{a.line2 ? `, ${a.line2}` : ''}<br />{a.city}, {a.state} {a.pincode}<br />Phone: {a.phone}</span>
            </label>
          ))}
          <label className={`card flex cursor-pointer gap-3 p-4 ${sel === 'new' ? 'ring-2 ring-ink' : ''}`}>
            <input type="radio" name="addr" checked={sel === 'new'} onChange={() => setSel('new')} className="mt-1" />
            <span className="text-sm font-bold">Use a new address</span>
          </label>
          {sel === 'new' && (
            <div className="card space-y-3 p-4">
              <AddressFields value={form} onChange={setForm} />
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={save} onChange={(e) => setSave(e.target.checked)} /> Save this address for next time</label>
            </div>
          )}
        </div>
        <aside className="card h-fit space-y-3 p-5">
          <h2 className="text-lg font-bold">Summary ({cart.items.length} {cart.items.length === 1 ? 'item' : 'items'})</h2>
          <ul className="max-h-48 space-y-1 overflow-y-auto text-sm">
            {cart.items.map((i) => <li key={i.key} className="flex justify-between gap-2"><span className="truncate">{i.quantity} × {i.productName}</span><span>{inr(i.lineTotal)}</span></li>)}
          </ul>
          <div className="seam-line" />
          <dl className="space-y-1.5 text-sm">
            <div className="flex justify-between"><dt>Subtotal</dt><dd>{inr(cart.subtotal)}</dd></div>
            {discount > 0 && <div className="flex justify-between text-moss"><dt>Coupon {coupon.code}</dt><dd>−{inr(discount)}</dd></div>}
            <div className="flex justify-between"><dt>GST</dt><dd>{inr(est.tax)}</dd></div>
            <div className="flex justify-between"><dt>Shipping</dt><dd>{est.shipping === 0 ? 'Free' : inr(est.shipping)}</dd></div>
            <div className="flex justify-between pt-1 text-base font-extrabold"><dt>Total</dt><dd>{inr(est.total)}</dd></div>
          </dl>
          <p className="text-xs text-ink-faint">The final amount is confirmed when your order is placed.</p>
          <button className="btn-thread w-full py-3" disabled={busy}>{busy ? 'Please wait…' : `Place order & pay ${inr(est.total)}`}</button>
          <p className="text-center text-xs text-ink-faint">Payments are processed securely by Razorpay.</p>
        </aside>
      </form>
    </div>
  )
}
