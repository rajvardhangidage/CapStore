import { useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg } from '../api'
import { Field } from '../components/ui'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [done, setDone] = useState(false)
  const [err, setErr] = useState('')
  const submit = async (e) => {
    e.preventDefault(); setErr('')
    try { await api.post('/auth/forgot-password', { email }); setDone(true) } catch (x) { setErr(errMsg(x)) }
  }
  return (
    <div className="mx-auto max-w-md px-4 py-12">
      <form onSubmit={submit} className="card space-y-4 p-6">
        <h1 className="text-2xl font-extrabold">Forgot password</h1>
        {done ? <p className="rounded bg-moss-light p-3 text-sm text-moss">If that email is registered, a reset link is on its way. It is valid for 30 minutes.</p> : <>
          {err && <p role="alert" className="rounded bg-brim-light p-3 text-sm text-brim">{err}</p>}
          <Field label="Your email"><input className="input" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></Field>
          <button className="btn-primary w-full">Send reset link</button>
        </>}
        <Link to="/login" className="block text-center text-sm font-bold underline">Back to log in</Link>
      </form>
    </div>
  )
}
