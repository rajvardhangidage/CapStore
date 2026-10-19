import { Link } from 'react-router-dom'
import { Heart } from 'lucide-react'
import { CapMark, colorFor } from './ui'
import { inr } from '../lib/format'
import { useWishlist } from '../context/WishlistContext'

export default function ProductCard({ p }) {
  const { ids, toggle } = useWishlist()
  const liked = ids.has(p.id)
  const stock = p.variants.reduce((s, v) => s + v.stockQty, 0)
  const prices = p.variants.map((v) => Number(v.price ?? p.basePrice))
  const from = prices.length ? Math.min(...prices) : Number(p.basePrice)
  const coverImage = p.imageUrls?.[0] || p.imageUrl
  return (
    <div className="group card relative overflow-hidden transition-all duration-300 hover:-translate-y-1 hover:border-white hover:bg-white/85 hover:shadow-[0_24px_60px_-30px_rgba(27,42,73,.38)]">
      <Link to={`/products/${p.publicId}`} className="block">
        <div className="relative flex aspect-square items-center justify-center overflow-hidden bg-gradient-to-br from-[#eef1f6] via-[#f8f9fb] to-[#e8edf4]">
          {coverImage
            ? <img src={coverImage} alt={p.name} loading="lazy" className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105" />
            : <CapMark className="w-2/3 drop-shadow-[0_18px_16px_rgba(27,42,73,.18)] transition-transform duration-500 group-hover:scale-105" color={colorFor(p.name)} />}
          {stock === 0 && <span className="absolute left-2 top-2 rounded bg-brim px-2 py-0.5 text-xs font-bold text-white">Sold out</span>}
        </div>
        <div className="p-3">
          <p className="text-[10px] font-bold uppercase tracking-[.16em] text-ink-faint">{p.categoryName}</p>
          <h3 className="line-clamp-2 text-sm font-bold leading-snug sm:text-base">{p.name}</h3>
          <p className="mt-1 font-bold">{inr(from)}</p>
        </div>
      </Link>
      <button
        onClick={() => toggle(p.id)}
        aria-label={liked ? 'Remove from wishlist' : 'Add to wishlist'}
        className="absolute right-2 top-2 rounded-full bg-white/90 p-2 shadow-sm hover:bg-white"
      >
        <Heart size={18} className={liked ? 'fill-brim text-brim' : 'text-ink-soft'} />
      </button>
    </div>
  )
}
