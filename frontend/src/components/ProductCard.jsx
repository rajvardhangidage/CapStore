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
  const varies = prices.length > 1 && Math.max(...prices) !== from
  const images = p.imageUrls?.length ? p.imageUrls : p.imageUrl ? [p.imageUrl] : []
  const colours = [...new Set(p.variants.map((v) => v.color).filter(Boolean))]
  const soldOut = stock === 0
  const low = !soldOut && stock <= 10

  return (
    <article className="group relative">
      <Link to={`/products/${p.publicId}`} className="block focus-visible:outline-offset-4">
        <div className="relative aspect-[4/5] overflow-hidden rounded-md bg-twill">
          {images.length ? (
            <>
              <img src={images[0]} alt={p.name} loading="lazy" className={`h-full w-full object-cover transition duration-500 group-hover:scale-[1.03] ${images[1] ? 'group-hover:opacity-0' : ''} ${soldOut ? 'opacity-60' : ''}`} />
              {images[1] && <img src={images[1]} alt="" loading="lazy" aria-hidden="true" className="absolute inset-0 h-full w-full object-cover opacity-0 transition duration-500 group-hover:opacity-100" />}
            </>
          ) : (
            <div className="flex h-full items-center justify-center">
              <CapMark className={`w-3/5 transition-transform duration-500 group-hover:-rotate-3 group-hover:scale-105 ${soldOut ? 'opacity-50' : ''}`} color={colorFor(p.name)} />
            </div>
          )}
          <div className="absolute left-3 top-3 flex flex-col items-start gap-1.5">
            {soldOut && <span className="rounded-full bg-secondary px-2.5 py-1 text-[11px] font-medium uppercase tracking-[0.04em] text-white">Sold out</span>}
            {low && <span className="rounded-full bg-white px-2.5 py-1 text-[11px] font-medium uppercase tracking-[0.04em] text-primary">Only {stock} left</span>}
          </div>
        </div>
        <div className="pt-3">
          <p className="eyebrow">{p.categoryName}</p>
          <h3 className="mt-1 line-clamp-2 text-[15px] font-medium leading-snug transition-colors group-hover:text-primary">{p.name}</h3>
          <div className="mt-1.5 flex items-center justify-between gap-2">
            <p className="font-semibold">{varies && <span className="mr-1 text-xs font-normal text-ink-faint">From</span>}{inr(from)}</p>
            {colours.length > 1 && <p className="text-xs text-ink-faint">{colours.length} colours</p>}
          </div>
        </div>
      </Link>
      <button
        onClick={() => toggle(p.id)}
        aria-label={liked ? `Remove ${p.name} from wishlist` : `Save ${p.name} to wishlist`}
        aria-pressed={liked}
        className="absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full bg-white text-ink transition hover:scale-105 hover:text-primary"
      >
        <Heart size={17} className={liked ? 'fill-primary text-primary' : ''} />
      </button>
    </article>
  )
}
