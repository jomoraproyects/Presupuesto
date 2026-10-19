import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { vigenciasService } from '../services/catalogos'
import type { Vigencia } from '../types/domain'
import { useAuth } from './AuthContext'
import { apiMessage } from '../api/client'

interface VigenciaState {
  vigencias: Vigencia[]
  selected: Vigencia | null
  selectedId: number | null
  loading: boolean
  error: string
  select: (id: number) => void
  refresh: () => Promise<void>
}

const STORAGE_KEY = 'presupuesto.vigencia'
const VigenciaContext = createContext<VigenciaState | null>(null)

export function VigenciaProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [vigencias, setVigencias] = useState<Vigencia[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    if (!user) { setVigencias([]); setSelectedId(null); setError(''); return }
    setLoading(true); setError('')
    try {
      const rows = (await vigenciasService.list()).sort((a, b) => b.anio - a.anio)
      setVigencias(rows)
      setSelectedId(current => {
        const stored = Number(sessionStorage.getItem(STORAGE_KEY))
        const wanted = current ?? stored
        if (rows.some(row => row.id === wanted)) return wanted
        return rows.find(row => row.activa && (row.estado === 'ABIERTA' || row.estado === 'EN_REVISION'))?.id
          ?? rows.find(row => row.activa)?.id ?? rows[0]?.id ?? null
      })
    } catch (reason) { setError(apiMessage(reason)); throw reason } finally { setLoading(false) }
  }, [user])

  useEffect(() => { void refresh().catch(() => setLoading(false)) }, [refresh])

  const select = useCallback((id: number) => {
    sessionStorage.setItem(STORAGE_KEY, String(id))
    setSelectedId(id)
  }, [])
  const selected = vigencias.find(row => row.id === selectedId) ?? null
  const value = useMemo(() => ({ vigencias, selected, selectedId, loading, error, select, refresh }),
    [vigencias, selected, selectedId, loading, error, select, refresh])
  return <VigenciaContext.Provider value={value}>{children}</VigenciaContext.Provider>
}

export function useVigencia() {
  const context = useContext(VigenciaContext)
  if (!context) throw new Error('VigenciaProvider no disponible')
  return context
}
