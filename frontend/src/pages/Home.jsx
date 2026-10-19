import { Link } from 'react-router-dom'
import { ArrowRight, BadgePercent, FileText, Truck, ShieldCheck, Factory } from 'lucide-react'
import api from '../api'
import ProductCard from '../components/ProductCard'
import { CapMark, ErrorBox, PageLoader } from '../components/ui'
import { useAsync } from '../lib/hooks'

export default function Home() {
  const { data, loading, error, reload } = useAsync(async () => {
    const [banners, cats, prods] = await Promise.all([
      api.get('/banners'), api.get('/categories'), api.get('/products', { params: { size: 8, sort: 'newest' } }),
    ])
    return { banners: banners.data, cats: cats.data, prods: prods.data.content }
  })
  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  const hero = data.banners[0]
  const more = data.banners.slice(1)

  return (
    <>
      <section className="mx-auto grid max-w-7xl items-center gap-8 px-4 py-8 md:grid-cols-2 md:py-14">
        <div className="relative z-10 py-4">
          <p className="mb-5 inline-flex items-center gap-2 rounded-full border border-thread/30 bg-white/60 px-4 py-2 text-[10px] font-extrabold uppercase tracking-[.2em] text-ink shadow-sm backdrop-blur"><span className="h-1.5 w-1.5 rounded-full bg-thread-dark" />Made in our own workshop</p>
          <h1 className="max-w-2xl text-5xl font-extrabold leading-[.98] sm:text-6xl lg:text-[4.5rem]">{hero?.title || 'Caps that fit your head and your brand.'}</h1>
          <p className="mt-5 max-w-lg text-base leading-7 text-ink-soft sm:text-lg">{hero?.subtitle || 'Cotton caps, snapbacks and bucket hats. Order one for yourself or a few hundred for your team.'}</p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to={hero?.linkUrl || '/products'} className="btn-primary">Shop caps <ArrowRight size={16} /></Link>
            <Link to="/register?type=business" className="btn-outline">Order in bulk</Link>
          </div>
        </div>
        <div className="glass-panel relative flex aspect-[4/3] items-center justify-center overflow-hidden rounded-[2rem] bg-gradient-to-br from-white/85 via-[#eef1f6]/80 to-[#dfe5ed]/75">
          <div className="pointer-events-none absolute -right-12 -top-20 h-64 w-64 rounded-full bg-thread/15 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-20 -left-12 h-64 w-64 rounded-full bg-ink/10 blur-3xl" />
          {hero?.imageUrl
            ? <img src={hero.imageUrl} alt="" className="relative h-full w-full object-cover" />
            : <>
                <div className="relative flex h-4/5 w-4/5 items-center justify-center rounded-[1.7rem] border border-white/70 bg-white/25 shadow-[0_30px_70px_-30px_rgba(27,42,73,.35)] backdrop-blur-sm">
                  <CapMark className="w-3/5 drop-shadow-[0_24px_20px_rgba(27,42,73,.2)]" />
                  <div className="absolute bottom-5 left-5 flex items-center gap-2 rounded-full border border-white/80 bg-white/65 px-3 py-2 shadow-lg backdrop-blur-xl">
                    {['#1b2a49', '#2f7d5b', '#c0392b', '#d6b06b', '#6b7794'].map((c) => <span key={c} className="h-4 w-4 rounded-full border border-white shadow-sm" style={{ background: c }} />)}
                    <span className="ml-1 text-[10px] font-bold uppercase tracking-wider text-ink-soft">Made to wear</span>
                  </div>
                </div>
              </>}
        </div>
      </section>

      {more.length > 0 && (
        <section className="mx-auto flex max-w-7xl gap-4 overflow-x-auto px-4 pb-6">
          {more.map((b) => (
            <Link key={b.id} to={b.linkUrl || '/products'} className="relative flex min-h-[140px] min-w-[280px] flex-1 flex-col justify-end overflow-hidden rounded-lg bg-ink p-5 text-white">
              {b.imageUrl && <img src={b.imageUrl} alt="" className="absolute inset-0 h-full w-full object-cover opacity-50" />}
              <h3 className="relative text-lg font-bold">{b.title}</h3>
              {b.subtitle && <p className="relative text-sm text-white/80">{b.subtitle}</p>}
            </Link>
          ))}
        </section>
      )}

      <section className="mx-auto max-w-7xl px-4 py-6">
        <div className="flex flex-wrap gap-2">
          {data.cats.map((c) => <Link key={c.id} to={`/products?categoryId=${c.id}`} className="rounded-full border-2 border-dashed border-ink/30 bg-white px-4 py-1.5 text-sm font-bold hover:border-ink">{c.name}</Link>)}
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-8">
        <div className="mb-5 flex items-end justify-between">
          <h2 className="text-2xl font-extrabold sm:text-3xl">New arrivals</h2>
          <Link to="/products" className="text-sm font-bold underline underline-offset-4">See all</Link>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3 lg:grid-cols-4">
          {data.prods.map((p) => <ProductCard key={p.id} p={p} />)}
        </div>
      </section>

      <section className="mx-auto my-10 max-w-7xl px-4">
        <div className="relative overflow-hidden rounded-[2rem] border border-white/10 bg-gradient-to-br from-[#17233e] via-ink to-[#31466a] p-6 text-white shadow-[0_30px_80px_-40px_rgba(27,42,73,.65)] sm:p-10">
          <div className="pointer-events-none absolute -right-16 -top-28 h-72 w-72 rounded-full border border-white/10" />
          <div className="pointer-events-none absolute -right-2 -top-14 h-44 w-44 rounded-full border border-thread/20" />
          <div className="grid gap-8 md:grid-cols-2 md:items-center">
            <div>
              <h2 className="text-3xl font-extrabold">Ordering for a team, school or brand?</h2>
              <p className="mt-2 text-white/75">Open a business account and get volume pricing, tax invoices for your accounts team, and quotes for custom logo embroidery.</p>
              <Link to="/register?type=business" className="btn-thread mt-5">Open a business account</Link>
            </div>
            <ul className="space-y-4 text-sm">
              <li className="flex gap-3"><BadgePercent className="shrink-0 text-thread" /><span><b>Bulk price tiers.</b> The more you order, the lower the price per cap.</span></li>
              <li className="flex gap-3"><FileText className="shrink-0 text-thread" /><span><b>GST invoices.</b> Download a tax invoice for every paid order.</span></li>
              <li className="flex gap-3"><Factory className="shrink-0 text-thread" /><span><b>Custom branding.</b> Upload your logo and request a quote.</span></li>
            </ul>
          </div>
        </div>
      </section>

      <section className="mx-auto grid max-w-7xl gap-4 px-4 sm:grid-cols-3">
        {[[ShieldCheck, 'Secure payments', 'UPI, cards and netbanking through Razorpay.'], [Truck, 'Track every order', 'See each step from packing to your doorstep.'], [Factory, 'Straight from the maker', 'No middlemen, so you get workshop prices.']].map(([Icon, t, d]) => (
          <div key={t} className="flex gap-3 p-2"><Icon className="shrink-0" /><div><h3 className="font-bold">{t}</h3><p className="text-sm text-ink-soft">{d}</p></div></div>
        ))}
      </section>
    </>
  )
}
