import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Download, ExternalLink } from 'lucide-react'
import api, { downloadBlob, errMsg } from '../api'
import { ErrorBox, Field, Modal, OrderProgress, PageLoader, StatusBadge } from '../components/ui'
import { useAsync, useStoreConfig } from '../lib/hooks'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useDialog, useToast } from '../context/ToastContext'
import { fmtDate, inr, statusLabel } from '../lib/format'
import { startPayment } from '../lib/pay'

export const CANCELLABLE = ['PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING']

export function OrderBody({ o, timeline }) {
  const tracking = Array.isArray(timeline) ? null : timeline
  const events = Array.isArray(timeline) ? timeline : timeline.events
  return (
    <>
      <div className="card mt-4 p-4">
        <h2 className="mb-2 font-bold">Items</h2>
        <ul className="divide-y divide-twill-dark">
          {o.items.map((i, idx) => (
            <li key={idx} className="flex justify-between gap-3 py-2 text-sm">
              <span>{i.productName}<br /><span className="text-ink-faint">{[i.color, i.size].filter(Boolean).join(' / ')} · {i.sku} · {i.quantity} × {inr(i.unitPrice)}</span></span>
              <b>{inr(i.lineTotal)}</b>
            </li>
          ))}
        </ul>
        <dl className="mt-3 space-y-1 border-t-2 border-dashed border-twill-dark pt-3 text-sm">
          <div className="flex justify-between"><dt>Subtotal</dt><dd>{inr(o.subtotal)}</dd></div>
          {Number(o.discount) > 0 && <div className="flex justify-between text-moss"><dt>Discount {o.couponCode && `(${o.couponCode})`}</dt><dd>−{inr(o.discount)}</dd></div>}
          <div className="flex justify-between"><dt>GST</dt><dd>{inr(o.tax)}</dd></div>
          <div className="flex justify-between"><dt>Shipping</dt><dd>{Number(o.shipping) === 0 ? 'Free' : inr(o.shipping)}</dd></div>
          <div className="flex justify-between text-base font-extrabold"><dt>Total</dt><dd>{inr(o.total)}</dd></div>
        </dl>
      </div>
      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <div className="card p-4 text-sm">
          <h2 className="mb-1 font-bold">Delivery address</h2>
          {o.address.name}<br />{o.address.line1}{o.address.line2 ? `, ${o.address.line2}` : ''}<br />{o.address.city}, {o.address.state} {o.address.pincode}<br />Phone: {o.address.phone}
        </div>
        <div className="card p-4 text-sm">
          <h2 className="mb-1 font-bold">Shipment</h2>
          {o.trackingNo ? <>Courier: {o.courier}<br />Tracking no: <b>{o.trackingNo}</b>{o.trackingUrl && <><br /><a href={o.trackingUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 font-bold underline">Track with courier <ExternalLink size={13} /></a></>}</> : <span className="text-ink-faint">Not shipped yet.</span>}
        </div>
      </div>
      <div className="card mt-4 p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="font-bold">{tracking ? 'Shipment tracking' : 'Order history'}</h2>
          {tracking && <span className="text-sm text-ink-soft">
            Current: <b>{statusLabel(tracking.status)}</b>
            {tracking.expectedDelivery && <> · Expected delivery: <b>{fmtDate(tracking.expectedDelivery)}</b></>}
          </span>}
        </div>
        <ol className="space-y-3 border-l-2 border-dashed border-ink/30 pl-4">
          {events.map((t, i) => (
            <li key={i} className="relative text-sm">
              <span className="absolute -left-[23px] top-1 h-3 w-3 rounded-full bg-ink" />
              <b>{statusLabel(t.status)}</b> <span className="text-ink-faint">· {fmtDate(t.occurredAt || t.at)}</span>
              {(t.description || t.note) && <p className="text-ink-soft">{t.description || t.note}</p>}
            </li>
          ))}
        </ol>
        {events.length === 0 && <p className="text-sm text-ink-faint">Shipment tracking updates will appear here once the order ships.</p>}
      </div>
    </>
  )
}

export default function OrderDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const { refresh: refreshCart } = useCart()
  const cfg = useStoreConfig()
  const toast = useToast()
  const { confirm } = useDialog()
  const { data, loading, error, reload } = useAsync(async () => {
    const [o, t, r] = await Promise.all([
      api.get(`/orders/${id}`),
      api.get(`/orders/${id}/tracking`),
      api.get(`/orders/${id}/returns`),
    ])
    return { o: o.data, t: t.data, returns: r.data }
  }, [id])
  const [returning, setReturning] = useState(false)
  const [returnForm, setReturnForm] = useState({ orderItemId: '', type: 'REFUND', reason: '', quantity: 1, upiId: '' })
  const [busy, setBusy] = useState(false)

  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  const { o, t, returns } = data
  const returnableItems = o.items.filter((item) => item.returnable)

  const run = async (fn, okMsg) => {
    setBusy(true)
    try { await fn(); if (okMsg) toast(okMsg); reload() } catch (e) {
      if (e.code === 'PAYMENT_FAILED_CANCELLED') {
        sessionStorage.removeItem('cs_checkout_request_key')
        await refreshCart()
        toast('Payment failed. The order was cancelled and the items are back in your cart.', 'error')
        reload()
      } else if (e.message !== 'cancelled') {
        toast(errMsg(e), 'error')
      }
    } finally { setBusy(false) }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <Link to="/orders" className="text-sm font-bold underline underline-offset-4">← All orders</Link>
      <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
        <div><h1 className="text-2xl font-extrabold">{o.orderNo}</h1><p className="text-sm text-ink-soft">Placed {fmtDate(o.createdAt)}</p></div>
        <StatusBadge status={o.status} />
      </div>
      <div className="card mt-4 p-4"><OrderProgress status={o.status} />{!['CONFIRMED','PROCESSING','PACKED','SHIPPED','OUT_FOR_DELIVERY','DELIVERED'].includes(o.status) && <p className="text-sm text-ink-soft">Payment: <StatusBadge status={o.paymentStatus} /></p>}</div>

      <div className="mt-4 flex flex-wrap gap-2">
        {o.status === 'PENDING_PAYMENT' && <button className="btn-thread" disabled={busy} onClick={() => run(async () => { await startPayment(o, user, cfg?.storeName, confirm); sessionStorage.removeItem('cs_checkout_request_key'); await refreshCart() }, 'Payment received. Thank you!')}>Pay {inr(o.total)} now</button>}
        {CANCELLABLE.includes(o.status) && <button className="btn-outline" disabled={busy} onClick={async () => { if (await confirm('Cancel this order?')) await run(() => api.post(`/orders/${id}/cancel`), 'Order cancelled') }}>Cancel order</button>}
        {o.status === 'DELIVERED' && returnableItems.length > 0 && <button className="btn-outline" onClick={() => {
          setReturnForm({ orderItemId: String(returnableItems[0]?.id ?? ''), type: 'REFUND', reason: '', quantity: 1, upiId: '' })
          setReturning(true)
        }}>Return or replace an item</button>}
        {o.paymentStatus === 'PAID' && <button className="btn-ghost" onClick={() => downloadBlob(`/orders/${id}/invoice`, `invoice-${o.orderNo}.pdf`).catch((e) => toast(errMsg(e), 'error'))}><Download size={16} /> GST invoice</button>}
      </div>
      {o.status === 'PENDING_PAYMENT' && <p className="mt-2 text-xs text-ink-faint">Unpaid orders are cancelled automatically after 30 minutes and the stock is released.</p>}
      {o.paymentStatus === 'REFUND_PENDING' && <p className="mt-2 rounded-md bg-thread-light p-3 text-sm">Your refund is being processed. It usually reaches your account in 5–7 working days.</p>}

      {returns.length > 0 && (
        <section className="card mt-4 p-4">
          <h2 className="mb-2 font-bold">Returns and replacements</h2>
          <ul className="space-y-3">
            {returns.map((r) => (
              <li key={r.id} className="rounded-xl border border-ink/10 p-3 text-sm">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <b>{r.productName} · {r.quantity} item{r.quantity === 1 ? '' : 's'}</b>
                  <StatusBadge status={r.status} />
                </div>
                <p className="mt-1 text-ink-soft">{r.type === 'REPLACEMENT' ? 'Replacement' : 'Refund'} request · {r.reason}</p>
                {r.refundAmount != null && r.type === 'REFUND' && <p className="mt-1">Calculated refund: <b>{inr(r.refundAmount)}</b></p>}
                {r.awb && <p className="mt-1">Pickup tracking: <b>{r.courier} · {r.awb}</b>{r.trackingUrl && <> · <a className="font-bold underline" href={r.trackingUrl} target="_blank" rel="noreferrer">Track pickup</a></>}</p>}
                {r.adminNote && <p className="mt-1 text-ink-soft">{r.adminNote}</p>}
                {r.qcNote && <p className="mt-1 text-ink-soft">Quality check: {r.qcNote}</p>}
              </li>
            ))}
          </ul>
        </section>
      )}

      <OrderBody o={o} timeline={t} />

      {returning && (
        <Modal title="Return or replace an item" onClose={() => setReturning(false)}>
          <form onSubmit={(e) => {
            e.preventDefault()
            run(() => api.post('/returns', {
              orderId: o.publicId,
              orderItemId: Number(returnForm.orderItemId),
              type: returnForm.type,
              reason: returnForm.reason,
              quantity: Number(returnForm.quantity),
              upiId: returnForm.upiId.trim() || null,
            }), 'Request submitted').then(() => { setReturning(false); reload() })
          }} className="space-y-3">
            <Field label="Item">
              <select className="input" required value={returnForm.orderItemId} onChange={(e) => setReturnForm({ ...returnForm, orderItemId: e.target.value })}>
                {returnableItems.map((item) => <option key={item.id} value={item.id}>{item.productName} · {item.sku} (purchased: {item.quantity})</option>)}
              </select>
            </Field>
            <Field label="What would you like?">
              <select className="input" value={returnForm.type} onChange={(e) => setReturnForm({ ...returnForm, type: e.target.value })}>
                <option value="REFUND">Return for a refund</option>
                <option value="REPLACEMENT">Replace with the same item</option>
              </select>
            </Field>
            <Field label="Quantity">
              <input className="input" type="number" min="1" max={returnableItems.find((item) => String(item.id) === returnForm.orderItemId)?.quantity || 1} required value={returnForm.quantity} onChange={(e) => setReturnForm({ ...returnForm, quantity: e.target.value })} />
            </Field>
            <Field label="Reason"><textarea className="input" rows={3} required maxLength={1000} value={returnForm.reason} onChange={(e) => setReturnForm({ ...returnForm, reason: e.target.value })} /></Field>
            <Field label="UPI ID (required for COD refunds)">
              <input className="input" maxLength={100} placeholder="name@bank" value={returnForm.upiId} onChange={(e) => setReturnForm({ ...returnForm, upiId: e.target.value })} />
            </Field>
            <p className="text-xs text-ink-faint">Requests must be made within the return window and are checked against the product’s return policy. After approval, pickup and quality check are arranged.</p>
            <button className="btn-primary w-full" disabled={busy}>{busy ? 'Submitting…' : 'Submit request'}</button>
          </form>
        </Modal>
      )}
    </div>
  )
}
