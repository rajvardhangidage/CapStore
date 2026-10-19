import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SlidersHorizontal } from 'lucide-react'
import api from '../api'
import ProductCard from '../components/ProductCard'
import { EmptyState, ErrorBox, Pagination, Spinner } from '../components/ui'
import { useAsync } from '../lib/hooks'

export default function Products() {
  const [sp, setSp] = useSearchParams()
  const [showFilters, setShowFilters] = useState(false)
  const get = (k) => sp.get(k) || ''
  const page = Number(get('page') || 0)
  const cats = useAsync(() => api.get('/categories').then((r) => r.data))
  const [price, setPrice] = useState({ min: get('minPrice'), max: get('maxPrice') })

  const { data, loading, error, reload } = useAsync(
    () => api.get('/products', { params: {
      q: get('q') || undefined, categoryId: get('categoryId') || undefined, minPrice: get('minPrice') || undefined,
      maxPrice: get('maxPrice') || undefined, sort: get('sort') || undefined, page, size: 12 } }).then((r) => r.data),
    [sp.toString()]
  )

  const set = (patch) => {
    const n = new URLSearchParams(sp)
    Object.entries(patch).forEach(([k, v]) => (v ? n.set(k, v) : n.delete(k)))
    if (!('page' in patch)) n.delete('page')
    setSp(n)
  }

  const Filters = (
    <div className="space-y-6">
      <div>
        <h3 className="mb-2 text-sm font-bold">Category</h3>
        <div className="flex flex-col gap-1">
          <button onClick={() => set({ categoryId: '' })} className={`rounded px-2 py-1.5 text-left text-sm ${!get('categoryId') ? 'bg-ink font-bold text-white' : 'hover:bg-twill'}`}>All caps</button>
          {cats.data?.map((c) => (
            <button key={c.id} onClick={() => set({ categoryId: String(c.id) })} className={`rounded px-2 py-1.5 text-left text-sm ${get('categoryId') === String(c.id) ? 'bg-ink font-bold text-white' : 'hover:bg-twill'}`}>{c.name}</button>
          ))}
        </div>
      </div>
      <div>
        <h3 className="mb-2 text-sm font-bold">Price (₹)</h3>
        <div className="flex items-center gap-2">
          <input className="input" inputMode="numeric" placeholder="Min" value={price.min} onChange={(e) => setPrice({ ...price, min: e.target.value })} aria-label="Minimum price" />
          <span>–</span>
          <input className="input" inputMode="numeric" placeholder="Max" value={price.max} onChange={(e) => setPrice({ ...price, max: e.target.value })} aria-label="Maximum price" />
        </div>
        <button className="btn-outline btn-sm mt-2 w-full" onClick={() => set({ minPrice: price.min, maxPrice: price.max })}>Apply</button>
      </div>
    </div>
  )

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold">{get('q') ? `Results for "${get('q')}"` : 'All caps'}</h1>
          {data && <p className="text-sm text-ink-soft">{data.totalElements} {data.totalElements === 1 ? 'cap' : 'caps'}</p>}
        </div>
        <div className="flex items-center gap-2">
          <button className="btn-outline btn-sm md:hidden" onClick={() => setShowFilters(!showFilters)}><SlidersHorizontal size={14} /> Filters</button>
          <select className="input !w-auto" value={get('sort')} onChange={(e) => set({ sort: e.target.value })} aria-label="Sort">
            <option value="">Newest</option>
            <option value="price_asc">Price: low to high</option>
            <option value="price_desc">Price: high to low</option>
          </select>
        </div>
      </div>
      <div className="grid gap-8 md:grid-cols-[220px_1fr]">
        <aside className={`${showFilters ? 'block' : 'hidden'} card h-fit p-4 md:block`}>{Filters}</aside>
        <div>
          {loading ? <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
            : error ? <ErrorBox message={error} onRetry={reload} />
            : data.content.length === 0 ? <EmptyState title="No caps found" text="Try a different search or clear the filters." />
            : <>
                <div className="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3">{data.content.map((p) => <ProductCard key={p.id} p={p} />)}</div>
                <Pagination page={data.page} totalPages={data.totalPages} onChange={(p) => { set({ page: String(p) }); window.scrollTo({ top: 0 }) }} />
              </>}
        </div>
      </div>
    </div>
  )
}
