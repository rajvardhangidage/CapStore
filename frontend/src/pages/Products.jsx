import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SlidersHorizontal, X } from 'lucide-react'
import api from '../api'
import ProductCard from '../components/ProductCard'
import { EmptyState, ErrorBox, Pagination, ProductGridSkeleton } from '../components/ui'
import { useAsync } from '../lib/hooks'
import { inr } from '../lib/format'

const SORTS = [['', 'Newest'], ['price_asc', 'Price: low to high'], ['price_desc', 'Price: high to low'], ['name', 'Name: A–Z']]
const PRICE_PRESETS = [['', '500', 'Under ₹500'], ['500', '1000', '₹500 – ₹1,000'], ['1000', '', 'Over ₹1,000']]

export default function Products() {
  const [sp, setSp] = useSearchParams()
  const [showFilters, setShowFilters] = useState(false)
  const get = (k) => sp.get(k) || ''
  const page = Number(get('page') || 0)
  const cats = useAsync(() => api.get('/categories').then((r) => r.data))
  const [price, setPrice] = useState({ min: get('minPrice'), max: get('maxPrice') })

  // Keep the price inputs in sync when filters change through the URL (chips, back button, "clear all").
  useEffect(() => { setPrice({ min: sp.get('minPrice') || '', max: sp.get('maxPrice') || '' }) }, [sp])

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

  const category = cats.data?.find((c) => String(c.id) === get('categoryId'))
  const active = [
    get('q') && ['q', `“${get('q')}”`],
    category && ['categoryId', category.name],
    (get('minPrice') || get('maxPrice')) && ['price', get('minPrice') && get('maxPrice') ? `${inr(get('minPrice'))} – ${inr(get('maxPrice'))}` : get('minPrice') ? `Over ${inr(get('minPrice'))}` : `Under ${inr(get('maxPrice'))}`],
  ].filter(Boolean)
  const clear = (key) => set(key === 'price' ? { minPrice: '', maxPrice: '' } : { [key]: '' })
  const clearAll = () => setSp(get('sort') ? { sort: get('sort') } : {})
  const applyPrice = (e) => { e.preventDefault(); set({ minPrice: price.min.replace(/\D/g, ''), maxPrice: price.max.replace(/\D/g, '') }) }

  return (
    <div className="page py-10">
      <header className="mb-8 border-b border-tertiary pb-8">
        <p className="eyebrow mb-2">Shop</p>
        <h1 className="page-title">{get('q') ? `Results for “${get('q')}”` : category?.name || 'All caps'}</h1>
        <div className="scrollbar-none -mx-4 mt-6 flex gap-2 overflow-x-auto px-4 sm:mx-0 sm:flex-wrap sm:px-0">
          <button onClick={() => set({ categoryId: '' })} className={`chip-outline shrink-0 ${!get('categoryId') ? 'chip-active' : ''}`}>All</button>
          {cats.data?.map((c) => (
            <button key={c.id} onClick={() => set({ categoryId: String(c.id) })} aria-pressed={get('categoryId') === String(c.id)} className={`chip-outline shrink-0 ${get('categoryId') === String(c.id) ? 'chip-active' : ''}`}>{c.name}</button>
          ))}
        </div>
      </header>

      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <button className="btn-outline btn-sm lg:hidden" onClick={() => setShowFilters(!showFilters)} aria-expanded={showFilters}><SlidersHorizontal size={14} /> Filters</button>
          <p className="text-sm text-ink-soft" aria-live="polite">{data ? `${data.totalElements} ${data.totalElements === 1 ? 'cap' : 'caps'}` : ' '}</p>
          {active.map(([key, label]) => (
            <button key={key} onClick={() => clear(key)} className="chip hover:bg-primary hover:text-white" aria-label={`Remove filter ${label}`}>{label}<X size={12} /></button>
          ))}
          {active.length > 1 && <button onClick={() => clearAll()} className="text-xs font-medium text-ink-soft underline underline-offset-4 hover:text-primary">Clear all</button>}
        </div>
        <label className="flex items-center gap-2 text-sm">
          <span className="hidden text-ink-soft sm:inline">Sort by</span>
          <select className="input !min-h-[38px] !w-auto !py-1.5 pr-8 text-sm" value={get('sort')} onChange={(e) => set({ sort: e.target.value })} aria-label="Sort">
            {SORTS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
      </div>

      <div className="grid gap-10 lg:grid-cols-[220px_1fr]">
        <aside className={`${showFilters ? 'block' : 'hidden'} h-fit space-y-8 lg:sticky lg:top-24 lg:block`} aria-label="Filters">
          <div>
            <h3 className="eyebrow mb-3 text-ink">Price</h3>
            <div className="flex flex-col gap-1">
              {PRICE_PRESETS.map(([min, max, label]) => {
                const on = get('minPrice') === min && get('maxPrice') === max
                return (
                  <button key={label} onClick={() => set(on ? { minPrice: '', maxPrice: '' } : { minPrice: min, maxPrice: max })} aria-pressed={on}
                    className={`flex items-center gap-2.5 rounded-sm px-2 py-2 text-left text-sm transition-colors hover:bg-twill ${on ? 'font-medium text-primary' : ''}`}>
                    <span className={`h-4 w-4 rounded-full border ${on ? 'border-[5px] border-primary' : 'border-ink/30'}`} />{label}
                  </button>
                )
              })}
            </div>
            <form onSubmit={applyPrice} className="mt-4">
              <div className="flex items-center gap-2">
                <input className="input !min-h-[40px] !px-3" inputMode="numeric" placeholder="Min ₹" value={price.min} onChange={(e) => setPrice({ ...price, min: e.target.value })} aria-label="Minimum price" />
                <span className="text-ink-faint">–</span>
                <input className="input !min-h-[40px] !px-3" inputMode="numeric" placeholder="Max ₹" value={price.max} onChange={(e) => setPrice({ ...price, max: e.target.value })} aria-label="Maximum price" />
              </div>
              <button className="btn-outline btn-sm mt-2 w-full">Apply</button>
            </form>
          </div>
        </aside>

        <div>
          {loading ? <ProductGridSkeleton count={9} className="grid-cols-2 md:grid-cols-3" />
            : error ? <ErrorBox message={error} onRetry={reload} />
            : data.content.length === 0 ? (
              <EmptyState title="No caps found" text="Try a different search or clear the filters.">
                {active.length > 0 && <button className="btn-outline" onClick={() => clearAll()}>Clear filters</button>}
              </EmptyState>
            ) : <>
                <div className="grid grid-cols-2 gap-x-4 gap-y-10 sm:gap-x-6 md:grid-cols-3">{data.content.map((p) => <ProductCard key={p.id} p={p} />)}</div>
                <Pagination page={data.page} totalPages={data.totalPages} onChange={(p) => { set({ page: String(p) }); window.scrollTo({ top: 0, behavior: 'smooth' }) }} />
              </>}
        </div>
      </div>
    </div>
  )
}
