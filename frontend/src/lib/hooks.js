import { useEffect, useState } from 'react'
import api, { errMsg } from '../api'

/** Runs an async function on mount / when deps change. fn must return the data. */
export function useAsync(fn, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [tick, setTick] = useState(0)
  useEffect(() => {
    let alive = true
    setState((s) => ({ ...s, loading: true, error: null }))
    fn()
      .then((data) => alive && setState({ data, loading: false, error: null }))
      .catch((e) => alive && setState({ data: null, loading: false, error: errMsg(e) }))
    return () => { alive = false }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])
  return { ...state, reload: () => setTick((t) => t + 1) }
}

let configCache = null

export function updateStoreConfig(config) {
  configCache = config
  window.dispatchEvent(new Event('store-config-updated'))
}

export function useStoreConfig() {
  const [cfg, setCfg] = useState(configCache)
  useEffect(() => {
    const syncConfig = () => setCfg(configCache)
    window.addEventListener('store-config-updated', syncConfig)
    if (!configCache) api.get('/config').then((r) => { updateStoreConfig(r.data) }).catch(() => {})
    return () => window.removeEventListener('store-config-updated', syncConfig)
  }, [])
  return cfg
}
