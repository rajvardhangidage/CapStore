import { useState } from 'react'
import api, { errMsg } from '../../api'
import { ErrorBox, Field, Modal, PageLoader, Pagination, StatusBadge } from '../../components/ui'
import { useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { fmtDate, inr } from '../../lib/format'
import { PageHeader, Table } from './common'

export default function AdminQuotes() {
  const toast = useToast()
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useAsync(() => api.get('/admin/quotes', { params: { status: status || undefined, page, size: 20 } }).then((r) => r.data), [status, page])
  const [q, setQ] = useState(null)

  const send = async (e) => {
    e.preventDefault()
    try { await api.put(`/admin/quotes/${q.id}/respond`, { quotedPrice: Number(q.price), adminNote: q.note || null }); toast('Quote sent to customer'); setQ(null); reload() } catch (x) { toast(errMsg(x), 'error') }
  }
  return (
    <>
      <PageHeader title="Quote requests">
        <select className="input !w-auto" value={status} onChange={(e) => { setPage(0); setStatus(e.target.value) }} aria-label="Filter by status">
          <option value="">All</option><option value="PENDING">Pending</option><option value="QUOTED">Quoted</option><option value="ACCEPTED">Accepted</option><option value="REJECTED">Declined</option>
        </select>
      </PageHeader>
      {loading ? <PageLoader /> : error ? <ErrorBox message={error} onRetry={reload} /> : <>
        <Table head={['Customer', 'Request', 'Qty', 'Logo', 'Quote', 'Status', '']} empty="No quote requests.">
          {data.content.map((x) => (
            <tr key={x.id}>
              <td className="td"><b>{x.companyName || x.customerName}</b><br /><span className="text-xs text-ink-faint">{x.customerEmail}</span></td>
              <td className="td max-w-xs">{x.productName && <b>{x.productName}<br /></b>}<span className="text-ink-soft">{x.description}</span><br /><span className="text-xs text-ink-faint">{fmtDate(x.createdAt)}</span></td>
              <td className="td">{x.quantity}</td>
              <td className="td">{x.logoUrl ? <a href={x.logoUrl} target="_blank" rel="noreferrer"><img src={x.logoUrl} alt="Customer logo" className="h-10 rounded border border-twill-dark" /></a> : '—'}</td>
              <td className="td">{x.quotedPrice ? `${inr(x.quotedPrice)}/pc` : '—'}</td>
              <td className="td"><StatusBadge status={x.status} /></td>
              <td className="td">{x.status !== 'ACCEPTED' && <button className="btn-outline btn-sm" onClick={() => setQ({ id: x.id, price: x.quotedPrice || '', note: x.adminNote || '', who: x.companyName || x.customerName, qty: x.quantity })}>{x.status === 'PENDING' ? 'Respond' : 'Revise'}</button>}</td>
            </tr>
          ))}
        </Table>
        <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
      </>}
      {q && (
        <Modal title={`Quote for ${q.who}`} onClose={() => setQ(null)}>
          <form onSubmit={send} className="space-y-3">
            <Field label={`Price per piece (₹, before GST) for ${q.qty} pcs`}><input className="input" type="number" min="0.01" step="0.01" required value={q.price} onChange={(e) => setQ({ ...q, price: e.target.value })} /></Field>
            <Field label="Note to customer (lead time, embroidery details…)"><textarea className="input" rows={3} value={q.note} onChange={(e) => setQ({ ...q, note: e.target.value })} /></Field>
            <button className="btn-primary w-full">Send quote by email</button>
          </form>
        </Modal>
      )}
    </>
  )
}
