import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { AlertCircle, CheckCircle2, CircleHelp, X } from 'lucide-react'

const ToastCtx = createContext(() => {})
const DialogCtx = createContext(null)
export const useToast = () => useContext(ToastCtx)
export const useDialog = () => {
  const dialog = useContext(DialogCtx)
  if (!dialog) throw new Error('useDialog must be used within ToastProvider')
  return dialog
}

export function ToastProvider({ children }) {
  const [notifications, setNotifications] = useState([])
  const [queue, setQueue] = useState([])
  const [promptValue, setPromptValue] = useState('')
  const nextId = useRef(0)
  const active = queue[0]

  const enqueue = useCallback((item) => new Promise((resolve) => {
    setQueue((items) => [...items, { ...item, id: ++nextId.current, resolve }])
  }), [])

  const toast = useCallback((message, type = 'success') => {
    const id = ++nextId.current
    setNotifications((items) => [...items, { id, message, type }])
    setTimeout(() => setNotifications((items) => items.filter((item) => item.id !== id)), 4500)
  }, [])

  const confirm = useCallback((message) => enqueue({ kind: 'confirm', title: 'Please confirm', message }), [enqueue])
  const prompt = useCallback((title, defaultValue = '') => enqueue({
    kind: 'prompt',
    title,
    message: 'Enter a new name.',
    value: defaultValue,
  }), [enqueue])

  const finish = useCallback((result) => {
    if (!active) return
    setQueue((items) => items.filter((item) => item.id !== active.id))
    active.resolve(result)
  }, [active])

  useEffect(() => {
    if (!active) return undefined
    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
        finish(active.kind === 'confirm' ? false : active.kind === 'prompt' ? null : undefined)
      }
    }
    document.addEventListener('keydown', onKeyDown)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = ''
    }
  }, [active, finish])

  useEffect(() => {
    setPromptValue(active?.kind === 'prompt' ? active.value : '')
  }, [active?.id])

  const dialogs = { confirm, prompt }
  return (
    <DialogCtx.Provider value={dialogs}>
      <ToastCtx.Provider value={toast}>
        {children}
        <div className="pointer-events-none fixed inset-x-0 bottom-4 z-[100] flex flex-col items-center gap-2 px-4" role="status" aria-live="polite">
          {notifications.map((item) => (
            <div key={item.id} className={`pointer-events-auto flex max-w-md items-start gap-2 rounded-md px-4 py-3 text-sm font-medium text-white shadow-lg ${item.type === 'error' ? 'bg-brim' : 'bg-ink'}`}>
              {item.type === 'error' ? <AlertCircle size={18} className="mt-0.5 shrink-0" /> : <CheckCircle2 size={18} className="mt-0.5 shrink-0" />}
              <span className="flex-1">{item.message}</span>
              <button onClick={() => setNotifications((items) => items.filter((notification) => notification.id !== item.id))} aria-label="Dismiss"><X size={16} /></button>
            </div>
          ))}
        </div>
        {active && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center bg-ink/50 p-4" role="presentation">
            <section
              className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl"
              role="alertdialog"
              aria-modal="true"
              aria-labelledby="popup-title"
              aria-describedby="popup-message"
            >
              <div className="mb-3 flex items-start gap-3">
                <CircleHelp size={22} className="mt-0.5 shrink-0 text-ink-soft" />
                <div className="min-w-0 flex-1">
                  <h2 id="popup-title" className="text-lg font-bold">
                    {active.title || (active.type === 'error' ? 'Something went wrong' : 'Success')}
                  </h2>
                  <p id="popup-message" className="mt-1 whitespace-pre-wrap text-sm text-ink-soft">{active.message}</p>
                </div>
                <button
                  onClick={() => finish(active.kind === 'confirm' ? false : active.kind === 'prompt' ? null : undefined)}
                  aria-label="Close popup"
                  className="rounded p-1 text-ink-faint hover:bg-twill"
                >
                  <X size={18} />
                </button>
              </div>
              {active.kind === 'prompt' && (
                <input
                  autoFocus
                  className="input mb-4"
                  value={promptValue}
                  onChange={(event) => setPromptValue(event.target.value)}
                  aria-label={active.title}
                />
              )}
              <div className="flex justify-end gap-2">
                <button className="btn-outline" onClick={() => finish(active.kind === 'confirm' ? false : null)}>Cancel</button>
                <button
                  autoFocus={active.kind !== 'prompt'}
                  className="btn-primary"
                  onClick={() => finish(active.kind === 'confirm' ? true : active.kind === 'prompt' ? promptValue : undefined)}
                >
                  {active.kind === 'confirm' ? 'Confirm' : 'Save'}
                </button>
              </div>
            </section>
          </div>
        )}
      </ToastCtx.Provider>
    </DialogCtx.Provider>
  )
}
