import { Link } from 'react-router-dom'
import { ArrowRight, Check, Factory, Sparkles } from 'lucide-react'

const values = [
  {
    icon: Factory,
    title: 'Made in our workshop',
    text: 'We make our caps in-house, keeping a close eye on materials, construction and the details that make a cap comfortable to wear.',
  },
  {
    icon: Check,
    title: 'Made for everyday',
    text: 'From classic cotton caps to snapbacks and bucket hats, our collection is designed to be easy to wear and easy to make your own.',
  },
  {
    icon: Sparkles,
    title: 'Made for your team, too',
    text: 'Need a larger run or custom branding? We help businesses and groups bring their ideas together with bulk pricing and custom quotes.',
  },
]

export default function About() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:py-16">
      <section className="mx-auto max-w-4xl text-center">
        <p className="mb-4 text-xs font-bold uppercase tracking-[0.18em] text-primary">About us</p>
        <h1 className="text-4xl font-bold leading-tight sm:text-6xl">Good caps start with care.</h1>
        <p className="mx-auto mt-6 max-w-2xl text-base leading-7 text-ink-soft sm:text-lg">
          We are a cap maker focused on practical design, dependable quality and personal style. Every piece starts in our workshop, where we make headwear for everyday wear and for teams with something to say.
        </p>
        <Link to="/products" className="btn-primary mt-8">Explore the collection <ArrowRight size={16} /></Link>
      </section>

      <section className="mt-16 grid gap-5 md:mt-20 md:grid-cols-3">
        {values.map(({ icon: Icon, title, text }) => (
          <article key={title} className="card p-6 sm:p-8">
            <span className="mb-5 inline-flex h-11 w-11 items-center justify-center rounded-md bg-primary-soft text-primary">
              <Icon size={22} aria-hidden="true" />
            </span>
            <h2 className="text-xl font-bold">{title}</h2>
            <p className="mt-3 leading-7 text-ink-soft">{text}</p>
          </article>
        ))}
      </section>

      <section className="mt-12 flex flex-col items-start justify-between gap-5 rounded-md bg-primary-soft p-6 sm:flex-row sm:items-center sm:p-8">
        <div>
          <h2 className="text-2xl font-bold">A cap for yourself or your whole crew?</h2>
          <p className="mt-2 max-w-2xl leading-7 text-ink-soft">Shop the collection for individual pieces, or get in touch through a business account for bulk pricing and custom branding.</p>
        </div>
        <Link to="/register?type=business" className="btn-primary shrink-0">Business orders <ArrowRight size={16} /></Link>
      </section>
    </div>
  )
}
