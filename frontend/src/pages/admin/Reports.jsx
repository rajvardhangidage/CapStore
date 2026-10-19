import { useState } from 'react'
import { Download } from 'lucide-react'
import api, { downloadBlob, errMsg } from '../../api'
import { ErrorBox, Field, Spinner } from '../../components/ui'
import { useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { PageHeader } from './common'

const TYPES = [
  ['sales', 'Daily sales'], ['products', 'Product-wise sales'], ['categories', 'Category-wise sales'],
  ['customer-types', 'B2B vs B2C'], ['gst', 'GST report'], ['inventory', 'Inventory & stock value'],
]
const iso = (d) => d.toISOString().slice(0, 10)
const today = new Date()
const monthAgo = new Date(Date.now() - 29 * 86400000)

export default function Reports() {
  const toast = useToast()
  const [type, setType] = useState('sales')
  const [from, setFrom] = useState(iso(monthAgo))
  const [to, setTo] = useState(iso(today))
  const { data, loading, error, reload } = useAsync(() => api.get(`/admin/reports/${type}`, { params: { from, to } }).then((r) => r.data), [type, from, to])
  const noDates = type === 'inventory'
  const fmtCell = (v) => (typeof v === 'number' && !Number.isInteger(v) ? v.toFixed(2) : typeof v === 'string' && /^\d+\.\d+$/.test(v) ? Number(v).toFixed(2) : v)

  return (
    <>
      <PageHeader title="Reports" sub="Sales figures include paid orders only.">
        <button className="btn-outline btn-sm" disabled={!data} onClick={() => downloadBlob(`/admin/reports/${type}/export`, `${type}-report.xlsx`, { from, to }).catch((e) => toast(errMsg(e), 'error'))}><Download size={14} /> Export to Excel</button>
      </PageHeader>
      <div className="card mb-5 flex flex-wrap items-end gap-3 p-4">
        <Field label="Report"><select className="input" value={type} onChange={(e) => setType(e.target.value)}>{TYPES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></Field>
        {!noDates && <>
          <Field label="From"><input className="input" type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} /></Field>
          <Field label="To"><input className="input" type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} /></Field>
        </>}
      </div>
      {loading ? <div className="flex justify-center py-16"><Spinner className="h-7 w-7" /></div> : error ? <ErrorBox message={error} onRetry={reload} /> : (
        <div className="card overflow-x-auto">
          <h2 className="border-b border-twill-dark p-4 font-bold">{data.title}</h2>
          <table className="w-full min-w-[560px]">
            <thead className="bg-twill/60"><tr>{data.headers.map((h) => <th key={h} className="th">{h}</th>)}</tr></thead>
            <tbody className="divide-y divide-twill-dark">
              {data.rows.map((r, i) => <tr key={i} className={r[0] === 'TOTAL' ? 'bg-thread-light font-extrabold' : ''}>{r.map((c, j) => <td key={j} className="td">{fmtCell(c)}</td>)}</tr>)}
            </tbody>
          </table>
          {data.rows.length === 0 && <p className="p-8 text-center text-sm text-ink-faint">No data for this period.</p>}
        </div>
      )}
    </>
  )
}
