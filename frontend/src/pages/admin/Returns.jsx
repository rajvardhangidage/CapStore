import { useState } from 'react'
import api, { errMsg } from '../../api'
import { ErrorBox, Field, PageLoader, StatusBadge } from '../../components/ui'
import { useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { fmtDate, inr, statusLabel } from '../../lib/format'
import { PageHeader } from './common'

export default function AdminReturns() {
  const toast = useToast()
  const [busyId, setBusyId] = useState(null)
  const [notes, setNotes] = useState({})
  const { data, loading, error, reload } = useAsync(
    () => api.get('/admin/returns').then((r) => r.data), [],
  )

  const act = async (request, action, body) => {
    setBusyId(request.id)
    try {
      await api.post(`/admin/returns/${request.id}/${action}`, body)
      toast('Return request updated')
      reload()
    } catch (e) {
      toast(errMsg(e), 'error')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <PageHeader title="Returns and replacements" sub="Approve pickup requests, record inspection results, and handle manual COD refunds." />
      {loading ? <PageLoader /> : error ? <ErrorBox message={error} onRetry={reload} /> : data.length === 0 ? (
        <div className="card p-8 text-center text-sm text-ink-faint">No return or replacement requests yet.</div>
      ) : (
        <div className="space-y-3">
          {data.map((r) => (
            <article key={r.id} className="card space-y-3 p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="font-bold">{r.productName} · {r.quantity} item{r.quantity === 1 ? '' : 's'}</h2>
                  <p className="text-sm text-ink-soft">{r.type === 'REPLACEMENT' ? 'Replacement' : 'Refund'} · Order <span className="font-mono text-xs">{r.orderId}</span> · {fmtDate(r.createdAt)}</p>
                </div>
                <StatusBadge status={r.status} />
              </div>
              <p className="text-sm">Reason: {r.reason}</p>
              {r.type === 'REFUND' && <p className="text-sm">Calculated refund: <b>{inr(r.refundAmount)}</b></p>}
              {r.awb && <p className="text-sm">Pickup: {r.courier} · <b>{r.awb}</b>{r.trackingUrl && <> · <a href={r.trackingUrl} target="_blank" rel="noreferrer" className="font-bold underline">Track</a></>}</p>}
              {r.upiId && <p className="text-sm">Customer UPI: <b>{r.upiId}</b></p>}
              {r.adminNote && <p className="text-sm text-ink-soft">Admin note: {r.adminNote}</p>}
              {r.qcNote && <p className="text-sm text-ink-soft">QC note: {r.qcNote}</p>}
              <Field label={r.status === 'RECEIVED' ? 'Inspection note' : 'Decision note'}>
                <input className="input" value={notes[r.id] || ''} onChange={(e) => setNotes({ ...notes, [r.id]: e.target.value })} maxLength={500} />
              </Field>
              <div className="flex flex-wrap gap-2">
                {r.status === 'REQUESTED' && <>
                  <button className="btn-primary btn-sm" disabled={busyId === r.id} onClick={() => act(r, 'approve', { note: notes[r.id] || 'Approved' })}>Approve and schedule pickup</button>
                  <button className="btn-outline btn-sm" disabled={busyId === r.id} onClick={() => act(r, 'reject', { note: notes[r.id] || 'Rejected' })}>Reject</button>
                </>}
                {r.status === 'RECEIVED' && <>
                  <button className="btn-primary btn-sm" disabled={busyId === r.id} onClick={() => act(r, 'quality-check', { passed: true, note: notes[r.id] || 'Passed inspection' })}>{r.type === 'REPLACEMENT' ? 'Pass QC and create replacement' : 'Pass QC and initiate refund'}</button>
                  <button className="btn-outline btn-sm" disabled={busyId === r.id} onClick={() => act(r, 'quality-check', { passed: false, note: notes[r.id] || 'Failed inspection' })}>Fail QC and return item</button>
                </>}
                {r.status === 'REFUND_INITIATED' && r.manualRefund && (
                  <button className="btn-primary btn-sm" disabled={busyId === r.id} onClick={() => act(r, 'refund-confirmed', { note: notes[r.id] || 'COD refund sent to customer UPI' })}>Confirm COD refund sent</button>
                )}
              </div>
              {r.status === 'PICKUP_SCHEDULED' && <p className="text-xs text-ink-faint">Waiting for the courier webhook to confirm pickup and delivery to the warehouse.</p>}
              {r.status === 'REFUND_INITIATED' && !r.manualRefund && <p className="text-xs text-ink-faint">Gateway refund is awaiting confirmation from the signed payment webhook.</p>}
              {r.replacementOrderId && <p className="text-sm">Replacement order: <span className="font-mono text-xs">{r.replacementOrderId}</span></p>}
              {busyId === r.id && <p className="text-xs text-ink-faint">{statusLabel(r.status)}…</p>}
            </article>
          ))}
        </div>
      )}
    </>
  )
}
