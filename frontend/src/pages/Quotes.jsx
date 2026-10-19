import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { errMsg } from '../api'
import { EmptyState, ErrorBox, Field, PageLoader, StatusBadge } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { useAsync } from '../lib/hooks'
import { fmtDate, inr } from '../lib/format'

export default function Quotes() {
  const { isB2B } = useAuth()
  const toast = useToast()
  const [sp] = useSearchParams()
  const list = useAsync(() => api.get('/quotes').then((r) => r.data))
  const products = useAsync(() => api.get('/products', { params: { size: 60 } }).then((r) => r.data.content))
  const [f, setF] = useState({ productId: sp.get('product') || '', quantity: 50, description: '', logoUrl: '' })
  const [busy, setBusy] = useState(false)

  if (!isB2B) return <EmptyState title="Quotes are for business accounts" text="Open a business account to request custom pricing and logo embroidery."><Link to="/register?type=business" className="btn-primary">Open a business account</Link></EmptyState>

  const upload = async (file) => {
    if (!file) return
    const fd = new FormData(); fd.append('file', file)
    try { const { data } = await api.post('/uploads', fd); setF((x) => ({ ...x, logoUrl: data.url })) } catch (e) { toast(errMsg(e), 'error') }
  }
  const submit = async (e) => {
    e.preventDefault(); setBusy(true)
    try {
      await api.post('/quotes', { ...f, productId: f.productId ? Number(f.productId) : null, quantity: Number(f.quantity), logoUrl: f.logoUrl || null })
      toast('Quote request sent. We will reply by email.'); setF({ ...f, description: '', logoUrl: '' }); list.reload()
    } catch (x) { toast(errMsg(x), 'error') } finally { setBusy(false) }
  }
  const decide = async (id, accept) => { try { await api.put(`/quotes/${id}/decision`, { accept }); list.reload(); toast(accept ? 'Quote accepted. We will contact you to finalise the order.' : 'Quote declined') } catch (x) { toast(errMsg(x), 'error') } }

  return (
    <div className="mx-auto grid max-w-6xl gap-8 px-4 py-8 lg:grid-cols-[400px_1fr]">
      <form onSubmit={submit} className="card h-fit space-y-3 p-5">
        <h1 className="text-xl font-extrabold">Request a quote</h1>
        <Field label="Cap (optional)"><select className="input" value={f.productId} onChange={(e) => setF({ ...f, productId: e.target.value })}><option value="">Custom / not listed</option>{products.data?.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select></Field>
        <Field label="Quantity"><input className="input" type="number" min={1} required value={f.quantity} onChange={(e) => setF({ ...f, quantity: e.target.value })} /></Field>
        <Field label="What do you need?" hint="Colours, sizes, logo placement, delivery date"><textarea className="input" rows={4} required value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></Field>
        <Field label="Logo (optional)"><input type="file" accept="image/png,image/jpeg,image/webp" className="text-sm" onChange={(e) => upload(e.target.files[0])} /></Field>
        {f.logoUrl && <img src={f.logoUrl} alt="Logo preview" className="h-16 rounded border border-twill-dark" />}
        <button className="btn-primary w-full" disabled={busy}>Send request</button>
      </form>
      <div>
        <h2 className="mb-3 text-xl font-extrabold">Your requests</h2>
        {list.loading ? <PageLoader /> : list.error ? <ErrorBox message={list.error} onRetry={list.reload} /> : list.data.length === 0 ? <p className="text-sm text-ink-soft">No requests yet.</p> : (
          <ul className="space-y-3">
            {list.data.map((q) => (
              <li key={q.id} className="card p-4 text-sm">
                <div className="flex items-center justify-between"><b>{q.quantity} pcs {q.productName && `· ${q.productName}`}</b><StatusBadge status={q.status} /></div>
                <p className="mt-1 text-ink-soft">{q.description}</p>
                <p className="mt-1 text-xs text-ink-faint">{fmtDate(q.createdAt)}</p>
                {q.quotedPrice && <div className="mt-3 rounded-md bg-thread-light p-3"><b>{inr(q.quotedPrice)} per piece</b> · total {inr(q.quotedPrice * q.quantity)} + GST{q.adminNote && <p className="mt-1">{q.adminNote}</p>}</div>}
                {q.status === 'QUOTED' && <div className="mt-3 flex gap-2"><button className="btn-primary btn-sm" onClick={() => decide(q.id, true)}>Accept</button><button className="btn-outline btn-sm" onClick={() => decide(q.id, false)}>Decline</button></div>}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
