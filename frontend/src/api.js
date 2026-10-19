import axios from 'axios'

const BASE = import.meta.env.VITE_API_URL || '/api/v1'
const ACCESS = 'cs_access'
const REFRESH = 'cs_refresh'

export const tokens = {
  set(a, r) { localStorage.setItem(ACCESS, a); localStorage.setItem(REFRESH, r) },
  clear() { localStorage.removeItem(ACCESS); localStorage.removeItem(REFRESH) },
  get access() { return localStorage.getItem(ACCESS) },
  get refresh() { return localStorage.getItem(REFRESH) },
  get refreshExpiresAt() {
    const payload = this.refresh?.split('.')[1]
    if (!payload) return null
    try {
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
      const claims = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')))
      return typeof claims.exp === 'number' ? claims.exp * 1000 : null
    } catch {
      return null
    }
  },
}

const api = axios.create({ baseURL: BASE })

api.interceptors.request.use((cfg) => {
  if (tokens.access) cfg.headers.Authorization = `Bearer ${tokens.access}`
  return cfg
})

let refreshing = null

export function refreshSession() {
  if (!tokens.refresh) return Promise.reject(new Error('No refresh token available'))
  if (!refreshing) {
    refreshing = axios.post(`${BASE}/auth/refresh`, { refreshToken: tokens.refresh })
      .then(({ data }) => {
        tokens.set(data.accessToken, data.refreshToken)
        return data
      })
      .finally(() => { refreshing = null })
  }
  return refreshing
}

api.interceptors.response.use(
  (r) => r,
  async (err) => {
    const orig = err.config
    const isAuthCall = orig?.url?.includes('/auth/')
    if (err.response?.status === 401 && orig && !orig._retry && tokens.refresh && !isAuthCall) {
      orig._retry = true
      try {
        await refreshSession()
        orig.headers.Authorization = `Bearer ${data.accessToken}`
        return api(orig)
      } catch (e) {
        if (e.response?.status === 401 || e.response?.status === 403) {
          tokens.clear()
          window.dispatchEvent(new Event('cs:logout'))
        }
      }
    }
    return Promise.reject(err)
  }
)

/** Human-friendly message from an API error */
export function errMsg(e) {
  const d = e?.response?.data
  if (d?.errors) return Object.values(d.errors)[0]
  if (d?.message) return d.message
  if (e?.response?.status === 429) return 'Too many attempts. Please wait a minute.'
  if (!e?.response) return 'Cannot reach the server. Check your connection and try again.'
  return e?.message || 'Something went wrong'
}

export async function downloadBlob(url, filename, params) {
  const res = await api.get(url, { responseType: 'blob', params })
  const href = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = href
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(href)
}

export default api
