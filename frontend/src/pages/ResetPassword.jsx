import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import api, { errMsg } from '../api'
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
    <div className="mx-auto max-w-md px-4 py-12">
      <form onSubmit={submit} className="card space-y-4 p-6">
        <h1 className="text-2xl font-extrabold">Choose a new password</h1>
        {!sp.get('token') && <p className="rounded bg-brim-light p-3 text-sm text-brim">This reset link is incomplete. <Link to="/forgot-password" className="font-bold underline">Request a new one.</Link></p>}
        {err && <p role="alert" className="rounded bg-brim-light p-3 text-sm text-brim">{err}</p>}
        <Field label="New password" hint="At least 8 characters"><input className="input" type="password" required minLength={8} autoComplete="new-password" value={pw} onChange={(e) => setPw(e.target.value)} /></Field>
        <button className="btn-primary w-full" disabled={!sp.get('token')}>Update password</button>
      </form>
    </div>
  )
}
