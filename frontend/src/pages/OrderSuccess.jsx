import { Link, useParams } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'
import api from '../api'
import { ErrorBox, PageLoader } from '../components/ui'
import { useAsync } from '../lib/hooks'
import { inr } from '../lib/format'

export default function OrderSuccess() {
  const { id } = useParams()
  const { data: o, loading, error, reload } = useAsync(() => api.get(`/orders/${id}`).then((r) => r.data), [id])
  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  return (
    <div className="mx-auto max-w-lg px-4 py-14 text-center">
      <CheckCircle2 className="mx-auto text-moss" size={56} />
      <h1 className="mt-4 text-3xl font-extrabold">Order confirmed</h1>
      <p className="mt-2 text-ink-soft">Thank you! We have received your payment of <b>{inr(o.total)}</b>. A confirmation email is on its way.</p>
      <div className="seam mt-6 p-5"><p className="text-sm text-ink-soft">Order number</p><p className="text-xl font-extrabold">{o.orderNo}</p></div>
      <div className="mt-6 flex justify-center gap-3"><Link to={`/orders/${o.publicId}`} className="btn-primary">Track order</Link><Link to="/products" className="btn-outline">Keep shopping</Link></div>
    </div>
  )
}
