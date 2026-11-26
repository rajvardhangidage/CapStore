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
          <div key={label} className="card p-5">
            <div className="flex items-center justify-between">
              <p className="eyebrow">{label}</p>
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-soft text-primary"><Icon size={17} /></span>
            </div>
            <p className="mt-3 text-3xl font-semibold tracking-tight">{val}</p>
          </div>
        ))}
      </div>
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <div className="card p-5">
          <h2 className="mb-4 font-semibold">Orders by status</h2>
          <ul className="space-y-2">
            {Object.entries(d.ordersByStatus).filter(([, n]) => n > 0).map(([s, n]) => (
              <li key={s}><Link to={`/admin/orders?status=${s}`} className="group block rounded-sm p-1 hover:bg-twill">
                <div className="flex justify-between text-sm"><span>{statusLabel(s)}</span><b>{n}</b></div>
                <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-twill"><div className="h-full rounded-full bg-primary transition-all group-hover:bg-primary-strong" style={{ width: `${(n / max) * 100}%` }} /></div>
              </Link></li>
            ))}
            {d.totalOrders === 0 && <p className="text-sm text-ink-faint">No orders yet.</p>}
          </ul>
        </div>
        <div>
          <h2 className="mb-3 flex items-center gap-2 font-semibold"><AlertTriangle size={16} className="text-warn" /> Low stock</h2>
          <Table head={['Product', 'SKU', 'Left']} empty="All stocked up." minWidth="min-w-0">
            {d.lowStock.map((v) => <tr key={v.variantId}><td className="td">{v.productName}<br /><span className="text-xs text-ink-faint">{[v.color, v.size].filter(Boolean).join(' / ')}</span></td><td className="td">{v.sku}</td><td className={`td font-bold ${v.stockQty === 0 ? 'text-brim' : 'text-warn'}`}>{v.stockQty}</td></tr>)}
          </Table>
        </div>
      </div>
    </>
  )
}
