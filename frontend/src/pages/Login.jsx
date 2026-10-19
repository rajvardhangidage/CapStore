import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { errMsg } from '../api'
import { Field } from '../components/ui'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { login } = useAuth()
  const nav = useNavigate()
  const loc = useLocation()
  const [f, setF] = useState({ email: '', password: '' })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try {
      const u = await login(f.email, f.password)
      nav(loc.state?.from || (u.role === 'ADMIN' ? '/admin' : '/'), { replace: true })
    } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }
  return (
    <div className="mx-auto max-w-md px-4 py-12">
      <form onSubmit={submit} className="card space-y-4 p-6">
        <h1 className="text-2xl font-extrabold">Log in</h1>
        {err && <p role="alert" className="rounded bg-brim-light p-3 text-sm text-brim">{err}</p>}
        <Field label="Email"><input className="input" type="email" autoComplete="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></Field>
        <Field label="Password"><input className="input" type="password" autoComplete="current-password" required value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></Field>
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
        <div className="flex justify-between text-sm"><Link to="/forgot-password" className="font-bold underline">Forgot password?</Link><Link to="/register" state={loc.state} className="font-bold underline">Create account</Link></div>
      </form>
    </div>
  )
}
