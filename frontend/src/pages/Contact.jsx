import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, BriefcaseBusiness, Clock3, Mail, PackageCheck, Phone, ShoppingBag } from 'lucide-react'
import api, { errMsg } from '../api'

const contactOptions = [
  {
    icon: PackageCheck,
    title: 'An existing order',
    text: 'Sign in to review your orders, check delivery progress or find your order details.',
    action: 'View my orders',
    to: '/orders',
  },
  {
    icon: BriefcaseBusiness,
    title: 'Bulk or custom caps',
    text: 'For teams, events and branded caps, create a business account to request custom pricing.',
    action: 'Business enquiries',
    to: '/register?type=business',
  },
  {
    icon: ShoppingBag,
    title: 'Choosing a cap',
    text: 'Explore the collection to compare styles, colours and available options.',
    action: 'Browse the shop',
    to: '/products',
  },
]

const faqs = [
  {
    question: 'How can I check the status of my order?',
    answer: 'Sign in and open My orders to see the latest order status and available shipment tracking details. Tracking updates appear once the order has shipped.',
    to: '/orders',
    link: 'View my orders',
  },
  {
    question: 'How much does shipping cost, and when will my order arrive?',
    answer: 'Any shipping charge and free-shipping threshold are shown in your cart and at checkout before you place the order. Once shipped, use the tracking details in My orders for delivery updates.',
    to: '/cart',
    link: 'View my cart',
  },
  {
    question: 'Can I cancel an order?',
    answer: 'Open the order in My orders. If its current status allows cancellation, you will see a Cancel order option there. Unpaid orders are automatically cancelled after 30 minutes.',
    to: '/orders',
    link: 'Manage my orders',
  },
  {
    question: 'Can I request a replacement or return?',
    answer: 'For an eligible delivered item, open its order details and submit a return or replacement request. Requests must be within the return window and are reviewed against the product’s return policy; approved requests proceed to pickup and inspection.',
    to: '/orders',
    link: 'Open an order',
  },
  {
    question: 'When will my refund reach me?',
    answer: 'Refunds are processed after the request is approved and the required checks are complete. For a refund-pending order, the status page advises allowing 5–7 working days for the payment to reach your account. COD refunds require UPI details.',
    to: '/orders',
    link: 'Check refund status',
  },
]

export default function Contact() {
  const [form, setForm] = useState({ name: '', phone: '', email: '', message: '' })
  const [sending, setSending] = useState(false)
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }))
    setSent(false)
    setError('')
  }

  const submit = async (event) => {
    event.preventDefault()
    setSending(true)
    setError('')
    try {
      await api.post('/contact', form)
      setSent(true)
      setForm({ name: '', phone: '', email: '', message: '' })
    } catch (e) {
      setError(errMsg(e))
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:py-16">
      <section className="mx-auto max-w-3xl text-center">
        <p className="mb-4 text-xs font-bold uppercase tracking-[0.18em] text-primary">Contact us</p>
        <h1 className="text-4xl font-bold leading-tight sm:text-6xl">How can we help?</h1>
        <p className="mx-auto mt-5 max-w-2xl text-base leading-7 text-ink-soft sm:text-lg">
          Choose the option that best matches your question. Order details and business quote requests are available directly through your CapStore account.
        </p>
      </section>

      <section className="mx-auto mt-14 grid max-w-6xl gap-6 lg:grid-cols-[minmax(0,1.5fr)_minmax(260px,0.8fr)]" aria-labelledby="contact-form-heading">
        <div className="card p-6 sm:p-8">
          <p className="mb-3 text-xs font-bold uppercase tracking-[0.18em] text-primary">Send us a message</p>
          <h2 id="contact-form-heading" className="text-2xl font-bold sm:text-3xl">Tell us what you need help with</h2>
          <p className="mt-3 leading-7 text-ink-soft">Share a few details and our team can get back to you.</p>
          <form className="mt-6 space-y-4" onSubmit={submit}>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="contact-name" className="label">Name</label>
                <input id="contact-name" className="input" autoComplete="name" maxLength={100} required value={form.name} onChange={update('name')} />
              </div>
              <div>
                <label htmlFor="contact-phone" className="label">Phone number</label>
                <input id="contact-phone" className="input" type="tel" autoComplete="tel" maxLength={24} required value={form.phone} onChange={update('phone')} />
              </div>
            </div>
            <div>
              <label htmlFor="contact-email" className="label">Email address</label>
              <input id="contact-email" className="input" type="email" autoComplete="email" maxLength={254} required value={form.email} onChange={update('email')} />
            </div>
            <div>
              <label htmlFor="contact-message" className="label">How can we help?</label>
              <textarea id="contact-message" className="input min-h-32 resize-y" minLength={10} maxLength={3000} required value={form.message} onChange={update('message')} />
              <p className="mt-1 text-xs text-ink-faint">Please include your order number if your question is about an order.</p>
            </div>
            {error && <p role="alert" className="rounded-sm border border-error/20 bg-error/5 p-3 text-sm font-medium text-error">{error}</p>}
            {sent && <p role="status" className="rounded-sm border border-moss/20 bg-moss-light p-3 text-sm font-medium text-moss">Thanks for contacting us. Your message has been sent.</p>}
            <button type="submit" className="btn-primary" disabled={sending}>{sending ? 'Sending…' : 'Send message'} <ArrowRight size={16} /></button>
          </form>
        </div>

        <aside className="rounded-md bg-primary-soft p-6 sm:p-8" aria-label="Contact details">
          <h2 className="text-2xl font-bold">Talk to our team</h2>
          <p className="mt-3 leading-7 text-ink-soft">Prefer to call? Our sample support details are below.</p>
          <div className="mt-7 space-y-6">
            <div className="flex gap-3">
              <Phone className="mt-1 shrink-0 text-primary" size={19} aria-hidden="true" />
              <div>
                <h3 className="text-sm font-bold">Phone</h3>
                <a className="mt-1 inline-block font-semibold text-primary hover:text-primary-strong" href="tel:+919000000000">+91 90000 00000</a>
                <p className="mt-1 text-xs font-medium text-ink-soft">Demo contact number — replace before publishing.</p>
              </div>
            </div>
            <div className="flex gap-3">
              <Clock3 className="mt-1 shrink-0 text-primary" size={19} aria-hidden="true" />
              <div>
                <h3 className="text-sm font-bold">Support hours</h3>
                <p className="mt-1 font-medium text-ink-soft">Monday–Saturday, 10:00 AM–6:00 PM IST</p>
                <p className="mt-1 text-xs font-medium text-ink-soft">Demo timings — replace before publishing.</p>
              </div>
            </div>
            <div className="flex gap-3">
              <Mail className="mt-1 shrink-0 text-primary" size={19} aria-hidden="true" />
              <div>
                <h3 className="text-sm font-bold">Online enquiry</h3>
                <p className="mt-1 leading-6 text-ink-soft">Send the form and we’ll reply to the email address you provide.</p>
              </div>
            </div>
          </div>
        </aside>
      </section>

      <section className="mx-auto mt-12 grid max-w-6xl gap-5 md:grid-cols-3" aria-label="Contact options">
        {contactOptions.map(({ icon: Icon, title, text, action, to }) => (
          <article key={title} className="card flex flex-col p-6 sm:p-8">
            <span className="mb-5 inline-flex h-11 w-11 items-center justify-center rounded-md bg-primary-soft text-primary">
              <Icon size={22} aria-hidden="true" />
            </span>
            <h2 className="text-xl font-bold">{title}</h2>
            <p className="mt-3 flex-1 leading-7 text-ink-soft">{text}</p>
            <Link to={to} className="mt-6 inline-flex items-center gap-2 text-sm font-bold text-primary hover:text-primary-strong">
              {action} <ArrowRight size={16} aria-hidden="true" />
            </Link>
          </article>
        ))}
      </section>

      <section className="mx-auto mt-16 max-w-4xl" aria-labelledby="contact-faq-heading">
        <div className="mb-6 text-center">
          <p className="mb-3 text-xs font-bold uppercase tracking-[0.18em] text-primary">Need a quick answer?</p>
          <h2 id="contact-faq-heading" className="text-3xl font-bold sm:text-4xl">Frequently asked questions</h2>
        </div>
        <div className="space-y-3">
          {faqs.map(({ question, answer, to, link }) => (
            <details key={question} className="card group p-5 sm:p-6">
              <summary className="cursor-pointer list-none pr-8 text-base font-bold text-ink marker:hidden focus-visible:outline-primary sm:text-lg">
                <span>{question}</span>
                <span className="float-right -mr-8 text-primary transition-transform group-open:rotate-45" aria-hidden="true">+</span>
              </summary>
              <p className="mt-4 leading-7 text-ink-soft">{answer}</p>
              <Link to={to} className="mt-3 inline-flex items-center gap-2 text-sm font-bold text-primary hover:text-primary-strong">
                {link} <ArrowRight size={15} aria-hidden="true" />
              </Link>
            </details>
          ))}
        </div>
      </section>
    </div>
  )
}
