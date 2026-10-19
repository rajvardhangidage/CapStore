import { useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { errMsg } from '../api'
import { Field } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { GST_RE } from '../lib/format'

export default function Register() {
  const { register } = useAuth()
  const [sp] = useSearchParams()
  const nav = useNavigate()
  const loc = useLocation()
  const toast = useToast()
  const [business, setBusiness] = useState(sp.get('type') === 'business')
  const [f, setF] = useState({ name: '', email: '', phone: '', password: '', companyName: '', gstNumber: '' })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: k === 'gstNumber' ? e.target.value.toUpperCase() : e.target.value })
  const gstWarn = business && f.gstNumber && !GST_RE.test(f.gstNumber)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try {
      await register({ ...f, businessAccount: business })
      if (business) toast('Business account created. Bulk pricing starts once we approve it.')
      nav(loc.state?.from || '/', { replace: true })
    } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }
  return (
    <div className="mx-auto max-w-md px-4 py-12">
      <form onSubmit={submit} className="card space-y-4 p-6">
        <h1 className="text-2xl font-extrabold">Create your account</h1>
        <div className="grid grid-cols-2 gap-1 rounded-md bg-twill p-1 text-sm font-bold" role="tablist">
          <button type="button" role="tab" aria-selected={!business} onClick={() => setBusiness(false)} className={`rounded py-2 ${!business ? 'bg-white shadow-sm' : 'text-ink-soft'}`}>Personal</button>
          <button type="button" role="tab" aria-selected={business} onClick={() => setBusiness(true)} className={`rounded py-2 ${business ? 'bg-white shadow-sm' : 'text-ink-soft'}`}>Business</button>
        </div>
        {business && <p className="rounded bg-thread-light p-3 text-xs">Business accounts get bulk pricing, GST invoices and custom quotes after we verify your GST number.</p>}
        {err && <p role="alert" className="rounded bg-brim-light p-3 text-sm text-brim">{err}</p>}
        <Field label="Full name"><input className="input" required autoComplete="name" value={f.name} onChange={set('name')} /></Field>
        <Field label="Email"><input className="input" type="email" required autoComplete="email" value={f.email} onChange={set('email')} /></Field>
        <Field label="Phone (10 digits)"><input className="input" inputMode="numeric" maxLength={10} required autoComplete="tel" value={f.phone} onChange={set('phone')} /></Field>
        {business && <>
          <Field label="Company name"><input className="input" required value={f.companyName} onChange={set('companyName')} /></Field>
          <Field label="GST number" error={gstWarn ? 'This does not look like a valid GSTIN. Double-check it.' : ''}><input className="input" required maxLength={15} value={f.gstNumber} onChange={set('gstNumber')} /></Field>
        </>}
        <Field label="Password" hint="At least 8 characters"><input className="input" type="password" required minLength={8} autoComplete="new-password" value={f.password} onChange={set('password')} /></Field>
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
        <p className="text-center text-sm">Already have an account? <Link to="/login" state={loc.state} className="font-bold underline">Log in</Link></p>
      </form>
    </div>
  )
}
