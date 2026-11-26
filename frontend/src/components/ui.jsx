import { useEffect, useId } from 'react'
import { AlertCircle, Check, ChevronLeft, ChevronRight, Loader2, Star, X } from 'lucide-react'
import { statusLabel } from '../lib/format'

export function Spinner({ className = '' }) {
  return <Loader2 className={`animate-spin ${className}`} size={20} aria-label="Loading" />
}

export function PageLoader() {
  return <div className="flex justify-center py-24 text-ink-faint" role="status"><Spinner className="h-7 w-7" /></div>
}

/** Placeholder grid shown while products load, so the layout doesn't jump. */
export function ProductGridSkeleton({ count = 8, className = 'grid-cols-2 md:grid-cols-3 lg:grid-cols-4' }) {
  return (
    <div className={`grid gap-x-4 gap-y-8 sm:gap-x-6 ${className}`} aria-busy="true" aria-label="Loading products">
      {Array.from({ length: count }, (_, i) => (
        <div key={i}>
          <div className="skeleton aspect-[4/5] rounded-md" />
          <div className="skeleton mt-3 h-3 w-1/3" />
          <div className="skeleton mt-2 h-4 w-3/4" />
          <div className="skeleton mt-2 h-4 w-1/4" />
        </div>
      ))}
    </div>
  )
}

export function ErrorBox({ message, onRetry }) {
  return (
    <div role="alert" className="mx-auto my-10 max-w-md rounded-md border border-brim/25 bg-brim-light p-6 text-center text-sm text-brim">
      <AlertCircle className="mx-auto mb-2" />
      <p className="font-medium">{message}</p>
      {onRetry && <button className="btn-outline btn-sm mt-4" onClick={onRetry}>Try again</button>}
    </div>
  )
}

export function EmptyState({ title, text, children }) {
  return (
    <div className="mx-auto my-12 max-w-lg px-4 text-center">
      <div className="mx-auto mb-5 flex h-24 w-24 items-center justify-center rounded-full bg-twill">
        <CapMark className="h-12 w-14" color="#9ca3af" />
      </div>
      <h2 className="font-display text-2xl font-semibold uppercase">{title}</h2>
      {text && <p className="mt-2 text-ink-soft">{text}</p>}
      {children && <div className="mt-6 flex flex-wrap justify-center gap-3">{children}</div>}
    </div>
  )
}

// Tones keep status colours meaningful while staying on-palette: blue = in progress, green = done, red = problem, amber = waiting.
const TONE = {
  info: 'bg-primary-soft text-primary',
  ok: 'bg-moss-light text-moss',
  bad: 'bg-brim-light text-brim',
  wait: 'bg-warn-light text-warn',
  muted: 'bg-twill text-ink-soft',
}
const STATUS_TONE = {
  PENDING_PAYMENT: 'wait', PENDING: 'wait', RETURN_REQUESTED: 'wait', REFUND_PENDING: 'wait', REQUESTED: 'wait',
  CONFIRMED: 'info', PROCESSING: 'info', PACKED: 'info', SHIPPED: 'info', OUT_FOR_DELIVERY: 'info',
  INITIATED: 'info', CREATED: 'info', IN_TRANSIT: 'info',
  APPROVED: 'info', PICKUP_SCHEDULED: 'info', PICKED_UP: 'info', RECEIVED: 'info', REFUND_INITIATED: 'info',
  DELIVERED: 'ok', PAID: 'ok', ACCEPTED: 'ok', QC_PASSED: 'ok', REFUNDED: 'muted', RETURNED: 'muted', CLOSED: 'muted',
  REPLACEMENT_CREATED: 'ok',
  CANCELLED: 'bad', FAILED: 'bad', REJECTED: 'bad', DECLINED: 'bad', QC_FAILED: 'bad',
}
export function StatusBadge({ status }) {
  return (
    <span className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-medium leading-none ${TONE[STATUS_TONE[status]] || TONE.muted}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden="true" />
      {statusLabel(status)}
    </span>
  )
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  return (
    <nav className="mt-10 flex items-center justify-center gap-3 text-sm" aria-label="Pagination">
      <button className="btn-outline btn-sm" disabled={page <= 0} onClick={() => onChange(page - 1)}><ChevronLeft size={16} /> Prev</button>
      <span className="min-w-[6rem] text-center text-ink-soft">Page {page + 1} of {totalPages}</span>
      <button className="btn-outline btn-sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next <ChevronRight size={16} /></button>
    </nav>
  )
}

export function Field({ label, hint, error, children }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      {children}
      {hint && !error && <span className="mt-1.5 block text-xs text-ink-faint">{hint}</span>}
      {error && <span className="mt-1.5 block text-xs text-brim">{error}</span>}
    </label>
  )
}

export function Modal({ title, onClose, children, wide }) {
  useEffect(() => {
    const h = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', h)
    document.body.style.overflow = 'hidden'
    return () => { document.removeEventListener('keydown', h); document.body.style.overflow = '' }
  }, [onClose])
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink/50 p-0 sm:items-center sm:p-4" onMouseDown={onClose} role="dialog" aria-modal="true" aria-label={title}>
      <div className={`max-h-[92vh] w-full animate-fade-up overflow-y-auto rounded-t-lg bg-white p-6 shadow-xl sm:rounded-md ${wide ? 'sm:max-w-2xl' : 'sm:max-w-md'}`} onMouseDown={(e) => e.stopPropagation()}>
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-xl font-semibold">{title}</h2>
          <button onClick={onClose} aria-label="Close" className="rounded-sm p-1 hover:bg-twill"><X size={20} /></button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function Stars({ value = 0, size = 16 }) {
  return (
    <span className="inline-flex" aria-label={`${value} out of 5`}>
      {[1, 2, 3, 4, 5].map((i) => (
        <Star key={i} size={size} className={i <= Math.round(value) ? 'fill-primary text-primary' : 'text-tertiary'} />
      ))}
    </span>
  )
}

// Placeholder tints for products without photos — muted, editorial, and readable against white.
const PALETTE = ['#282828', '#1f38d3', '#4b5563', '#1f7a4d', '#7c3aed', '#9a3412']
export const colorFor = (s = '') => PALETTE[[...s].reduce((a, c) => a + c.charCodeAt(0), 0) % PALETTE.length]

/** A cap drawn from simple shapes. Doubles as the product-image placeholder. */
export function CapMark({ className = '', color = '#1f38d3' }) {
  const gradientId = useId().replace(/:/g, '')
  return (
    <svg viewBox="0 0 120 90" className={className} role="img" aria-label="Cap">
      <defs>
        <linearGradient id={`${gradientId}-crown`} x1="25" y1="14" x2="91" y2="73" gradientUnits="userSpaceOnUse">
          <stop stopColor={color} />
          <stop offset="1" stopColor={color} stopOpacity=".82" />
        </linearGradient>
      </defs>
      <path d="M18 62C18 29 38 12 62 12s42 18 42 50H18Z" fill={`url(#${gradientId}-crown)`} />
      <path d="M62 13v49M40 19c-7 13-9 28-9 43m55-42c6 13 8 27 8 42" fill="none" stroke="#fff" strokeOpacity=".3" strokeWidth="1.5" />
      <path d="M13 62h93c12 0 14 10 2 14-22 8-62 8-88 0-11-3-15-14-7-14Z" fill={color} />
      <path d="M22 68c22 8 59 10 82 3" fill="none" stroke="#fff" strokeOpacity=".28" strokeWidth="1.5" strokeLinecap="round" />
      <circle cx="62" cy="11" r="4.5" fill={color} stroke="#fff" strokeWidth="1.5" />
    </svg>
  )
}

const STEPS = ['CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED']
export function OrderProgress({ status }) {
  const idx = STEPS.indexOf(status)
  if (idx < 0) return null
  const pct = (idx / (STEPS.length - 1)) * 100
  return (
    <div className="relative overflow-x-auto py-2">
      <div className="relative min-w-[480px]">
        <div className="absolute left-[8.33%] right-[8.33%] top-[14px] h-0.5 bg-tertiary" aria-hidden="true">
          <div className="h-full bg-primary transition-all duration-500" style={{ width: `${pct}%` }} />
        </div>
        <ol className="relative grid grid-cols-6" aria-label="Order progress">
          {STEPS.map((s, i) => (
            <li key={s} className="flex flex-col items-center text-center" aria-current={i === idx ? 'step' : undefined}>
              <span className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold ring-4 ring-white ${i <= idx ? 'bg-primary text-white' : 'border border-tertiary bg-white text-ink-faint'}`}>
                {i < idx ? <Check size={14} strokeWidth={3} /> : i + 1}
              </span>
              <span className={`mt-2 text-[11px] leading-tight ${i <= idx ? 'font-semibold text-ink' : 'text-ink-faint'}`}>{statusLabel(s)}</span>
            </li>
          ))}
        </ol>
      </div>
    </div>
  )
}
