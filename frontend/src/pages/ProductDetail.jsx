import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronRight, Heart, Minus, Plus, RotateCcw, ShieldCheck, Truck } from 'lucide-react'
import api, { errMsg } from '../api'
import { CapMark, colorFor, ErrorBox, Field, Stars } from '../components/ui'
import { useAsync, useStoreConfig } from '../lib/hooks'
import { inr, fmtDate } from '../lib/format'
import { useCart } from '../context/CartContext'
import { useWishlist } from '../context/WishlistContext'
import { useToast } from '../context/ToastContext'

const label = (v) => [v.color, v.size].filter(Boolean).join(' / ') || 'Standard'

function DetailSkeleton() {
  return (
    <div className="page grid gap-10 py-10 md:grid-cols-2" aria-busy="true">
      <div className="skeleton aspect-square rounded-md" />
      <div className="space-y-4">
        <div className="skeleton h-4 w-1/4" /><div className="skeleton h-10 w-3/4" /><div className="skeleton h-8 w-1/3" />
        <div className="skeleton h-24 w-full" /><div className="skeleton h-12 w-full" />
      </div>
    </div>
  )
}

function OptionButton({ selected, disabled, onClick, children }) {
  return (
    <button type="button" disabled={disabled} onClick={onClick} aria-pressed={selected}
      className={`min-w-[52px] rounded-sm border px-4 py-2.5 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:border-dashed disabled:text-ink-faint disabled:line-through ${selected ? 'border-secondary bg-secondary text-white' : 'border-tertiary bg-white hover:border-secondary'}`}>
      {children}
    </button>
  )
}

export default function ProductDetail() {
  const { id } = useParams()
  const { data: p, loading, error, reload } = useAsync(() => api.get(`/products/${id}`).then((r) => r.data), [id])
  const reviews = useAsync(() => p ? api.get(`/products/${p.id}/reviews`).then((r) => r.data) : Promise.resolve(null), [p?.id])
  const cfg = useStoreConfig()
  const { add, busy } = useCart()
  const { ids, toggle } = useWishlist()
  const toast = useToast()
  const [vid, setVid] = useState(null)
  const [selectedImage, setSelectedImage] = useState(0)
  const [qty, setQty] = useState(1)
  const [rv, setRv] = useState({ rating: 5, body: '' })

  useEffect(() => {
    if (p) { setVid((p.variants.find((v) => v.stockQty > 0) || p.variants[0])?.id); setSelectedImage(0); setQty(1) }
  }, [p])
  useEffect(() => { document.title = p ? `${p.name} | ${cfg?.storeName || 'CapStore'}` : (cfg?.storeName || 'CapStore') }, [p, cfg?.storeName])

  if (loading) return <DetailSkeleton />
  if (error) return <ErrorBox message={error} onRetry={reload} />

  const variant = p.variants.find((v) => v.id === vid)
  const price = Number(variant?.price ?? p.basePrice)
  const images = p.imageUrls?.length ? p.imageUrls : p.imageUrl ? [p.imageUrl] : []
  const inStock = variant && variant.stockQty > 0
  const clampQty = (n) => Math.max(1, Math.min(n || 1, variant?.stockQty || 1))

  // Split into colour + size pickers when every variant has both; otherwise fall back to a flat list.
  const colours = [...new Set(p.variants.map((v) => v.color).filter(Boolean))]
  const sizes = [...new Set(p.variants.map((v) => v.size).filter(Boolean))]
  const split = colours.length > 0 && sizes.length > 0 && p.variants.every((v) => v.color && v.size)
  const pick = (color, size) => {
    const exact = p.variants.find((v) => v.color === color && v.size === size)
    const fallback = p.variants.find((v) => v.color === color && v.stockQty > 0) || p.variants.find((v) => v.color === color)
    const next = exact && exact.stockQty > 0 ? exact : fallback || exact
    if (next) { setVid(next.id); setQty(1) }
  }

  const submitReview = async (e) => {
    e.preventDefault()
    try { await api.post(`/products/${p.id}/reviews`, rv); toast('Thanks for your review'); setRv({ rating: 5, body: '' }); reviews.reload() }
    catch (err) { toast(errMsg(err), 'error') }
  }

  return (
    <div className="page py-8">
      <nav className="mb-6 flex items-center gap-1.5 text-sm text-ink-faint" aria-label="Breadcrumb">
        <Link to="/products" className="hover:text-primary">Shop</Link>
        <ChevronRight size={14} />
        <Link to={`/products?categoryId=${p.categoryId}`} className="hover:text-primary">{p.categoryName}</Link>
        <ChevronRight size={14} />
        <span className="truncate text-ink" aria-current="page">{p.name}</span>
      </nav>

      <div className="grid gap-10 md:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)] lg:gap-16">
        {/* Gallery */}
        <div className="flex min-w-0 flex-col-reverse gap-3 lg:flex-row lg:items-start">
          {images.length > 1 && (
            <div className="scrollbar-none flex gap-3 overflow-x-auto lg:w-20 lg:flex-col" aria-label="Product images">
              {images.map((image, index) => (
                <button key={`${image}-${index}`} type="button" onClick={() => setSelectedImage(index)} aria-label={`Show image ${index + 1}`} aria-pressed={selectedImage === index}
                  className={`aspect-square w-20 shrink-0 overflow-hidden rounded-sm border-2 transition ${selectedImage === index ? 'border-primary' : 'border-transparent opacity-70 hover:opacity-100'}`}>
                  <img src={image} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
          <div className="flex aspect-square w-full min-w-0 flex-1 items-center justify-center overflow-hidden rounded-md bg-twill">
            {images.length
              ? <img src={images[selectedImage] || images[0]} alt={`${p.name}, image ${selectedImage + 1} of ${images.length}`} className="h-full w-full object-cover" />
              : <CapMark className="w-3/5" color={colorFor(p.name)} />}
          </div>
        </div>

        {/* Buy panel */}
        <div className="min-w-0 md:sticky md:top-24 md:h-fit">
          <p className="eyebrow">{p.categoryName}</p>
          <h1 className="mt-2 font-display text-4xl font-semibold uppercase leading-[0.95] sm:text-5xl">{p.name}</h1>
          {reviews.data?.count > 0 && (
            <a href="#reviews" className="mt-3 inline-flex items-center gap-2 text-sm hover:text-primary"><Stars value={reviews.data.average} /><span className="text-ink-soft">{Number(reviews.data.average).toFixed(1)} · {reviews.data.count} {reviews.data.count === 1 ? 'review' : 'reviews'}</span></a>
          )}
          <div className="mt-5 flex flex-wrap items-baseline gap-x-3 gap-y-1">
            <p className="text-3xl font-semibold">{inr(price)}</p>
            <p className="text-sm text-ink-faint">+ GST at checkout</p>
          </div>

          <div className="mt-8 space-y-6 border-t border-tertiary pt-6">
            {split ? (
              <>
                <div>
                  <p className="label">Colour: <span className="font-normal text-ink-soft">{variant?.color}</span></p>
                  <div className="flex flex-wrap gap-2">
                    {colours.map((c) => <OptionButton key={c} selected={variant?.color === c} disabled={!p.variants.some((v) => v.color === c && v.stockQty > 0)} onClick={() => pick(c, variant?.size)}>{c}</OptionButton>)}
                  </div>
                </div>
                <div>
                  <p className="label">Size: <span className="font-normal text-ink-soft">{variant?.size}</span></p>
                  <div className="flex flex-wrap gap-2">
                    {sizes.map((s) => {
                      const v = p.variants.find((x) => x.color === variant?.color && x.size === s)
                      return <OptionButton key={s} selected={variant?.size === s} disabled={!v || v.stockQty === 0} onClick={() => pick(variant?.color, s)}>{s}</OptionButton>
                    })}
                  </div>
                </div>
              </>
            ) : p.variants.length > 1 || (p.variants[0] && label(p.variants[0]) !== 'Standard') ? (
              <div>
                <p className="label">Option</p>
                <div className="flex flex-wrap gap-2">
                  {p.variants.map((v) => <OptionButton key={v.id} selected={v.id === vid} disabled={v.stockQty === 0} onClick={() => { setVid(v.id); setQty(1) }}>{label(v)}</OptionButton>)}
                </div>
              </div>
            ) : null}

            {variant && (
              <p className={`flex items-center gap-2 text-sm font-medium ${variant.stockQty === 0 ? 'text-brim' : variant.stockQty <= 10 ? 'text-warn' : 'text-moss'}`}>
                <span className="h-2 w-2 rounded-full bg-current" />
                {variant.stockQty === 0 ? 'Out of stock' : variant.stockQty <= 10 ? `Only ${variant.stockQty} left` : 'In stock, ready to ship'}
              </p>
            )}

            <div className="flex gap-2 sm:gap-3">
              <div className="flex h-[46px] shrink-0 items-center rounded-sm border border-tertiary">
                <button className="flex h-full w-11 items-center justify-center hover:text-primary disabled:opacity-40" onClick={() => setQty(clampQty(qty - 1))} disabled={qty <= 1} aria-label="Decrease quantity"><Minus size={16} /></button>
                <input className="h-full w-12 border-0 bg-transparent text-center text-sm font-semibold outline-none" inputMode="numeric" value={qty} onChange={(e) => setQty(clampQty(parseInt(e.target.value, 10)))} aria-label="Quantity" />
                <button className="flex h-full w-11 items-center justify-center hover:text-primary disabled:opacity-40" onClick={() => setQty(clampQty(qty + 1))} disabled={!variant || qty >= variant.stockQty} aria-label="Increase quantity"><Plus size={16} /></button>
              </div>
              <button className="btn-primary min-w-0 flex-1 px-4" disabled={!inStock || busy} onClick={() => add(p, variant, qty)}>
                {!inStock ? 'Out of stock' : busy ? 'Adding…' : <>Add to cart<span className="hidden sm:inline"> · {inr(price * qty)}</span></>}
              </button>
              <button className={`btn-outline w-[46px] shrink-0 px-0 ${ids.has(p.id) ? 'border-primary text-primary hover:bg-primary hover:text-white' : ''}`} onClick={() => toggle(p.id)} aria-pressed={ids.has(p.id)} aria-label={ids.has(p.id) ? 'Remove from wishlist' : 'Save to wishlist'}>
                <Heart size={18} className={ids.has(p.id) ? 'fill-current' : ''} />
              </button>
            </div>
          </div>

          {p.description && (
            <div className="mt-8 border-t border-tertiary pt-6">
              <h2 className="eyebrow mb-3 text-ink">Details</h2>
              <p className="whitespace-pre-line leading-7 text-ink-soft">{p.description}</p>
            </div>
          )}

          <ul className="mt-8 space-y-3 border-t border-tertiary pt-6 text-sm">
            <li className="flex items-center gap-2.5"><Truck size={18} className="shrink-0 text-primary" />{cfg?.freeShippingAbove != null ? `Free shipping over ${inr(cfg.freeShippingAbove)}` : 'Tracked delivery'}</li>
            <li className="flex items-center gap-2.5"><ShieldCheck size={18} className="shrink-0 text-primary" />Secure Razorpay checkout</li>
            {p.returnable !== false && <li className="flex items-center gap-2.5"><RotateCcw size={18} className="shrink-0 text-primary" />Easy returns</li>}
          </ul>
        </div>
      </div>

      <section id="reviews" className="mt-20 scroll-mt-24 border-t border-tertiary pt-12">
        <div className="grid gap-10 lg:grid-cols-[280px_1fr]">
          <div>
            <h2 className="font-display text-3xl font-semibold uppercase">Reviews</h2>
            {reviews.data?.count > 0 ? (
              <div className="mt-4">
                <p className="text-5xl font-semibold">{Number(reviews.data.average).toFixed(1)}</p>
                <Stars value={reviews.data.average} size={18} />
                <p className="mt-1 text-sm text-ink-soft">Based on {reviews.data.count} {reviews.data.count === 1 ? 'review' : 'reviews'}</p>
              </div>
            ) : <p className="mt-3 text-sm text-ink-soft">No reviews yet. Reviews come from verified buyers only.</p>}
          </div>
          <div>
            <ul className="divide-y divide-tertiary">
              {reviews.data?.reviews.map((r) => (
                <li key={r.id} className="py-5 first:pt-0">
                  <div className="flex items-center justify-between gap-3"><Stars value={r.rating} size={14} /><span className="text-xs text-ink-faint">{fmtDate(r.createdAt)}</span></div>
                  {r.body && <p className="mt-2 leading-7 text-ink">{r.body}</p>}
                  <p className="mt-2 text-sm font-medium text-ink-soft">{r.userName} · Verified buyer</p>
                </li>
              ))}
            </ul>
            {reviews.data?.canReview && (
              <form onSubmit={submitReview} className="panel mt-6 space-y-4">
                <h3 className="font-semibold">Write a review</h3>
                <div>
                  <span className="label">Your rating</span>
                  <div className="flex gap-1" role="radiogroup" aria-label="Rating">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button key={n} type="button" role="radio" aria-checked={rv.rating === n} aria-label={`${n} star${n > 1 ? 's' : ''}`} onClick={() => setRv({ ...rv, rating: n })} className="p-0.5">
                        <svg viewBox="0 0 24 24" className={`h-7 w-7 ${n <= rv.rating ? 'fill-primary text-primary' : 'fill-none text-ink/30'}`} stroke="currentColor" strokeWidth="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                      </button>
                    ))}
                  </div>
                </div>
                <Field label="Your review"><textarea className="input" rows={4} maxLength={1000} value={rv.body} onChange={(e) => setRv({ ...rv, body: e.target.value })} placeholder="How's the fit, fabric and finish?" /></Field>
                <button className="btn-primary">Submit review</button>
              </form>
            )}
          </div>
        </div>
      </section>
    </div>
  )
}
