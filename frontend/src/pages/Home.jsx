import { Link } from 'react-router-dom'
import { ArrowRight, ArrowUpRight, Factory, FileText, RotateCcw, ShieldCheck, Truck } from 'lucide-react'
import api from '../api'
import ProductCard from '../components/ProductCard'
import { CapMark, ErrorBox, ProductGridSkeleton } from '../components/ui'
import { useAsync, useStoreConfig } from '../lib/hooks'
import { inr } from '../lib/format'

const MARQUEE = ['Made in our own workshop', 'New styles every season', 'Easy returns', 'GST invoices', 'Ships across India']

function Hero({ hero }) {
  const title = hero?.title || 'Caps made to fit your everyday.'
  const subtitle = hero?.subtitle || 'Cotton caps, snapbacks and bucket hats, made in our own workshop and shipped across India.'
  const cta = hero?.linkUrl || '/products'

  if (hero?.imageUrl) {
    return (
      <section className="relative isolate overflow-hidden bg-secondary text-white">
        <img src={hero.imageUrl} alt="" className="absolute inset-0 -z-10 h-full w-full object-cover" />
        <div className="absolute inset-0 -z-10 bg-black/45" />
        <div className="page flex min-h-[520px] flex-col justify-end py-14 md:min-h-[640px] md:py-20">
          <p className="eyebrow text-white/80">New season</p>
          <h1 className="mt-3 max-w-4xl font-display text-5xl font-semibold uppercase leading-[0.95] sm:text-6xl lg:text-[5.5rem]">{title}</h1>
          <p className="mt-5 max-w-xl text-lg text-white/85">{subtitle}</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to={cta} className="btn-primary btn-lg">Shop now <ArrowRight size={18} /></Link>
            <Link to="/about" className="btn btn-lg border border-white bg-transparent text-white hover:bg-white hover:text-ink">Our story</Link>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="border-b border-tertiary">
      <div className="page grid items-center gap-10 py-12 md:grid-cols-[1.15fr_1fr] md:py-20">
        <div className="animate-fade-up">
          <p className="chip">Made in our own workshop</p>
          <h1 className="mt-5 font-display text-5xl font-semibold uppercase leading-[0.95] tracking-[-0.02em] sm:text-6xl lg:text-[5rem]">{title}</h1>
          <p className="mt-6 max-w-lg text-lg leading-8 text-ink-soft">{subtitle}</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to={cta} className="btn-primary btn-lg">Shop caps <ArrowRight size={18} /></Link>
            <Link to="/about" className="btn-outline btn-lg">Our story</Link>
          </div>
        </div>
        <div className="relative aspect-square overflow-hidden rounded-md bg-primary">
          <span className="pointer-events-none absolute -left-4 top-4 select-none font-display text-[9rem] font-semibold uppercase leading-none text-white/10 sm:text-[11rem]" aria-hidden="true">Caps</span>
          <CapMark className="absolute left-1/2 top-1/2 w-3/5 -translate-x-1/2 -translate-y-1/2 drop-shadow-[0_24px_24px_rgba(0,0,0,.25)]" color="#ffffff" />
          <div className="absolute inset-x-5 bottom-5 flex items-center justify-between text-white">
            <span className="text-xs font-medium uppercase tracking-[0.08em]">Est. workshop · India</span>
            <Link to="/products" className="flex h-11 w-11 items-center justify-center rounded-full bg-white text-primary transition-transform hover:rotate-45" aria-label="Shop all caps"><ArrowUpRight size={20} /></Link>
          </div>
        </div>
      </div>
    </section>
  )
}

function Marquee() {
  const row = [...MARQUEE, ...MARQUEE]
  return (
    <div className="overflow-hidden border-b border-tertiary py-4" aria-hidden="true">
      <div className="flex w-max animate-marquee whitespace-nowrap">
        {[...row, ...row].map((t, i) => (
          <span key={i} className="flex items-center gap-10 pr-10 font-display text-xl font-medium uppercase tracking-[-0.01em] text-ink sm:text-2xl">
            {t}<span className="h-2 w-2 rounded-full bg-primary" />
          </span>
        ))}
      </div>
    </div>
  )
}

function SectionHead({ eyebrow, title, to, cta = 'View all' }) {
  return (
    <div className="mb-8 flex items-end justify-between gap-4">
      <div>
        {eyebrow && <p className="eyebrow mb-2">{eyebrow}</p>}
        <h2 className="font-display text-3xl font-semibold uppercase leading-none sm:text-[2.75rem]">{title}</h2>
      </div>
      {to && <Link to={to} className="group flex shrink-0 items-center gap-1.5 text-sm font-medium uppercase tracking-[0.06em] hover:text-primary">{cta}<ArrowRight size={16} className="transition-transform group-hover:translate-x-1" /></Link>}
    </div>
  )
}

export default function Home() {
  const cfg = useStoreConfig()
  const content = useAsync(async () => {
    const [banners, cats] = await Promise.all([api.get('/banners'), api.get('/categories')])
    return { banners: banners.data, cats: cats.data }
  })
  const prods = useAsync(() => api.get('/products', { params: { size: 8, sort: 'newest' } }).then((r) => r.data.content))

  if (content.error) return <ErrorBox message={content.error} onRetry={content.reload} />
  const hero = content.data?.banners[0]
  const more = content.data?.banners.slice(1) || []

  return (
    <>
      {content.loading ? <div className="skeleton h-[520px] rounded-none" /> : <Hero hero={hero} />}
      <Marquee />

      {content.data?.cats.length > 0 && (
        <section className="page pt-14">
          <div className="scrollbar-none -mx-4 flex gap-2 overflow-x-auto px-4 sm:mx-0 sm:flex-wrap sm:px-0">
            <Link to="/products" className="chip-outline chip-active shrink-0">All caps</Link>
            {content.data.cats.map((c) => <Link key={c.id} to={`/products?categoryId=${c.id}`} className="chip-outline shrink-0">{c.name}</Link>)}
          </div>
        </section>
      )}

      <section className="page py-14">
        <SectionHead eyebrow="Just dropped" title="New arrivals" to="/products" />
        {prods.loading ? <ProductGridSkeleton />
          : prods.error ? <ErrorBox message={prods.error} onRetry={prods.reload} />
          : <div className="grid grid-cols-2 gap-x-4 gap-y-10 sm:gap-x-6 md:grid-cols-3 lg:grid-cols-4">{prods.data.map((p) => <ProductCard key={p.id} p={p} />)}</div>}
      </section>

      {more.length > 0 && (
        <section className="page pb-14">
          <div className={`grid gap-4 ${more.length > 1 ? 'md:grid-cols-2' : ''}`}>
            {more.map((b) => (
              <Link key={b.id} to={b.linkUrl || '/products'} className="group relative isolate flex min-h-[260px] flex-col justify-end overflow-hidden rounded-md bg-secondary p-7 text-white">
                {b.imageUrl && <img src={b.imageUrl} alt="" className="absolute inset-0 -z-10 h-full w-full object-cover opacity-60 transition-transform duration-700 group-hover:scale-105" />}
                <h3 className="font-display text-3xl font-semibold uppercase leading-none">{b.title}</h3>
                {b.subtitle && <p className="mt-2 text-white/80">{b.subtitle}</p>}
                <span className="mt-5 flex items-center gap-1.5 text-sm font-medium uppercase tracking-[0.06em]">Shop now <ArrowRight size={16} className="transition-transform group-hover:translate-x-1" /></span>
              </Link>
            ))}
          </div>
        </section>
      )}

      <section className="page">
        <div className="grid overflow-hidden rounded-md bg-primary text-white md:grid-cols-2">
          <div className="p-8 sm:p-12">
            <p className="eyebrow text-white/70">Why shop with us</p>
            <h2 className="mt-3 font-display text-4xl font-semibold uppercase leading-[0.95] sm:text-5xl">Our workshop.<br />Your style.</h2>
            <p className="mt-5 max-w-md text-white/80">Every cap is cut, stitched and checked in our own workshop, then shipped straight to you. No middlemen, no compromises.</p>
            <Link to="/products" className="btn mt-8 border border-white bg-white text-primary hover:bg-primary-soft">Shop the collection <ArrowRight size={16} /></Link>
          </div>
          <ul className="grid gap-px bg-white/15">
            {[[Factory, 'Made in-house', 'Quality materials, checked by the people who make them.'], [RotateCcw, 'Easy returns', 'Return or replace eligible items right from your orders page.'], [FileText, 'GST invoices', 'Download a tax invoice for every paid order.']].map(([Icon, t, d]) => (
              <li key={t} className="flex gap-4 bg-primary p-6 sm:px-10">
                <Icon className="mt-0.5 shrink-0" size={22} />
                <div><h3 className="font-semibold">{t}</h3><p className="mt-1 text-sm text-white/75">{d}</p></div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="page mt-14 grid gap-px overflow-hidden rounded-md border border-tertiary bg-tertiary sm:grid-cols-3">
        {[
          [ShieldCheck, 'Secure payments', 'UPI, cards and netbanking through Razorpay.'],
          [Truck, cfg?.freeShippingAbove != null ? `Free shipping over ${inr(cfg.freeShippingAbove)}` : 'Track every order', 'See each step from packing to your doorstep.'],
          [Factory, 'Straight from the maker', 'No middlemen, so you get workshop prices.'],
        ].map(([Icon, t, d]) => (
          <div key={t} className="flex gap-4 bg-white p-6">
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary-soft text-primary"><Icon size={20} /></span>
            <div><h3 className="font-semibold">{t}</h3><p className="mt-1 text-sm text-ink-soft">{d}</p></div>
          </div>
        ))}
      </section>
    </>
  )
}
