import { useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'
import { EmptyState, ErrorBox, PageLoader, Pagination, StatusBadge } from '../components/ui'
import { useAsync } from '../lib/hooks'
import { fmtDate, inr } from '../lib/format'

export default function Orders() {
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useAsync(() => api.get('/orders', { params: { page, size: 10 } }).then((r) => r.data), [page])
  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-3xl font-extrabold">My orders</h1>
      {data.content.length === 0 ? <EmptyState title="No orders yet" text="When you place an order, you can track it here."><Link to="/products" className="btn-primary">Start shopping</Link></EmptyState> : (
        <ul className="space-y-3">
          {data.content.map((o) => (
            <li key={o.id}>
              <Link to={`/orders/${o.publicId}`} className="card block p-4 transition-shadow hover:shadow-md">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <b>{o.orderNo}</b><StatusBadge status={o.status} />
                </div>
                <p className="mt-1 text-sm text-ink-soft">{fmtDate(o.createdAt)} · {o.items.reduce((s, i) => s + i.quantity, 0)} items</p>
                <p className="mt-1 truncate text-sm">{o.items.map((i) => i.productName).join(', ')}</p>
                <p className="mt-2 font-extrabold">{inr(o.total)}</p>
              </Link>
            </li>
          ))}
        </ul>
      )}
      <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  )
}
