import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Heart, Minus, Plus } from 'lucide-react'
import api, { errMsg } from '../api'
import { CapMark, colorFor, ErrorBox, Field, PageLoader, Stars } from '../components/ui'
import { useAsync } from '../lib/hooks'
import { inr, fmtDate } from '../lib/format'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { useWishlist } from '../context/WishlistContext'
import { useToast } from '../context/ToastContext'

const label = (v) => [v.color, v.size].filter(Boolean).join(' / ') || 'Standard'

export default function ProductDetail() {
  const { id } = useParams()
  const { data: p, loading, error, reload } = useAsync(() => api.get(`/products/${id}`).then((r) => r.data), [id])
  const reviews = useAsync(() => p ? api.get(`/products/${p.id}/reviews`).then((r) => r.data) : Promise.resolve(null), [p?.id])
  const { add, busy } = useCart()
  const { user, isB2B } = useAuth()
  const { ids, toggle } = useWishlist()
  const toast = useToast()
  const [vid, setVid] = useState(null)
  const [selectedImage, setSelectedImage] = useState(0)
  const [qty, setQty] = useState(1)
  const [rv, setRv] = useState({ rating: 5, body: '' })

  useEffect(() => {
    if (p) setVid((cur) => cur ?? (p.variants.find((v) => v.stockQty > 0) || p.variants[0])?.id)
  }, [p])
  useEffect(() => { document.title = p ? `${p.name} | CapStore` : 'CapStore' }, [p])

  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />

  const variant = p.variants.find((v) => v.id === vid)
  const price = Number(variant?.price ?? p.basePrice)
  const images = p.imageUrls?.length ? p.imageUrls : p.imageUrl ? [p.imageUrl] : []
  const inStock = variant && variant.stockQty > 0
  const clampQty = (n) => Math.max(1, Math.min(n || 1, variant?.stockQty || 1))

  const submitReview = async (e) => {
    e.preventDefault()
    try { await api.post(`/products/${p.id}/reviews`, rv); toast('Thanks for your review'); reviews.reload() }
    catch (err) { toast(errMsg(err), 'error') }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <nav className="mb-4 text-sm text-ink-faint"><Link to="/products" className="hover:underline">Shop</Link> / {p.categoryName}</nav>
      <div className="grid gap-8 md:grid-cols-2">
        <div className="space-y-3">
          <div className="glass-panel flex aspect-square items-center justify-center overflow-hidden rounded-[2rem] bg-white/45">
            {images.length
              ? <img src={images[selectedImage] || images[0]} alt={`${p.name} - image ${selectedImage + 1}`} className="h-full w-full object-cover" />
              : <CapMark className="w-2/3" color={colorFor(p.name)} />}
          </div>
          {images.length > 1 && (
            <div className="flex gap-3 overflow-x-auto pb-1" aria-label="Product images">
              {images.map((image, index) => (
                <button key={`${image}-${index}`} type="button" onClick={() => setSelectedImage(index)} aria-label={`Show image ${index + 1}`} aria-pressed={selectedImage === index}
                  className={`h-20 w-20 shrink-0 overflow-hidden rounded-xl border-2 bg-white/60 transition ${selectedImage === index ? 'border-thread-dark shadow-md shadow-thread/20' : 'border-white/80 opacity-75 hover:opacity-100'}`}>
                  <img src={image} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>
        <div>
          <h1 className="text-3xl font-extrabold">{p.name}</h1>
          {reviews.data?.count > 0 && <div className="mt-2 flex items-center gap-2 text-sm"><Stars value={reviews.data.average} /><span className="text-ink-soft">{reviews.data.average} ({reviews.data.count})</span></div>}
          <p className="mt-4 text-3xl font-extrabold">{inr(price)}</p>
          <p className="text-xs text-ink-faint">GST is added at checkout.</p>
          {p.description && <p className="mt-4 text-ink-soft">{p.description}</p>}

          <div className="mt-6">
            <p className="label">Choose option</p>
            <div className="flex flex-wrap gap-2">
              {p.variants.map((v) => (
                <button key={v.id} disabled={v.stockQty === 0} onClick={() => { setVid(v.id); setQty(1) }}
                  className={`rounded-md border-2 px-3 py-2 text-sm font-bold disabled:cursor-not-allowed disabled:border-dashed disabled:text-ink-faint disabled:line-through ${v.id === vid ? 'border-ink bg-ink text-white' : 'border-twill-dark bg-white hover:border-ink'}`}>
                  {label(v)}
                </button>
              ))}
            </div>
            {variant && <p className={`mt-2 text-sm font-medium ${variant.stockQty === 0 ? 'text-brim' : variant.stockQty <= 10 ? 'text-amber-700' : 'text-moss'}`}>
              {variant.stockQty === 0 ? 'Out of stock' : variant.stockQty <= 10 ? `Only ${variant.stockQty} left` : 'In stock'}
            </p>}
          </div>

          <div className="mt-5 flex flex-wrap items-center gap-3">
            <div className="flex items-center rounded-md border-2 border-twill-dark bg-white">
              <button className="p-2.5" onClick={() => setQty(clampQty(qty - 1))} aria-label="Decrease quantity"><Minus size={16} /></button>
              <input className="w-12 border-0 bg-transparent text-center text-sm font-bold" inputMode="numeric" value={qty} onChange={(e) => setQty(clampQty(parseInt(e.target.value, 10)))} aria-label="Quantity" />
              <button className="p-2.5" onClick={() => setQty(clampQty(qty + 1))} aria-label="Increase quantity"><Plus size={16} /></button>
            </div>
            <button className="btn-primary flex-1 sm:flex-none sm:px-10" disabled={!inStock || busy} onClick={() => add(p, variant, qty)}>Add to cart</button>
            <button className="btn-outline px-3" onClick={() => toggle(p.id)} aria-label="Wishlist"><Heart size={18} className={ids.has(p.id) ? 'fill-brim text-brim' : ''} /></button>
          </div>

          {p.tiers.length > 0 && (
            <div className="seam mt-8 p-4">
              <h3 className="font-bold">Bulk pricing for businesses</h3>
              <p className="mb-2 text-xs text-ink-soft">Applies automatically to approved business accounts.</p>
              <table className="w-full text-sm">
                <tbody>
                  <tr className="border-b border-twill-dark"><td className="py-1.5">1+ pieces</td><td className="text-right font-bold">{inr(p.basePrice)}</td></tr>
                  {p.tiers.map((t) => <tr key={t.minQty} className="border-b border-twill-dark last:border-0"><td className="py-1.5">{t.minQty}+ pieces</td><td className="text-right font-bold">{inr(t.price)}</td></tr>)}
                </tbody>
              </table>
              {isB2B && <Link to={`/quotes?product=${p.id}`} className="btn-thread btn-sm mt-3">Request a custom quote</Link>}
              {!user && <Link to="/register?type=business" className="mt-3 inline-block text-sm font-bold underline">Open a business account</Link>}
            </div>
          )}
        </div>
      </div>

      <section className="mt-14 max-w-3xl">
        <h2 className="mb-4 text-2xl font-extrabold">Reviews</h2>
        {reviews.data?.reviews.length === 0 && <p className="text-sm text-ink-soft">No reviews yet.</p>}
        <ul className="space-y-4">
          {reviews.data?.reviews.map((r) => (
            <li key={r.id} className="card p-4"><div className="flex items-center justify-between"><b className="text-sm">{r.userName}</b><span className="text-xs text-ink-faint">{fmtDate(r.createdAt)}</span></div><Stars value={r.rating} size={14} />{r.body && <p className="mt-1 text-sm text-ink-soft">{r.body}</p>}</li>
          ))}
        </ul>
        {reviews.data?.canReview && (
          <form onSubmit={submitReview} className="seam mt-6 space-y-3 p-4">
            <h3 className="font-bold">Write a review</h3>
            <Field label="Rating"><select className="input !w-auto" value={rv.rating} onChange={(e) => setRv({ ...rv, rating: Number(e.target.value) })}>{[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} stars</option>)}</select></Field>
            <Field label="Your review"><textarea className="input" rows={3} maxLength={1000} value={rv.body} onChange={(e) => setRv({ ...rv, body: e.target.value })} /></Field>
            <button className="btn-primary">Submit review</button>
          </form>
        )}
      </section>
    </div>
  )
}
