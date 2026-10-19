import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api from '../../api'
import { ErrorBox, PageLoader, Pagination, StatusBadge } from '../../components/ui'
import { useAsync } from '../../lib/hooks'
import { fmtDate, inr, statusLabel } from '../../lib/format'
import { PageHeader, Table } from './common'

const STATUSES = ['PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURN_REQUESTED', 'RETURNED', 'REFUNDED']

export default function AdminOrders() {
  const [sp, setSp] = useSearchParams()
  const [page, setPage] = useState(0)
  const status = sp.get('status') || ''
  const { data, loading, error, reload } = useAsync(() => api.get('/admin/orders', { params: { status: status || undefined, page, size: 20 } }).then((r) => r.data), [status, page])
  return (
    <>
      <PageHeader title="Orders">
        <select className="input !w-auto" value={status} onChange={(e) => { setPage(0); e.target.value ? setSp({ status: e.target.value }) : setSp({}) }} aria-label="Filter by status">
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}
        </select>
      </PageHeader>
      {loading ? <PageLoader /> : error ? <ErrorBox message={error} onRetry={reload} /> : <>
        <Table head={['Order', 'Date', 'Customer', 'Items', 'Total', 'Payment', 'Status']} empty="No orders match.">
          {data.content.map((o) => (
            <tr key={o.id} className="hover:bg-twill/50">
              <td className="td font-bold"><Link to={`/admin/orders/${o.publicId}`} className="underline underline-offset-4">{o.orderNo}</Link></td>
              <td className="td whitespace-nowrap">{fmtDate(o.createdAt)}</td>
              <td className="td">{o.customerName}<br /><span className="text-xs text-ink-faint">{o.customerEmail}</span></td>
              <td className="td">{o.items.reduce((s, i) => s + i.quantity, 0)}</td>
              <td className="td font-bold">{inr(o.total)}</td>
              <td className="td"><StatusBadge status={o.paymentStatus} /></td>
              <td className="td"><StatusBadge status={o.status} /></td>
            </tr>
          ))}
        </Table>
        <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
      </>}
    </>
  )
}
