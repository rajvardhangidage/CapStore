import { useState } from 'react'
import api, { errMsg } from '../../api'
import { ErrorBox, Field, Modal, PageLoader } from '../../components/ui'
import { useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { fmtDate } from '../../lib/format'
import { PageHeader, Table } from './common'

const LOW = 10

export default function Inventory() {
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(() => api.get('/admin/products', { params: { size: 200 } }).then((r) => r.data.content))
  const [q, setQ] = useState('')
  const [lowOnly, setLowOnly] = useState(false)
  const [adjust, setAdjust] = useState(null)
  const [log, setLog] = useState(null)

  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />

  const rows = data.flatMap((p) => p.variants.map((v) => ({ ...v, product: p.name })))
    .filter((r) => (!q || `${r.product} ${r.sku}`.toLowerCase().includes(q.toLowerCase())) && (!lowOnly || r.stockQty <= LOW))

  const submit = async (e) => {
    e.preventDefault()
    try { await api.post(`/admin/variants/${adjust.v.id}/stock`, { delta: Number(adjust.delta), reason: adjust.reason }); toast('Stock updated'); setAdjust(null); reload() } catch (x) { toast(errMsg(x), 'error') }
  }
  const openLog = async (v) => {
    try { const { data: d } = await api.get(`/admin/variants/${v.id}/stock-log`, { params: { size: 30 } }); setLog({ v, rows: d.content }) } catch (x) { toast(errMsg(x), 'error') }
  }

  return (
    <>
      <PageHeader title="Inventory" sub={`Low stock is ${LOW} or fewer pieces.`}>
        <input className="input !w-56" placeholder="Search product or SKU" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search inventory" />
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={lowOnly} onChange={(e) => setLowOnly(e.target.checked)} /> Low stock only</label>
      </PageHeader>
      <Table head={['Product', 'SKU', 'Variant', 'In stock', '']} empty="No variants match.">
        {rows.map((r) => (
          <tr key={r.id}>
            <td className="td font-bold">{r.product}</td><td className="td">{r.sku}</td>
            <td className="td">{[r.color, r.size].filter(Boolean).join(' / ') || '—'}</td>
            <td className={`td font-extrabold ${r.stockQty === 0 ? 'text-brim' : r.stockQty <= LOW ? 'text-amber-700' : ''}`}>{r.stockQty}</td>
            <td className="td"><div className="flex gap-2"><button className="btn-outline btn-sm" onClick={() => setAdjust({ v: r, delta: '', reason: 'Restock' })}>Adjust</button><button className="btn-ghost btn-sm" onClick={() => openLog(r)}>History</button></div></td>
          </tr>
        ))}
      </Table>
      {adjust && (
        <Modal title={`Adjust stock · ${adjust.v.sku}`} onClose={() => setAdjust(null)}>
          <form onSubmit={submit} className="space-y-3">
            <p className="text-sm text-ink-soft">Currently {adjust.v.stockQty} in stock. Use a negative number to remove stock.</p>
            <Field label="Change (+/−)"><input className="input" type="number" required value={adjust.delta} onChange={(e) => setAdjust({ ...adjust, delta: e.target.value })} /></Field>
            <Field label="Reason"><input className="input" required value={adjust.reason} onChange={(e) => setAdjust({ ...adjust, reason: e.target.value })} /></Field>
            <button className="btn-primary w-full" disabled={!Number(adjust.delta)}>Update stock</button>
          </form>
        </Modal>
      )}
      {log && (
        <Modal title={`Stock history · ${log.v.sku}`} onClose={() => setLog(null)} wide>
          <Table head={['When', 'Change', 'Reason']} empty="No history yet.">
            {log.rows.map((l) => <tr key={l.id}><td className="td">{fmtDate(l.at)}</td><td className={`td font-bold ${l.changeQty < 0 ? 'text-brim' : 'text-moss'}`}>{l.changeQty > 0 ? '+' : ''}{l.changeQty}</td><td className="td">{l.reason.replaceAll('_', ' ').toLowerCase()}{l.refId ? ` #${l.refId}` : ''}</td></tr>)}
          </Table>
        </Modal>
      )}
    </>
  )
}
