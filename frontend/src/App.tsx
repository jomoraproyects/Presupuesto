import { Navigate, Route, Routes } from 'react-router'
import { useAuth } from './context/AuthContext'
import { Loading } from './components/Feedback'
import { AppLayout } from './layouts/AppLayout'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { NecesidadesPage } from './pages/NecesidadesPage'
import { NecesidadFormPage } from './pages/NecesidadFormPage'
import { NecesidadDetailPage } from './pages/NecesidadDetailPage'
import { ConsolidadoPage } from './pages/ConsolidadoPage'
import { AdminPage } from './pages/AdminPage'
import { FormulacionPage } from './pages/FormulacionPage'
import { CrearPresupuestoPage } from './pages/CrearPresupuestoPage'
import { PresupuestoGeneralPage } from './pages/PresupuestoGeneralPage'
import { PresupuestoDetalladoPage } from './pages/PresupuestoDetalladoPage'
import { MaestroPage } from './pages/MaestroPage'
import { ConfiguracionVigenciaPage } from './pages/ConfiguracionVigenciaPage'
import { SubgerenciasPage } from './pages/SubgerenciasPage'
import type { Role } from './types/domain'

function Protected({ roles, children }: { roles?: Role[]; children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <Loading label="Verificando sesión…" />
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.rol)) return <Navigate to="/dashboard" replace />
  return children
}

const writers: Role[] = ['RESPONSABLE_AREA']
const consolidators: Role[] = ['SUPER_ADMIN', 'CONSULTA']

export default function App() {
  return <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route element={<Protected><AppLayout /></Protected>}>
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/necesidades" element={<NecesidadesPage />} />
      <Route path="/formulacion" element={<FormulacionPage />} />
      <Route path="/crear-presupuesto" element={<Protected roles={['SUPER_ADMIN']}><CrearPresupuestoPage /></Protected>} />
      <Route path="/presupuesto-general" element={<Protected roles={consolidators}><PresupuestoGeneralPage /></Protected>} />
      <Route path="/presupuesto-detallado" element={<Protected roles={consolidators}><PresupuestoDetalladoPage /></Protected>} />
      <Route path="/necesidades/nueva" element={<Protected roles={writers}><NecesidadFormPage /></Protected>} />
      <Route path="/necesidades/:id/editar" element={<Protected roles={writers}><NecesidadFormPage /></Protected>} />
      <Route path="/necesidades/:id" element={<NecesidadDetailPage />} />
      <Route path="/consolidado/:kind" element={<Protected roles={consolidators}><ConsolidadoPage /></Protected>} />
      <Route path="/consolidado/maestro" element={<Protected roles={consolidators}><MaestroPage /></Protected>} />
      <Route path="/configuracion-vigencia" element={<Protected roles={consolidators}><ConfiguracionVigenciaPage /></Protected>} />
      <Route path="/admin/vigencias" element={<Protected roles={['SUPER_ADMIN']}><AdminPage catalog="vigencias" /></Protected>} />
      <Route path="/admin/subgerencias" element={<Protected roles={['SUPER_ADMIN']}><SubgerenciasPage /></Protected>} />
      <Route path="/admin/rubros" element={<Protected roles={['SUPER_ADMIN']}><AdminPage catalog="conceptos" /></Protected>} />
      <Route path="/admin/conceptos" element={<Protected roles={['SUPER_ADMIN']}><Navigate to="/admin/rubros" replace /></Protected>} />
      <Route path="/admin/:kind" element={<Protected roles={['SUPER_ADMIN']}><AdminPage /></Protected>} />
    </Route>
    <Route path="*" element={<Navigate to="/dashboard" replace />} />
  </Routes>
}
