const fmt = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })
export const inr = (n) => fmt.format(Number(n || 0))

export const fmtDate = (d) =>
  d ? new Date(d.endsWith?.('Z') ? d : d + 'Z').toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''

export const statusLabel = (s) => (s || '').replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase())

/** Mirrors the backend maths so the cart can show an estimate before the order is placed. */
export function estimate(subtotal, discount, cfg) {
  const taxable = Math.max(subtotal - discount, 0)
  const tax = Math.round(taxable * (cfg?.gstRatePercent ?? 5)) / 100
  const shipping = subtotal === 0 ? 0 : taxable >= (cfg?.freeShippingAbove ?? 999) ? 0 : (cfg?.shippingFlatRate ?? 70)
  return { tax, shipping, total: taxable + tax + shipping }
}

export const GST_RE = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/
