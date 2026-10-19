import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { TOKEN_KEY } from '../api/client'
import { authService } from '../services/auth'
import type { Perfil } from '../types/domain'

interface AuthState {
  user: Perfil | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  refreshUser: () => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Perfil | null>(null)
  const [loading, setLoading] = useState(true)

  const logout = useCallback(() => {
    sessionStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  useEffect(() => {
    let active = true
    if (sessionStorage.getItem(TOKEN_KEY)) {
      authService.me().then(profile => { if (active) setUser(profile) })
        .catch(() => { if (active) logout() })
        .finally(() => { if (active) setLoading(false) })
    } else setLoading(false)
    const expired = () => logout()
    window.addEventListener('presupuesto:session-expired', expired)
    return () => { active = false; window.removeEventListener('presupuesto:session-expired', expired) }
  }, [logout])

  const login = useCallback(async (username: string, password: string) => {
    const response = await authService.login(username, password)
    sessionStorage.setItem(TOKEN_KEY, response.token)
    try { setUser(await authService.me()) }
    catch (error) { logout(); throw error }
  }, [logout])

  const refreshUser = useCallback(async () => { setUser(await authService.me()) }, [])

  const value = useMemo(() => ({ user, loading, login, refreshUser, logout }), [user, loading, login, refreshUser, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('AuthProvider no disponible')
  return context
}
