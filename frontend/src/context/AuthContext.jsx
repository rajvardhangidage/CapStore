import { createContext, useContext, useEffect, useState } from 'react'
import api, { refreshSession, tokens } from '../api'

const AuthCtx = createContext(null)
export const useAuth = () => useContext(AuthCtx)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (tokens.access) {
      api.get('/me').then((r) => setUser(r.data)).catch(() => tokens.clear()).finally(() => setLoading(false))
    } else setLoading(false)
    const onLogout = () => setUser(null)
    window.addEventListener('cs:logout', onLogout)
    return () => window.removeEventListener('cs:logout', onLogout)
  }, [])

  useEffect(() => {
    if (!user || !tokens.refresh) return

    let timer
    let stopped = false
    const onRefreshError = (error) => {
      if (error.response?.status === 401 || error.response?.status === 403) {
        tokens.clear()
        window.dispatchEvent(new Event('cs:logout'))
      } else if (!stopped) {
        schedule(30_000)
      }
    }
    const schedule = (retryAfter) => {
      clearTimeout(timer)
      const expiresAt = tokens.refreshExpiresAt
      const delay = retryAfter ?? (expiresAt ? Math.max(1000, expiresAt - Date.now() - 90_000) : 180_000)
      timer = setTimeout(() => {
        refreshSession().then(() => {
          if (!stopped) schedule()
        }).catch(onRefreshError)
      }, delay)
    }
    const refreshIfExpiring = () => {
      if (document.visibilityState !== 'visible') return
      const expiresAt = tokens.refreshExpiresAt
      if (expiresAt && expiresAt - Date.now() < 90_000) {
        clearTimeout(timer)
        refreshSession().then(() => {
          if (!stopped) schedule()
        }).catch(onRefreshError)
      }
    }

    schedule()
    document.addEventListener('visibilitychange', refreshIfExpiring)
    return () => {
      stopped = true
      clearTimeout(timer)
      document.removeEventListener('visibilitychange', refreshIfExpiring)
    }
  }, [user])

  const accept = (data) => { tokens.set(data.accessToken, data.refreshToken); setUser(data.user); return data.user }
  const login = async (email, password) => accept((await api.post('/auth/login', { email, password })).data)
  const register = async (payload) => accept((await api.post('/auth/register', payload)).data)
  const updateProfile = async (payload) => accept((await api.put('/me', payload)).data)
  const logout = () => { tokens.clear(); setUser(null) }

  return (
    <AuthCtx.Provider value={{ user, loading, login, register, updateProfile, logout, isAdmin: user?.role === 'ADMIN', isB2B: user?.role === 'B2B_USER' }}>
      {children}
    </AuthCtx.Provider>
  )
}
