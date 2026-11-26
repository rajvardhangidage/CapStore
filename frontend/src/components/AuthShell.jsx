import { FileText, RotateCcw, Truck } from 'lucide-react'
import { CapMark } from './ui'

/** Two-column auth layout: brand panel on desktop, a clean single column on mobile. */
export default function AuthShell({ title, subtitle, children }) {
  return (
    <div className="page grid items-center gap-10 py-10 lg:grid-cols-2 lg:py-14">
      <aside className="relative hidden min-h-[600px] overflow-hidden rounded-md bg-primary p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <span className="pointer-events-none absolute -bottom-8 -right-6 select-none font-display text-[12rem] font-semibold uppercase leading-none text-white/10" aria-hidden="true">Caps</span>
        <CapMark className="h-12 w-14" color="#ffffff" />
        <div className="relative">
          <h2 className="font-display text-5xl font-semibold uppercase leading-[0.95]">Made in our<br />own workshop.</h2>
          <ul className="mt-8 space-y-3 text-white/85">
            <li className="flex items-center gap-3"><Truck size={18} /> Track every order to your door</li>
            <li className="flex items-center gap-3"><RotateCcw size={18} /> Easy returns on eligible items</li>
            <li className="flex items-center gap-3"><FileText size={18} /> GST invoice with every order</li>
          </ul>
        </div>
      </aside>
      <div className="flex items-center justify-center">
        <div className="w-full max-w-md animate-fade-up">
          <h1 className="page-title">{title}</h1>
          {subtitle && <p className="mt-3 text-ink-soft">{subtitle}</p>}
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  )
}
