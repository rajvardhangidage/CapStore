import { useState } from 'react'
import api, { errMsg } from '../../api'
import { ErrorBox, Field, PageLoader } from '../../components/ui'
import { useToast } from '../../context/ToastContext'
import { updateStoreConfig, useAsync } from '../../lib/hooks'
import { inr } from '../../lib/format'
import { PageHeader } from './common'

export default function StoreSettings() {
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(() => api.get('/admin/store-settings').then((r) => r.data))
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState(null)

  const settings = form || data
  const change = (field, value) => setForm((current) => ({ ...(current || data), [field]: value }))

  const save = async (event) => {
    event.preventDefault()
    setSaving(true)
    try {
      const { data: updated } = await api.put('/admin/store-settings', {
        gstRatePercent: Number(settings.gstRatePercent),
        shippingFlatRate: Number(settings.shippingFlatRate),
        freeShippingAbove: Number(settings.freeShippingAbove),
      })
      updateStoreConfig(updated)
      setForm(null)
      reload()
      toast('Shipping and GST settings saved')
    } catch (err) {
      toast(errMsg(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />

  return (
    <>
      <PageHeader title="Shipping & tax" sub="These settings apply to new orders. Existing order totals and invoice tax rates remain unchanged." />
      <form onSubmit={save} className="card max-w-2xl space-y-5 p-5">
        <Field label="GST rate (%)">
          <input className="input" type="number" min="0" max="100" step="0.01" required
            value={settings.gstRatePercent} onChange={(e) => change('gstRatePercent', e.target.value)} />
          <p className="mt-1 text-xs text-ink-faint">Applied to the order subtotal after discounts.</p>
        </Field>
        <Field label="Flat shipping charge (₹)">
          <input className="input" type="number" min="0" step="0.01" required
            value={settings.shippingFlatRate} onChange={(e) => change('shippingFlatRate', e.target.value)} />
        </Field>
        <Field label="Free shipping on orders of (₹) or more">
          <input className="input" type="number" min="0" step="0.01" required
            value={settings.freeShippingAbove} onChange={(e) => change('freeShippingAbove', e.target.value)} />
          <p className="mt-1 text-xs text-ink-faint">Current flat charge: {inr(settings.shippingFlatRate)}. Set the threshold to 0 to make shipping free on every order.</p>
        </Field>
        <button className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save settings'}</button>
      </form>
    </>
  )
}
