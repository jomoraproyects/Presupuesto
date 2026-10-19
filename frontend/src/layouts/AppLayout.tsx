import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { useVigencia } from '../context/VigenciaContext'
import { roleLabel } from '../utils/format'
import { Icon } from '../components/Icon'

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(() => window.matchMedia('(max-width:650px)').matches)
  const { user, logout } = useAuth()
  const { vigencias, selectedId, select, loading, error } = useVigencia()
  const navigate = useNavigate()
  if (!user) return null
  const canConsolidate = user.rol !== 'RESPONSABLE_AREA'
  const links = [
    ...(canConsolidate ? [{ to: '/dashboard', label: 'Dependencias', icon: 'home' as const }, ...(user.rol === 'SUPER_ADMIN' ? [{ to: '/crear-presupuesto', label: 'Crear presupuesto', icon: 'create' as const }] : [])] : [{ to: '/dashboard', label: 'Mi presupuesto', icon: 'home' as const }, { to: '/formulacion', label: 'Mi formulario', icon: 'form' as const }]),
    ...(canConsolidate ? [
      { to: '/presupuesto-detallado', label: 'Presupuesto detallado', icon: 'detail' as const },
      { to: '/presupuesto-general', label: 'Presupuesto General', icon: 'general' as const },
    ] : []),
  ]
  const settings = [
    ...(user.rol === 'SUPER_ADMIN' ? [
      { to: '/admin/areas', label: 'Dependencias', icon: 'settings' as const }, { to: '/admin/usuarios', label: 'Usuarios y nombres', icon: 'settings' as const },
      { to: '/admin/rubros', label: 'Conceptos del formulario', icon: 'settings' as const },
    ] : []),
  ]
  return <div className={`app-shell ${collapsed ? 'sidebar-collapsed' : ''}`}>
    <aside className="app-sidebar">
      <div className="brand"><Link to="/dashboard">S<span>·</span> Presupuesto</Link></div>
      <div className="sidebar-label">NAVEGACIÓN</div>
      <nav aria-label="Navegación principal">{links.map(link => <NavLink key={link.to} to={link.to} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><Icon name={link.icon} /><span>{link.label}</span></NavLink>)}</nav>
      {settings.length > 0 && <><div className="sidebar-label">ADMINISTRACIÓN</div><nav aria-label="Administración">{settings.map(link => <NavLink key={link.to} to={link.to} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><Icon name={link.icon} /><span>{link.label}</span></NavLink>)}</nav></>}
      <div className="sidebar-footer">Serviciudad E.S.P.<br />Gestión presupuestal</div>
    </aside>
    <div className="app-main">
      <header className="app-header">
        <button type="button" className="btn btn-outline-secondary btn-sm" aria-label="Colapsar menú" onClick={() => setCollapsed(!collapsed)}>☰</button>
        <div className="header-vigencia"><label htmlFor="vigencia-global" className="small text-muted me-2">Vigencia</label><select id="vigencia-global" className="form-select form-select-sm" value={selectedId ?? ''} disabled={loading || vigencias.length === 0} onChange={event => select(Number(event.target.value))}>
          {vigencias.length === 0 && <option value="">Sin vigencias</option>}{vigencias.map(v => <option key={v.id} value={v.id}>{v.anio} · {v.nombre}</option>)}
        </select></div>
        <div className="header-user" title={`${user.nombre} · ${roleLabel[user.rol]}`}><strong>{user.nombre}</strong><span>{roleLabel[user.rol]}</span></div>
        <button type="button" className="btn btn-outline-secondary btn-sm logout-button" aria-label="Cerrar sesión" onClick={() => { logout(); navigate('/login') }}><Icon name="logout" size={19} /><span>Salir</span></button>
      </header>
      <main className="page-content">{error && <div className="alert alert-danger" role="alert">No se pudieron cargar las vigencias: {error}</div>}<Outlet /></main>
    </div>
  </div>
}
