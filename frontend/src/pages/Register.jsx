import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { errMsg } from '../api'
import AuthShell from '../components/AuthShell'
import { Field } from '../components/ui'
import { useAuth } from '../context/AuthContext'

export default function Register() {
  const { register } = useAuth()
  const nav = useNavigate()
  const loc = useLocation()
  const [f, setF] = useState({ name: '', email: '', phone: '', password: '' })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try {
      await register(f)
      nav(loc.state?.from || '/', { replace: true })
    } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }
  return (
    <AuthShell title="Create your account">
      <form onSubmit={submit} className="space-y-5">
        {err && <p role="alert" className="rounded-sm border border-brim/20 bg-brim-light p-3 text-sm text-brim">{err}</p>}
        <Field label="Full name"><input className="input" required autoComplete="name" value={f.name} onChange={set('name')} /></Field>
        <Field label="Email"><input className="input" type="email" required autoComplete="email" value={f.email} onChange={set('email')} /></Field>
        <Field label="Phone (10 digits)"><input className="input" inputMode="numeric" maxLength={10} required autoComplete="tel" value={f.phone} onChange={set('phone')} /></Field>
        <Field label="Password" hint="At least 8 characters"><input className="input" type="password" required minLength={8} autoComplete="new-password" value={f.password} onChange={set('password')} /></Field>
        <button className="btn-primary btn-lg w-full" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
        <p className="text-center text-sm">Already have an account? <Link to="/login" state={loc.state} className="link">Log in</Link></p>
      </form>
    </AuthShell>
  )
}
