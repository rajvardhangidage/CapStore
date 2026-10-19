import { useState } from 'react'
import { Plus } from 'lucide-react'
import api, { errMsg } from '../../api'
import { ErrorBox, Field, Modal, PageLoader } from '../../components/ui'
import { useDialog, useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { PageHeader, Table } from './common'

const blank = { title: '', subtitle: '', imageUrl: '', linkUrl: '/products', sortOrder: 1, active: true }

export default function Banners() {
  const toast = useToast()
  const { confirm } = useDialog()
  const { data, loading, error, reload } = useAsync(() => api.get('/admin/banners').then((r) => r.data))
  const [edit, setEdit] = useState(null)

  const save = async (e) => {
    e.preventDefault()
    const b = { ...edit, sortOrder: Number(edit.sortOrder) || 0, imageUrl: edit.imageUrl || null, subtitle: edit.subtitle || null, linkUrl: edit.linkUrl || null }
    try { if (edit.id) await api.put(`/admin/banners/${edit.id}`, b); else await api.post('/admin/banners', b); toast('Banner saved'); setEdit(null); reload() } catch (x) { toast(errMsg(x), 'error') }
  }
  const del = async (b) => { if (await confirm(`Delete "${b.title}"?`)) { try { await api.delete(`/admin/banners/${b.id}`); reload() } catch (x) { toast(errMsg(x), 'error') } } }
  const upload = async (file) => {
    if (!file) return
    const fd = new FormData(); fd.append('file', file)
    try { const { data: d } = await api.post('/uploads', fd); setEdit((x) => ({ ...x, imageUrl: d.url })) } catch (x) { toast(errMsg(x), 'error') }
  }

  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  return (
    <>
      <PageHeader title="Home page banners" sub="The first active banner is the hero. The rest show as cards below it."><button className="btn-primary btn-sm" onClick={() => setEdit({ ...blank })}><Plus size={14} /> New banner</button></PageHeader>
      <Table head={['Order', 'Banner', 'Link', 'Status', '']} empty="No banners yet.">
        {data.map((b) => (
          <tr key={b.id}>
            <td className="td">{b.sortOrder}</td>
            <td className="td"><b>{b.title}</b><br /><span className="text-xs text-ink-faint">{b.subtitle}</span></td>
            <td className="td">{b.linkUrl || '—'}</td>
            <td className="td"><span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${b.active ? 'bg-moss-light text-moss' : 'bg-twill-dark text-ink-soft'}`}>{b.active ? 'Live' : 'Hidden'}</span></td>
            <td className="td"><div className="flex gap-2"><button className="btn-outline btn-sm" onClick={() => setEdit({ ...b, imageUrl: b.imageUrl || '', subtitle: b.subtitle || '', linkUrl: b.linkUrl || '' })}>Edit</button><button className="btn-ghost btn-sm text-brim" onClick={() => del(b)}>Delete</button></div></td>
          </tr>
        ))}
      </Table>
      {edit && (
        <Modal title={edit.id ? 'Edit banner' : 'New banner'} onClose={() => setEdit(null)}>
          <form onSubmit={save} className="space-y-3">
            <Field label="Title"><input className="input" required value={edit.title} onChange={(e) => setEdit({ ...edit, title: e.target.value })} /></Field>
            <Field label="Subtitle"><input className="input" value={edit.subtitle} onChange={(e) => setEdit({ ...edit, subtitle: e.target.value })} /></Field>
            <Field label="Link (e.g. /products?categoryId=1)"><input className="input" value={edit.linkUrl} onChange={(e) => setEdit({ ...edit, linkUrl: e.target.value })} /></Field>
            <Field label="Image (optional)"><input type="file" accept="image/png,image/jpeg,image/webp" className="text-sm" onChange={(e) => upload(e.target.files[0])} /></Field>
            {edit.imageUrl && <img src={edit.imageUrl} alt="Preview" className="h-24 rounded border border-twill-dark" />}
            <Field label="Sort order"><input className="input" type="number" value={edit.sortOrder} onChange={(e) => setEdit({ ...edit, sortOrder: e.target.value })} /></Field>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={edit.active} onChange={(e) => setEdit({ ...edit, active: e.target.checked })} /> Show on home page</label>
            <button className="btn-primary w-full">Save banner</button>
          </form>
        </Modal>
      )}
    </>
  )
}
