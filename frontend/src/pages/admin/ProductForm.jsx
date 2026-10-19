import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Plus, Trash2, Upload, X } from 'lucide-react'
import api, { errMsg } from '../../api'
import { ErrorBox, Field, PageLoader } from '../../components/ui'
import { useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { PageHeader } from './common'

const blankVariant = { sku: '', size: '', color: '', price: '', stockQty: 0 }

export default function ProductForm() {
  const { id } = useParams()
  const isNew = !id
  const nav = useNavigate()
  const toast = useToast()
  const cats = useAsync(() => api.get('/categories').then((r) => r.data))
  const existing = useAsync(async () => {
    if (isNew) return null
    return (await api.get(`/admin/products/${id}`)).data
  }, [id])
  const [f, setF] = useState({ name: '', description: '', categoryId: '', basePrice: '', imageUrls: [], active: true, returnable: true })
  const [tiers, setTiers] = useState([])
  const [variants, setVariants] = useState([{ ...blankVariant }]) // new product only
  const [busy, setBusy] = useState(false)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    const p = existing.data
    if (!p) return
    setF({
      name: p.name,
      description: p.description || '',
      categoryId: p.categoryId,
      basePrice: p.basePrice,
      imageUrls: p.imageUrls?.length ? p.imageUrls : p.imageUrl ? [p.imageUrl] : [],
      active: p.active,
      returnable: p.returnable,
    })
    setTiers(p.tiers.map((t) => ({ minQty: t.minQty, price: t.price })))
  }, [existing.data])

  if (cats.loading || existing.loading) return <PageLoader />
  if (existing.error) return <ErrorBox message={existing.error} />
  if (!isNew && !existing.data) return <ErrorBox message="Product not found" />
  const p = existing.data

  const upload = async (files) => {
    if (!files.length) return
    setUploading(true)
    const uploaded = []
    try {
      for (const file of files) {
        const fd = new FormData()
        fd.append('file', file)
        const { data } = await api.post('/uploads', fd)
        uploaded.push(data.url)
      }
      setF((current) => ({ ...current, imageUrls: [...current.imageUrls, ...uploaded] }))
    } catch (e) {
      if (uploaded.length) setF((current) => ({ ...current, imageUrls: [...current.imageUrls, ...uploaded] }))
      toast(`Some images could not be uploaded: ${errMsg(e)}`, 'error')
    } finally {
      setUploading(false)
    }
  }
  const clean = (v) => ({ sku: v.sku, size: v.size || null, color: v.color || null, price: v.price === '' || v.price == null ? null : Number(v.price), stockQty: Number(v.stockQty) || 0 })

  const save = async (e) => {
    e.preventDefault(); setBusy(true)
    const body = {
      name: f.name, description: f.description, categoryId: Number(f.categoryId), basePrice: Number(f.basePrice),
      imageUrl: f.imageUrls[0] || null, imageUrls: f.imageUrls, active: f.active, returnable: f.returnable,
      tiers: tiers.filter((t) => t.minQty && t.price).map((t) => ({ minQty: Number(t.minQty), price: Number(t.price) })),
    }
    try {
      if (isNew) await api.post('/admin/products', { ...body, variants: variants.filter((v) => v.sku.trim()).map(clean) })
      else await api.put(`/admin/products/${p.publicId}`, body)
      toast('Product saved'); nav('/admin/products')
    } catch (x) { toast(errMsg(x), 'error') } finally { setBusy(false) }
  }

  return (
    <div className="max-w-3xl">
      <Link to="/admin/products" className="text-sm font-bold underline underline-offset-4">← Products</Link>
      <PageHeader title={isNew ? 'New product' : 'Edit product'} />
      <form onSubmit={save} className="space-y-5">
        <div className="card space-y-3 p-4">
          <Field label="Name"><input className="input" required value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></Field>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Category"><select className="input" required value={f.categoryId} onChange={(e) => setF({ ...f, categoryId: e.target.value })}><option value="">Select…</option>{cats.data.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></Field>
            <Field label="Base price (₹, before GST)"><input className="input" type="number" min="0" step="0.01" required value={f.basePrice} onChange={(e) => setF({ ...f, basePrice: e.target.value })} /></Field>
          </div>
          <Field label="Description"><textarea className="input" rows={3} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></Field>
          <div>
            <Field label="Product images">
              <span className="flex cursor-pointer items-center justify-center gap-2 rounded-2xl border border-dashed border-ink/20 bg-white/40 px-4 py-5 text-sm font-bold text-ink-soft transition hover:border-thread-dark hover:bg-white/70">
                <Upload size={17} />{uploading ? 'Uploading images…' : 'Choose multiple images'}
                <input type="file" accept="image/png,image/jpeg,image/webp" multiple disabled={uploading} className="sr-only" onChange={(e) => { upload(Array.from(e.target.files || [])); e.target.value = '' }} />
              </span>
            </Field>
            <p className="mt-1 text-xs text-ink-faint">The first image is used as the product cover. Add images in the order you want them displayed.</p>
            {f.imageUrls.length > 0 && (
              <div className="mt-3 grid grid-cols-3 gap-3 sm:grid-cols-4">
                {f.imageUrls.map((url, index) => (
                  <div key={`${url}-${index}`} className="group relative aspect-square overflow-hidden rounded-xl border border-white bg-white/60 shadow-sm">
                    <img src={url} alt={`Product preview ${index + 1}`} className="h-full w-full object-cover" />
                    {index === 0 && <span className="absolute bottom-2 left-2 rounded-full bg-ink/85 px-2 py-1 text-[10px] font-bold text-white">Cover</span>}
                    <button type="button" aria-label={`Remove image ${index + 1}`} onClick={() => setF((current) => ({ ...current, imageUrls: current.imageUrls.filter((_, i) => i !== index) }))} className="absolute right-2 top-2 rounded-full bg-white/90 p-1.5 text-brim shadow hover:bg-white">
                      <X size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Visible in store</label>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.returnable} onChange={(e) => setF({ ...f, returnable: e.target.checked })} /> Eligible for returns and replacements</label>
        </div>

        <div className="card space-y-3 p-4">
          <div className="flex items-center justify-between"><h2 className="font-bold">Bulk price tiers (business accounts)</h2><button type="button" className="btn-outline btn-sm" onClick={() => setTiers([...tiers, { minQty: '', price: '' }])}><Plus size={13} /> Add tier</button></div>
          {tiers.length === 0 && <p className="text-sm text-ink-faint">No tiers. Business customers pay the normal price.</p>}
          {tiers.map((t, i) => (
            <div key={i} className="flex items-end gap-2">
              <Field label="Min quantity"><input className="input" type="number" min="1" value={t.minQty} onChange={(e) => setTiers(tiers.map((x, j) => (j === i ? { ...x, minQty: e.target.value } : x)))} /></Field>
              <Field label="Price per piece (₹)"><input className="input" type="number" min="0" step="0.01" value={t.price} onChange={(e) => setTiers(tiers.map((x, j) => (j === i ? { ...x, price: e.target.value } : x)))} /></Field>
              <button type="button" className="p-2.5 text-brim" onClick={() => setTiers(tiers.filter((_, j) => j !== i))} aria-label="Remove tier"><Trash2 size={18} /></button>
            </div>
          ))}
        </div>

        {isNew && (
          <div className="card space-y-3 p-4">
            <div className="flex items-center justify-between"><h2 className="font-bold">Variants (size / colour / stock)</h2><button type="button" className="btn-outline btn-sm" onClick={() => setVariants([...variants, { ...blankVariant }])}><Plus size={13} /> Add variant</button></div>
            {variants.map((v, i) => <VariantRow key={i} v={v} onChange={(nv) => setVariants(variants.map((x, j) => (j === i ? nv : x)))} onRemove={variants.length > 1 ? () => setVariants(variants.filter((_, j) => j !== i)) : null} />)}
          </div>
        )}
        <button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save product'}</button>
      </form>

      {!isNew && <ExistingVariants product={p} onChange={existing.reload} />}
    </div>
  )
}

function VariantRow({ v, onChange, onRemove }) {
  const s = (k) => ({ className: 'input', value: v[k] ?? '', onChange: (e) => onChange({ ...v, [k]: e.target.value }) })
  return (
    <div className="grid grid-cols-2 items-end gap-2 sm:grid-cols-6">
      <Field label="SKU"><input {...s('sku')} /></Field>
      <Field label="Size"><input {...s('size')} /></Field>
      <Field label="Colour"><input {...s('color')} /></Field>
      <Field label="Price override"><input {...s('price')} type="number" min="0" step="0.01" /></Field>
      <Field label="Stock"><input {...s('stockQty')} type="number" min="0" /></Field>
      {onRemove && <button type="button" className="p-2.5 text-brim" onClick={onRemove} aria-label="Remove variant"><Trash2 size={18} /></button>}
    </div>
  )
}

function ExistingVariants({ product, onChange }) {
  const toast = useToast()
  const [adding, setAdding] = useState({ ...blankVariant })
  const [edits, setEdits] = useState({})
  const clean = (v) => ({ sku: v.sku, size: v.size || null, color: v.color || null, price: v.price === '' || v.price == null ? null : Number(v.price), stockQty: Number(v.stockQty) || 0 })

  const saveRow = async (v) => {
    try { await api.put(`/admin/variants/${v.id}`, clean(edits[v.id])); toast('Variant saved'); onChange() } catch (e) { toast(errMsg(e), 'error') }
  }
  const addRow = async (e) => {
    e.preventDefault()
    try { await api.post(`/admin/products/${product.publicId}/variants`, clean(adding)); setAdding({ ...blankVariant }); toast('Variant added'); onChange() } catch (x) { toast(errMsg(x), 'error') }
  }
  return (
    <div className="card mt-6 space-y-3 p-4">
      <h2 className="font-bold">Variants</h2>
      <p className="text-xs text-ink-faint">Change stock levels on the Inventory page so every change is logged.</p>
      {product.variants.map((v) => {
        const cur = edits[v.id] || v
        return (
          <div key={v.id} className="flex flex-wrap items-end gap-2">
            <div className="grid flex-1 grid-cols-2 gap-2 sm:grid-cols-4">
              {['sku', 'size', 'color', 'price'].map((k) => (
                <Field key={k} label={k === 'sku' ? 'SKU' : k === 'price' ? 'Price override' : k[0].toUpperCase() + k.slice(1)}>
                  <input className="input" value={cur[k] ?? ''} onChange={(e) => setEdits({ ...edits, [v.id]: { ...cur, [k]: e.target.value } })} />
                </Field>
              ))}
            </div>
            <span className="pb-3 text-sm text-ink-soft">Stock: <b>{v.stockQty}</b></span>
            <button className="btn-outline btn-sm mb-1" disabled={!edits[v.id]} onClick={() => saveRow(v)}>Save</button>
          </div>
        )
      })}
      <form onSubmit={addRow} className="seam space-y-2 p-3">
        <p className="text-sm font-bold">Add a variant</p>
        <VariantRow v={adding} onChange={setAdding} />
        <button className="btn-thread btn-sm" disabled={!adding.sku.trim()}>Add variant</button>
      </form>
    </div>
  )
}
