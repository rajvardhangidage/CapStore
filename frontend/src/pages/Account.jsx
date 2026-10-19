import { useState } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import api, { errMsg } from '../api'
import { ErrorBox, Field, Modal, PageLoader } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useDialog, useToast } from '../context/ToastContext'
import { useAsync } from '../lib/hooks'
import { AddressFields, emptyAddress } from './Checkout'

export default function Account() {
  const { user, updateProfile } = useAuth()
  const toast = useToast()
  const { confirm } = useDialog()
  const { data, loading, error, reload } = useAsync(() => api.get('/me/addresses').then((r) => r.data))
  const [edit, setEdit] = useState(null)
  const [profile, setProfile] = useState(null)
  const [profileBusy, setProfileBusy] = useState(false)

  const saveProfile = async (e) => {
    e.preventDefault()
    setProfileBusy(true)
    try {
      await updateProfile(profile)
      setProfile(null)
      toast('Account details updated')
    } catch (x) {
      toast(errMsg(x), 'error')
    } finally {
      setProfileBusy(false)
    }
  }
  const save = async (e) => {
    e.preventDefault()
    try {
      if (edit.id) await api.put(`/me/addresses/${edit.id}`, edit)
      else await api.post('/me/addresses', edit)
      setEdit(null); reload(); toast('Address saved')
    } catch (x) { toast(errMsg(x), 'error') }
  }
  const del = async (a) => {
    if (!(await confirm('Delete this address?'))) return
    try { await api.delete(`/me/addresses/${a.id}`); reload() } catch (x) { toast(errMsg(x), 'error') }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-3xl font-extrabold">My account</h1>
      <div className="card p-5 text-sm">
        <div className="flex items-start justify-between gap-3">
          <div><p><b>{user.name}</b></p><p>{user.email} · {user.phone}</p>
            {user.role === 'B2B_USER' && <p className="mt-2">{user.companyName} · {user.businessApproved ? <span className="font-bold text-moss">Business account approved</span> : <span className="font-bold text-amber-700">Awaiting approval. Bulk pricing starts once approved.</span>}</p>}
          </div>
          <button className="btn-outline btn-sm shrink-0" onClick={() => setProfile({ name: user.name, email: user.email, phone: user.phone })}><Pencil size={13} /> Edit details</button>
        </div>
      </div>
      <div className="mb-3 mt-8 flex items-center justify-between"><h2 className="text-xl font-extrabold">Saved addresses</h2><button className="btn-primary btn-sm" onClick={() => setEdit({ ...emptyAddress, name: user.name, phone: user.phone })}><Plus size={14} /> Add address</button></div>
      {loading ? <PageLoader /> : error ? <ErrorBox message={error} onRetry={reload} /> : data.length === 0 ? <p className="text-sm text-ink-soft">No saved addresses yet.</p> : (
        <ul className="grid gap-3 sm:grid-cols-2">
          {data.map((a) => (
            <li key={a.id} className="card p-4 text-sm">
              <div className="flex items-start justify-between"><b>{a.name}{a.label && <span className="font-normal text-ink-faint"> ({a.label})</span>}</b>{a.defaultAddress && <span className="rounded bg-thread-light px-2 text-xs font-bold">Default</span>}</div>
              <p className="mt-1 text-ink-soft">{a.line1}{a.line2 ? `, ${a.line2}` : ''}, {a.city}, {a.state} {a.pincode}<br />{a.phone}</p>
              <div className="mt-3 flex gap-2">
                <button className="btn-outline btn-sm" onClick={() => setEdit(a)}><Pencil size={13} /> Edit</button>
                {!a.defaultAddress && <button className="btn-ghost btn-sm" onClick={() => api.put(`/me/addresses/${a.id}`, { ...a, defaultAddress: true }).then(reload)}>Make default</button>}
                <button className="btn-ghost btn-sm text-brim" onClick={() => del(a)} aria-label="Delete address"><Trash2 size={14} /></button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {edit && (
        <Modal title={edit.id ? 'Edit address' : 'New address'} onClose={() => setEdit(null)} wide>
          <form onSubmit={save} className="space-y-3">
            <AddressFields value={edit} onChange={setEdit} />
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={!!edit.defaultAddress} onChange={(e) => setEdit({ ...edit, defaultAddress: e.target.checked })} /> Make this my default address</label>
            <button className="btn-primary w-full">Save address</button>
          </form>
        </Modal>
      )}
      {profile && (
        <Modal title="Edit account details" onClose={() => setProfile(null)}>
          <form onSubmit={saveProfile} className="space-y-3">
            <Field label="Full name"><input className="input" required maxLength={120} value={profile.name} onChange={(e) => setProfile({ ...profile, name: e.target.value })} /></Field>
            <Field label="Email"><input className="input" type="email" required maxLength={190} value={profile.email} onChange={(e) => setProfile({ ...profile, email: e.target.value })} /></Field>
            <Field label="Phone (10 digits)"><input className="input" type="tel" inputMode="numeric" pattern="[0-9]{10}" maxLength={10} required value={profile.phone} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} /></Field>
            <button className="btn-primary w-full" disabled={profileBusy}>{profileBusy ? 'Saving…' : 'Save details'}</button>
          </form>
        </Modal>
      )}
    </div>
  )
}
