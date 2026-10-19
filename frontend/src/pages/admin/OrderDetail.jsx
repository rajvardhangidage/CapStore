import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Download } from 'lucide-react'
import api, { downloadBlob, errMsg } from '../../api'
import { ErrorBox, Field, PageLoader, StatusBadge } from '../../components/ui'
import { OrderBody } from '../OrderDetail'
import { useAsync } from '../../lib/hooks'
import { useToast } from '../../context/ToastContext'
import { fmtDate, statusLabel } from '../../lib/format'
import { PageHeader } from './common'

const NEXT = ['CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURNED', 'REFUNDED']

export default function AdminOrderDetail() {
  const { id } = useParams()
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(async () => {
    const [o, t] = await Promise.all([api.get(`/admin/orders/${id}`), api.get(`/admin/orders/${id}/tracking`)])
    return { o: o.data, t: t.data }
  }, [id])
  const [st, setSt] = useState({ status: '', note: '' })

  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  const { o, t } = data

  const act = async (fn, msg) => { try { await fn(); toast(msg); reload() } catch (e) { toast(errMsg(e), 'error') } }
  const locked = o.status === 'CANCELLED' || o.status === 'PENDING_PAYMENT'

  return (
    <div className="max-w-4xl">
      <Link to="/admin/orders" className="text-sm font-bold underline underline-offset-4">← All orders</Link>
      <PageHeader title={o.orderNo} sub={`${fmtDate(o.createdAt)} · ${o.customerName} (${o.customerEmail})`}>
        <StatusBadge status={o.paymentStatus} /><StatusBadge status={o.status} />
        {o.paymentStatus === 'PAID' && <button className="btn-outline btn-sm" onClick={() => downloadBlob(`/admin/orders/${id}/invoice`, `invoice-${o.orderNo}.pdf`).catch((e) => toast(errMsg(e), 'error'))}><Download size={14} /> Invoice</button>}
      </PageHeader>

      {o.paymentStatus === 'REFUND_PENDING' && <p className="mb-4 rounded-md bg-thread-light p-3 text-sm"><b>Refund needed.</b> Refund this payment from your Razorpay dashboard, then set the status to “Refunded”.</p>}

      <div className="grid gap-4 md:grid-cols-2">
        <form className="card space-y-3 p-4" onSubmit={(e) => { e.preventDefault(); act(() => api.put(`/admin/orders/${id}/status`, st), 'Status updated').then(() => setSt({ status: '', note: '' })) }}>
          <h2 className="font-bold">Update status</h2>
          <Field label="New status"><select className="input" required disabled={locked} value={st.status} onChange={(e) => setSt({ ...st, status: e.target.value })}><option value="">Select…</option>{NEXT.map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}</select></Field>
          <Field label="Note to customer (optional)"><input className="input" disabled={locked} value={st.note} onChange={(e) => setSt({ ...st, note: e.target.value })} /></Field>
          <button className="btn-primary w-full" disabled={locked || !st.status}>Update & notify customer</button>
          {locked && <p className="text-xs text-ink-faint">{o.status === 'CANCELLED' ? 'Cancelled orders cannot be changed.' : 'Waiting for payment.'}</p>}
        </form>
        <form className="card space-y-3 p-4" onSubmit={(e) => { e.preventDefault(); const f = Object.fromEntries(new FormData(e.target)); act(() => api.put(`/admin/orders/${id}/shipment`, { ...f, trackingUrl: f.trackingUrl || null, expectedDelivery: f.expectedDelivery || null }), 'Shipment saved') }}>
          <h2 className="font-bold">Shipment & tracking</h2>
          <Field label="Courier"><input className="input" name="courier" required defaultValue={o.courier || ''} placeholder="Delhivery, DTDC…" /></Field>
          <Field label="Tracking number"><input className="input" name="trackingNo" required defaultValue={o.trackingNo || ''} /></Field>
          <Field label="Tracking link (optional)"><input className="input" name="trackingUrl" type="url" defaultValue={o.trackingUrl || ''} /></Field>
          <Field label="Expected delivery (optional)"><input className="input" name="expectedDelivery" type="date" defaultValue={t.expectedDelivery || ''} /></Field>
          <button className="btn-thread w-full" disabled={locked || o.paymentStatus !== 'PAID'}>Save & mark shipped</button>
        </form>
      </div>
      <OrderBody o={o} timeline={t} />
    </div>
  )
}
