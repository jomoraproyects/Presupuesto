import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { apiMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'

export function LoginPage() {
  const { user, loading, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  if (!loading && user) return <Navigate to="/dashboard" replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login(username.trim(), password)
      navigate('/dashboard', { replace: true })
    } catch (reason) {
      setError(apiMessage(reason))
    } finally {
      setBusy(false)
    }
  }

  return <main className="login-screen">
    <div className="login-shell">
      <section className="login-intro" aria-label="Presupuesto Serviciudad">
        <img className="login-logo" src="/serviciudad-logo.jpg" alt="Logo de Serviciudad E.S.P." />
        <h1>Presupuesto Serviciudad</h1>
      </section>

      <section className="login-card" aria-labelledby="login-title">
        <div className="login-form-wrap">
          <h2 id="login-title">Iniciar sesión</h2>

          {error && <div className="alert alert-danger login-error" role="alert">{error}</div>}

          <form onSubmit={submit}>
            <div className="login-field">
              <label htmlFor="username">Usuario</label>
              <input id="username" className="form-control" autoComplete="username" required value={username}
                onChange={event => setUsername(event.target.value)} placeholder="Tu usuario" />
            </div>
            <div className="login-field">
              <label htmlFor="password">Contraseña</label>
              <div className="login-password-field">
                <input id="password" className="form-control" type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password" required value={password}
                  onChange={event => setPassword(event.target.value)} placeholder="Tu contraseña" />
                <button type="button" className="login-password-toggle"
                  onClick={() => setShowPassword(value => !value)}
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  aria-pressed={showPassword}>
                  {showPassword ? 'Ocultar' : 'Mostrar'}
                </button>
              </div>
            </div>
            <button className="login-submit" type="submit" disabled={busy || loading}>
              <span>{busy ? 'Ingresando…' : 'Ingresar'}</span>
              <span aria-hidden="true">→</span>
            </button>
          </form>
        </div>
      </section>
    </div>
  </main>
}
