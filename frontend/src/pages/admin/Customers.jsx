import { useState } from 'react'
import { Plus } from 'lucide-react'
import api, { errMsg } from '../../api'
import { ErrorBox, Field, Modal, PageLoader, Pagination, StatusBadge } from '../../components/ui'
import { useToast } from '../../context/ToastContext'
import { useAsync } from '../../lib/hooks'
import { fmtDate } from '../../lib/format'
import { PageHeader, Table } from './common'

export default function Customers() {
  const toast = useToast()
  const [role, setRole] = useState('')
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useAsync(() => api.get('/admin/customers', { params: { role: role || undefined, page, size: 20 } }).then((r) => r.data), [role, page])
  const [adding, setAdding] = useState(null)

  const act = async (fn, msg) => { try { await fn(); toast(msg); reload() } catch (e) { toast(errMsg(e), 'error') } }
  const addAdmin = async (e) => {
    e.preventDefault()
    try { await api.post('/admin/admins', adding); toast('Admin created'); setAdding(null); reload() } catch (x) { toast(errMsg(x), 'error') }
  }
  const pendingFirst = data ? [...data.content].sort((a, b) => (a.role === 'B2B_USER' && !a.businessApproved ? -1 : 0) - (b.role === 'B2B_USER' && !b.businessApproved ? -1 : 0)) : []

  return (
    <>
      <PageHeader title="Customers">
        <select className="input !w-auto" value={role} onChange={(e) => { setPage(0); setRole(e.target.value) }} aria-label="Filter by type">
          <option value="">Everyone</option><option value="USER">Retail (B2C)</option><option value="B2B_USER">Business (B2B)</option><option value="ADMIN">Admins</option>
        </select>
        <button className="btn-primary btn-sm" onClick={() => setAdding({ name: '', email: '', phone: '', password: '' })}><Plus size={14} /> Add admin</button>
      </PageHeader>
      {loading ? <PageLoader /> : error ? <ErrorBox message={error} onRetry={reload} /> : <>
        <Table head={['Name', 'Contact', 'Type', 'Business', 'Joined', 'Status', '']} empty="No customers.">
          {pendingFirst.map((c) => (
            <tr key={c.id}>
              <td className="td font-bold">{c.name}</td>
              <td className="td">{c.email}<br /><span className="text-xs text-ink-faint">{c.phone}</span></td>
              <td className="td">{c.role === 'USER' ? 'Retail' : c.role === 'B2B_USER' ? 'Business' : 'Admin'}</td>
              <td className="td">{c.companyName ? <>{c.companyName}<br /><span className="text-xs text-ink-faint">{c.gstNumber}</span></> : '—'}</td>
              <td className="td whitespace-nowrap">{fmtDate(c.createdAt)}</td>
              <td className="td">{!c.enabled ? <StatusBadge status="REJECTED" /> : c.role === 'B2B_USER' ? (c.businessApproved ? <StatusBadge status="ACCEPTED" /> : <StatusBadge status="PENDING" />) : <span className="text-xs text-ink-faint">Active</span>}</td>
              <td className="td"><div className="flex gap-2">
                {c.role === 'B2B_USER' && !c.businessApproved && <button className="btn-thread btn-sm" onClick={() => act(() => api.put(`/admin/customers/${c.id}/approve-business`), 'Business approved')}>Approve</button>}
                {c.role !== 'ADMIN' && <button className="btn-ghost btn-sm" onClick={() => act(() => api.put(`/admin/customers/${c.id}/enabled`, null, { params: { value: !c.enabled } }), c.enabled ? 'Account disabled' : 'Account enabled')}>{c.enabled ? 'Disable' : 'Enable'}</button>}
              </div></td>
            </tr>
          ))}
        </Table>
        <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
      </>}
      {adding && (
        <Modal title="Add admin" onClose={() => setAdding(null)}>
          <form onSubmit={addAdmin} className="space-y-3">
            <Field label="Name"><input className="input" required value={adding.name} onChange={(e) => setAdding({ ...adding, name: e.target.value })} /></Field>
            <Field label="Email"><input className="input" type="email" required value={adding.email} onChange={(e) => setAdding({ ...adding, email: e.target.value })} /></Field>
            <Field label="Phone (10 digits)"><input className="input" maxLength={10} required value={adding.phone} onChange={(e) => setAdding({ ...adding, phone: e.target.value })} /></Field>
            <Field label="Password" hint="At least 8 characters"><input className="input" type="password" minLength={8} required value={adding.password} onChange={(e) => setAdding({ ...adding, password: e.target.value })} /></Field>
            <button className="btn-primary w-full">Create admin</button>
          </form>
        </Modal>
      )}
    </>
  )
}
