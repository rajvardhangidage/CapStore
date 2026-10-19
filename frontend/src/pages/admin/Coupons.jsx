import { useState } from 'react'
import { Plus } from 'lucide-react'
import api, { errMsg } from '../../api'
import { ErrorBox, Field, Modal, PageLoader } from '../../components/ui'
import { useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { fmtDate, inr } from '../../lib/format'
import { PageHeader, Table } from './common'

const blank = { code: '', description: '', discountType: 'PERCENT', discountValue: '', minOrder: '', maxDiscount: '', expiresAt: '', usageLimit: '', active: true }

export default function Coupons() {
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(() => api.get('/admin/coupons').then((r) => r.data))
  const [edit, setEdit] = useState(null)

  const body = (c) => ({
    code: c.code, description: c.description || null, discountType: c.discountType, discountValue: Number(c.discountValue),
    minOrder: c.minOrder === '' || c.minOrder == null ? null : Number(c.minOrder), maxDiscount: c.maxDiscount === '' || c.maxDiscount == null ? null : Number(c.maxDiscount),
    expiresAt: c.expiresAt ? c.expiresAt.slice(0, 16) : null, usageLimit: c.usageLimit === '' || c.usageLimit == null ? null : Number(c.usageLimit), active: c.active,
  })
  const save = async (e) => {
    e.preventDefault()
    try {
      if (edit.id) await api.put(`/admin/coupons/${edit.id}`, body(edit)); else await api.post('/admin/coupons', body(edit))
      toast('Coupon saved'); setEdit(null); reload()
    } catch (x) { toast(errMsg(x), 'error') }
  }
  const s = (k, extra = {}) => ({ className: 'input', value: edit[k] ?? '', onChange: (e) => setEdit({ ...edit, [k]: e.target.value }), ...extra })

  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  return (
    <>
      <PageHeader title="Coupons"><button className="btn-primary btn-sm" onClick={() => setEdit({ ...blank })}><Plus size={14} /> New coupon</button></PageHeader>
      <Table head={['Code', 'Discount', 'Min order', 'Used', 'Expires', 'Status', '']} empty="No coupons yet.">
        {data.map((c) => (
          <tr key={c.id}>
            <td className="td font-bold">{c.code}<br /><span className="text-xs font-normal text-ink-faint">{c.description}</span></td>
            <td className="td">{c.discountType === 'PERCENT' ? `${c.discountValue}%` : inr(c.discountValue)}{c.maxDiscount ? ` (max ${inr(c.maxDiscount)})` : ''}</td>
            <td className="td">{inr(c.minOrder)}</td>
            <td className="td">{c.usedCount}{c.usageLimit ? ` / ${c.usageLimit}` : ''}</td>
            <td className="td">{c.expiresAt ? fmtDate(c.expiresAt) : 'Never'}</td>
            <td className="td"><span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${c.active ? 'bg-moss-light text-moss' : 'bg-twill-dark text-ink-soft'}`}>{c.active ? 'Active' : 'Off'}</span></td>
            <td className="td"><button className="btn-outline btn-sm" onClick={() => setEdit({ ...c, expiresAt: c.expiresAt ? c.expiresAt.slice(0, 16) : '' })}>Edit</button></td>
          </tr>
        ))}
      </Table>
      {edit && (
        <Modal title={edit.id ? 'Edit coupon' : 'New coupon'} onClose={() => setEdit(null)}>
          <form onSubmit={save} className="space-y-3">
            <Field label="Code"><input {...s('code', { required: true, disabled: !!edit.id, style: { textTransform: 'uppercase' } })} /></Field>
            <Field label="Description (shown to customers)"><input {...s('description')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Type"><select {...s('discountType')}><option value="PERCENT">Percent (%)</option><option value="FLAT">Flat (₹)</option></select></Field>
              <Field label="Value"><input {...s('discountValue', { type: 'number', min: 0.01, step: 0.01, required: true })} /></Field>
              <Field label="Minimum order (₹)"><input {...s('minOrder', { type: 'number', min: 0 })} /></Field>
              <Field label="Max discount (₹)"><input {...s('maxDiscount', { type: 'number', min: 0 })} /></Field>
              <Field label="Expires"><input {...s('expiresAt', { type: 'datetime-local' })} /></Field>
              <Field label="Usage limit"><input {...s('usageLimit', { type: 'number', min: 1 })} /></Field>
            </div>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={edit.active} onChange={(e) => setEdit({ ...edit, active: e.target.checked })} /> Active</label>
            <button className="btn-primary w-full">Save coupon</button>
          </form>
        </Modal>
      )}
    </>
  )
}
