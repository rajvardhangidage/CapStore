import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import api, { errMsg } from '../api'
import AuthShell from '../components/AuthShell'
import { Field } from '../components/ui'
import { useToast } from '../context/ToastContext'

export default function ResetPassword() {
  const [sp] = useSearchParams()
  const nav = useNavigate()
  const toast = useToast()
  const [pw, setPw] = useState('')
  const [err, setErr] = useState('')
  const submit = async (e) => {
    e.preventDefault(); setErr('')
    try { await api.post('/auth/reset-password', { token: sp.get('token'), newPassword: pw }); toast('Password updated. Please log in.'); nav('/login') } catch (x) { setErr(errMsg(x)) }
  }
  return (
    <AuthShell title="Choose a new password">
      <form onSubmit={submit} className="space-y-5">
        {!sp.get('token') && <p className="rounded-sm border border-brim/20 bg-brim-light p-3 text-sm text-brim">This reset link is incomplete. <Link to="/forgot-password" className="link">Request a new one.</Link></p>}
        {err && <p role="alert" className="rounded-sm border border-brim/20 bg-brim-light p-3 text-sm text-brim">{err}</p>}
        <Field label="New password" hint="At least 8 characters"><input className="input" type="password" required minLength={8} autoComplete="new-password" value={pw} onChange={(e) => setPw(e.target.value)} /></Field>
        <button className="btn-primary btn-lg w-full" disabled={!sp.get('token')}>Update password</button>
      </form>
    </AuthShell>
  )
}
