import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

type Tone = 'success' | 'danger' | 'info'
interface Notice { id: number; text: string; tone: Tone }
interface ToastState { notify: (text: string, tone?: Tone) => void }
const ToastContext = createContext<ToastState | null>(null)
let nextId = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [notices, setNotices] = useState<Notice[]>([])
  const notify = useCallback((text: string, tone: Tone = 'info') => {
    const id = ++nextId
    setNotices(current => [...current, { id, text, tone }])
    window.setTimeout(() => setNotices(current => current.filter(item => item.id !== id)), 5500)
  }, [])
  const value = useMemo(() => ({ notify }), [notify])
  return <ToastContext.Provider value={value}>
    {children}
    <div className="toast-region" aria-live="polite">
      {notices.map(notice => <div key={notice.id} className={`alert alert-${notice.tone} shadow-sm d-flex gap-3 align-items-start`} role="status">
        <span className="flex-grow-1">{notice.text}</span>
        <button type="button" className="btn-close" aria-label="Cerrar mensaje"
          onClick={() => setNotices(current => current.filter(item => item.id !== notice.id))} />
      </div>)}
    </div>
  </ToastContext.Provider>
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('ToastProvider no disponible')
  return context
}
