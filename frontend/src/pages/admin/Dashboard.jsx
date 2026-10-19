import { Link } from 'react-router-dom'
import { AlertTriangle, IndianRupee, ShoppingCart, Truck } from 'lucide-react'
import api from '../../api'
import { ErrorBox, PageLoader } from '../../components/ui'
import { useAsync } from '../../lib/hooks'
import { inr, statusLabel } from '../../lib/format'
import { PageHeader, Table } from './common'

export default function Dashboard() {
  const { data: d, loading, error, reload } = useAsync(() => api.get('/admin/dashboard').then((r) => r.data))
  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  const toProcess = (d.ordersByStatus.CONFIRMED || 0) + (d.ordersByStatus.PROCESSING || 0) + (d.ordersByStatus.PACKED || 0)
  const stats = [
    ['Today’s sales', inr(d.todaySales), IndianRupee], ['This month', inr(d.monthSales), IndianRupee],
    ['Total orders', d.totalOrders, ShoppingCart], ['To process / ship', toProcess, Truck],
  ]
  const max = Math.max(1, ...Object.values(d.ordersByStatus))
  return (
    <>
      <PageHeader title="Dashboard" sub="Sales count only paid orders." />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {stats.map(([label, val, Icon]) => (
          <div key={label} className="card group relative overflow-hidden p-4 transition-transform hover:-translate-y-1">
            <div className="absolute -right-5 -top-8 h-24 w-24 rounded-full bg-thread/10 blur-2xl transition group-hover:bg-thread/20" />
            <span className="relative flex h-10 w-10 items-center justify-center rounded-xl border border-white bg-white/70 text-ink shadow-sm"><Icon size={18} /></span>
            <p className="relative mt-4 text-[10px] font-extrabold uppercase tracking-[.16em] text-ink-faint">{label}</p><p className="relative mt-1 text-2xl font-extrabold tracking-tight">{val}</p>
          </div>
        ))}
      </div>
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <div className="card p-4">
          <h2 className="mb-3 font-bold">Orders by status</h2>
          <ul className="space-y-2">
            {Object.entries(d.ordersByStatus).filter(([, n]) => n > 0).map(([s, n]) => (
              <li key={s}><Link to={`/admin/orders?status=${s}`} className="block">
                <div className="flex justify-between text-sm"><span>{statusLabel(s)}</span><b>{n}</b></div>
                <div className="mt-1 h-2 overflow-hidden rounded-full bg-ink/[.06]"><div className="h-2 rounded-full bg-gradient-to-r from-ink to-[#6a7895]" style={{ width: `${(n / max) * 100}%` }} /></div>
              </Link></li>
            ))}
            {d.totalOrders === 0 && <p className="text-sm text-ink-faint">No orders yet.</p>}
          </ul>
        </div>
        <div>
          <h2 className="mb-3 flex items-center gap-2 font-bold"><AlertTriangle size={16} className="text-amber-600" /> Low stock</h2>
          <Table head={['Product', 'SKU', 'Left']} empty="All stocked up.">
            {d.lowStock.map((v) => <tr key={v.variantId}><td className="td">{v.productName}<br /><span className="text-xs text-ink-faint">{[v.color, v.size].filter(Boolean).join(' / ')}</span></td><td className="td">{v.sku}</td><td className={`td font-bold ${v.stockQty === 0 ? 'text-brim' : 'text-amber-700'}`}>{v.stockQty}</td></tr>)}
          </Table>
        </div>
      </div>
    </>
  )
}
