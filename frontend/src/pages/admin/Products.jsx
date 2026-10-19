import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import api, { errMsg } from '../../api'
import { CapMark, colorFor, ErrorBox, PageLoader, Pagination } from '../../components/ui'
import { useDialog, useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { inr } from '../../lib/format'
import { PageHeader, Table } from './common'

export const toRequest = (p, over = {}) => ({
  name: p.name, description: p.description, categoryId: p.categoryId, basePrice: p.basePrice, imageUrl: p.imageUrl,
  active: p.active, tiers: p.tiers.map((t) => ({ minQty: t.minQty, price: t.price })), ...over,
})

export default function AdminProducts() {
  const toast = useToast()
  const { confirm, prompt } = useDialog()
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useAsync(() => api.get('/admin/products', { params: { page, size: 20 } }).then((r) => r.data), [page])
  const cats = useAsync(() => api.get('/categories').then((r) => r.data))
  const [cat, setCat] = useState('')

  const toggle = async (p) => {
    try { await api.put(`/admin/products/${p.publicId}`, toRequest(p, { active: !p.active })); toast(p.active ? 'Product hidden from store' : 'Product is live'); reload() } catch (e) { toast(errMsg(e), 'error') }
  }
  const del = async (p) => {
    if (!(await confirm(`Delete "${p.name}" from the store? It will be hidden from customers, while existing order history will be retained.`))) return
    try { await api.delete(`/admin/products/${p.publicId}`); toast('Product removed from store'); reload() } catch (e) { toast(errMsg(e), 'error') }
  }
  const addCat = async (e) => {
    e.preventDefault()
    try { await api.post('/admin/categories', { name: cat }); setCat(''); cats.reload(); toast('Category added') } catch (x) { toast(errMsg(x), 'error') }
  }
  const rename = async (c) => {
    const name = await prompt('Rename category', c.name)
    if (name && name.trim() && name !== c.name) { try { await api.put(`/admin/categories/${c.id}`, { name, parentId: c.parentId }); cats.reload() } catch (x) { toast(errMsg(x), 'error') } }
  }

  return (
    <>
      <PageHeader title="Products"><Link to="/admin/products/new" className="btn-primary btn-sm"><Plus size={14} /> Add product</Link></PageHeader>
      <div className="card mb-5 p-4">
        <h2 className="mb-2 text-sm font-bold">Categories</h2>
        <div className="flex flex-wrap items-center gap-2">
          {cats.data?.map((c) => <button key={c.id} onClick={() => rename(c)} title="Click to rename" className="rounded-full border-2 border-dashed border-ink/30 px-3 py-1 text-sm hover:border-ink">{c.name}</button>)}
          <form onSubmit={addCat} className="flex gap-2"><input className="input !w-44" placeholder="New category" value={cat} onChange={(e) => setCat(e.target.value)} aria-label="New category name" /><button className="btn-outline btn-sm" disabled={!cat.trim()}>Add</button></form>
        </div>
      </div>
      {loading ? <PageLoader /> : error ? <ErrorBox message={error} onRetry={reload} /> : <>
        <Table head={['Product', 'Category', 'Price', 'Variants', 'Stock', 'Status', '']} empty="No products yet.">
          {data.content.map((p) => (
            <tr key={p.id}>
              <td className="td"><div className="flex items-center gap-3"><div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded bg-twill">{p.imageUrl ? <img src={p.imageUrl} alt="" className="h-full w-full object-cover" /> : <CapMark className="w-9" color={colorFor(p.name)} />}</div><b>{p.name}</b></div></td>
              <td className="td">{p.categoryName}</td>
              <td className="td">{inr(p.basePrice)}</td>
              <td className="td">{p.variants.length}</td>
              <td className="td">{p.variants.reduce((s, v) => s + v.stockQty, 0)}</td>
              <td className="td"><button onClick={() => toggle(p)} className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${p.active ? 'bg-moss-light text-moss' : 'bg-twill-dark text-ink-soft'}`} title="Click to toggle">{p.active ? 'Live' : 'Hidden'}</button></td>
              <td className="td"><div className="flex gap-2"><Link to={`/admin/products/${p.publicId}`} className="btn-outline btn-sm"><Pencil size={13} /> Edit</Link><button className="btn-ghost btn-sm text-brim" onClick={() => del(p)} aria-label={`Delete ${p.name}`} title="Remove from store"><Trash2 size={14} /> Delete</button></div></td>
            </tr>
          ))}
        </Table>
        <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
      </>}
    </>
  )
}
