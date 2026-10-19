import { useEffect, useId } from 'react'
import { AlertCircle, ChevronLeft, ChevronRight, Loader2, Star, X, Check } from 'lucide-react'
import { statusLabel } from '../lib/format'

export function Spinner({ className = '' }) {
  return <Loader2 className={`animate-spin ${className}`} size={20} aria-label="Loading" />
}

export function PageLoader() {
  return <div className="flex justify-center py-24 text-ink-faint"><Spinner className="h-8 w-8" /></div>
}

export function ErrorBox({ message, onRetry }) {
  return (
    <div className="mx-auto my-10 max-w-md rounded-lg border border-brim/30 bg-brim-light p-5 text-center text-sm text-brim">
      <AlertCircle className="mx-auto mb-2" />
      <p>{message}</p>
      {onRetry && <button className="btn-outline btn-sm mt-3" onClick={onRetry}>Try again</button>}
    </div>
  )
}

export function EmptyState({ title, text, children }) {
  return (
    <div className="seam mx-auto my-10 max-w-lg p-10 text-center">
      <CapMark className="mx-auto mb-4 h-16 w-16" color="#6b7794" />
      <h2 className="text-xl font-bold">{title}</h2>
      {text && <p className="mt-1 text-sm text-ink-soft">{text}</p>}
      <div className="mt-5 flex justify-center gap-2">{children}</div>
    </div>
  )
}

const STATUS_STYLE = {
  PENDING_PAYMENT: 'bg-thread-light text-amber-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  PROCESSING: 'bg-blue-100 text-blue-800',
  PACKED: 'bg-indigo-100 text-indigo-800',
  SHIPPED: 'bg-indigo-100 text-indigo-800',
  OUT_FOR_DELIVERY: 'bg-indigo-100 text-indigo-800',
  DELIVERED: 'bg-moss-light text-moss',
  CANCELLED: 'bg-brim-light text-brim',
  RETURN_REQUESTED: 'bg-thread-light text-amber-800',
  RETURNED: 'bg-twill-dark text-ink-soft',
  REFUNDED: 'bg-twill-dark text-ink-soft',
  PAID: 'bg-moss-light text-moss',
  PENDING: 'bg-thread-light text-amber-800',
  FAILED: 'bg-brim-light text-brim',
  REFUND_PENDING: 'bg-thread-light text-amber-800',
  QUOTED: 'bg-blue-100 text-blue-800',
  ACCEPTED: 'bg-moss-light text-moss',
  REJECTED: 'bg-brim-light text-brim',
}
export function StatusBadge({ status }) {
  return <span className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-bold ${STATUS_STYLE[status] || 'bg-twill-dark text-ink-soft'}`}>{statusLabel(status)}</span>
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  return (
    <div className="mt-6 flex items-center justify-center gap-3 text-sm">
      <button className="btn-outline btn-sm" disabled={page <= 0} onClick={() => onChange(page - 1)}><ChevronLeft size={16} /> Prev</button>
      <span className="text-ink-soft">Page {page + 1} of {totalPages}</span>
      <button className="btn-outline btn-sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next <ChevronRight size={16} /></button>
    </div>
  )
}

export function Field({ label, hint, error, children }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-ink-faint">{hint}</span>}
      {error && <span className="mt-1 block text-xs text-brim">{error}</span>}
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
      <div className={`max-h-[92vh] w-full overflow-y-auto rounded-t-xl bg-white p-5 shadow-xl sm:rounded-xl ${wide ? 'sm:max-w-2xl' : 'sm:max-w-md'}`} onMouseDown={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-bold">{title}</h2>
          <button onClick={onClose} aria-label="Close" className="rounded p-1 hover:bg-twill"><X size={20} /></button>
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
        <Star key={i} size={size} className={i <= Math.round(value) ? 'fill-thread text-thread' : 'text-twill-dark'} />
      ))}
    </span>
  )
}

const PALETTE = ['#1b2a49', '#2f7d5b', '#c0392b', '#d99a0b', '#3d4c6f', '#7a4e2d']
export const colorFor = (s = '') => PALETTE[[...s].reduce((a, c) => a + c.charCodeAt(0), 0) % PALETTE.length]

/** A cap drawn from simple shapes. Doubles as the product-image placeholder. */
export function CapMark({ className = '', color = '#1b2a49' }) {
  const gradientId = useId().replace(/:/g, '')
  return (
    <svg viewBox="0 0 120 90" className={className} role="img" aria-label="Cap">
      <defs>
        <linearGradient id={`${gradientId}-crown`} x1="25" y1="14" x2="91" y2="73" gradientUnits="userSpaceOnUse">
          <stop stopColor={color} />
          <stop offset="1" stopColor={color} stopOpacity=".78" />
        </linearGradient>
        <linearGradient id={`${gradientId}-brim`} x1="28" y1="63" x2="99" y2="81" gradientUnits="userSpaceOnUse">
          <stop stopColor={color} stopOpacity=".9" />
          <stop offset="1" stopColor={color} />
        </linearGradient>
      </defs>
      <path d="M18 62C18 29 38 12 62 12s42 18 42 50H18Z" fill={`url(#${gradientId}-crown)`} />
      <path d="M62 13v49M40 19c-7 13-9 28-9 43m55-42c6 13 8 27 8 42" fill="none" stroke="#fff" strokeOpacity=".32" strokeWidth="1.5" />
      <path d="M13 62h93c12 0 14 10 2 14-22 8-62 8-88 0-11-3-15-14-7-14Z" fill={`url(#${gradientId}-brim)`} />
      <path d="M22 68c22 8 59 10 82 3" fill="none" stroke="#fff" strokeOpacity=".3" strokeWidth="1.5" strokeLinecap="round" />
      <circle cx="62" cy="11" r="4.5" fill="#d6b06b" stroke="#fff" strokeWidth="1.5" />
    </svg>
  )
}

const STEPS = ['CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED']
export function OrderProgress({ status }) {
  const idx = STEPS.indexOf(status)
  if (idx < 0) return null
  return (
    <ol className="flex items-start justify-between gap-1 overflow-x-auto py-2" aria-label="Order progress">
      {STEPS.map((s, i) => (
        <li key={s} className="flex min-w-[64px] flex-1 flex-col items-center text-center">
          <span className={`flex h-7 w-7 items-center justify-center rounded-full border-2 text-xs font-bold ${i <= idx ? 'border-ink bg-ink text-white' : 'border-dashed border-ink/30 text-ink-faint'}`}>
            {i < idx ? <Check size={14} /> : i + 1}
          </span>
          <span className={`mt-1 text-[11px] leading-tight ${i <= idx ? 'font-bold text-ink' : 'text-ink-faint'}`}>{statusLabel(s)}</span>
        </li>
      ))}
    </ol>
  )
}
