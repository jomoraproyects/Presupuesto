import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { useAuth } from '../context/AuthContext'
import { useVigencia } from '../context/VigenciaContext'
import { consolidadosService } from '../services/consolidados'
import { formulacionService } from '../services/formulacion'
import type { ConsolidadoArea, Formulacion, ResumenPresupuesto } from '../types/domain'
import { money, roleLabel } from '../utils/format'
import { responseState } from '../utils/formState'

export function DashboardPage() {
  const { user } = useAuth()
  const { selected, loading: vigenciaLoading } = useVigencia()
  const [rows, setRows] = useState<ConsolidadoArea[]>([])
  const [summary, setSummary] = useState<ResumenPresupuesto | null>(null)
  const [form, setForm] = useState<Formulacion | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    if (!selected || !user) return
    let active = true; setLoading(true); setError(''); setRows([]); setSummary(null); setForm(null)
    const request = user.rol === 'RESPONSABLE_AREA'
      ? formulacionService.get(selected.id).then(value => { if (active) setForm(value) })
      : Promise.all([consolidadosService.byArea(selected.id), consolidadosService.summary(selected.id)])
      .then(([areas, totals]) => { if (active) { setRows(areas); setSummary(totals) } })
    request.catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [selected?.id, user?.rol])
  const responded = rows.filter(row => ['ENVIADO', 'EN_REVISION', 'APROBADO'].includes(row.estado)).length
  const drafts = rows.filter(row => row.estado === 'BORRADOR').length
  const pending = rows.filter(row => row.estado === 'PENDIENTE').length
  const welcome = user && <div className="dashboard-welcome"><span>Bienvenido al presupuesto</span><strong>{user.nombre}</strong><small>{form?.area && user.rol === 'RESPONSABLE_AREA' ? form.area : roleLabel[user.rol]}</small></div>
  if (loading || vigenciaLoading) return <>{welcome}<Loading label="Cargando presupuesto…" /></>
  if (user?.rol === 'RESPONSABLE_AREA') return <>{welcome}<PageTitle eyebrow="NUEVO PRESUPUESTO" title={selected ? `Presupuesto ${selected.anio}` : 'Mi presupuesto'}>{form?.area}</PageTitle>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    {!selected || !form?.disponible ? <Empty title="No hay presupuesto pendiente para esta dependencia" /> : form && <>
      <p><strong>Estado:</strong> {form.estado === 'PENDIENTE' ? 'PENDIENTE POR DILIGENCIAR' : form.estado === 'BORRADOR' ? 'BORRADOR' : form.estado === 'ENVIADO' ? 'ENVIADO A PRESUPUESTO' : form.estado === 'EN_REVISION' ? 'EN REVISIÓN' : 'APROBADO'}</p>
      <div className="row g-3 mb-3"><div className="col-md-6"><div className="metric-card metric-card--requested"><div className="metric-label">Total solicitado</div><div className="metric-value">{money(form.solicitado)}</div><div className="metric-note">Suma de los ítems de tu dependencia</div></div></div>
        <div className="col-md-6"><div className="metric-card metric-card--approved"><div className="metric-label">Total aprobado</div><div className="metric-value">{form.enviada ? money(form.aprobado) : 'Pendiente de envío'}</div><div className="metric-note">{form.estado === 'APROBADO' ? 'Valor final aprobado por Presupuesto' : 'Valor sujeto a revisión de Presupuesto'}</div></div></div></div>
      <Link to="/formulacion" className="btn btn-primary">{form.enviada ? 'Ver formulario enviado' : form.estado === 'PENDIENTE' ? 'DILIGENCIAR' : 'CONTINUAR'}</Link>
    </>}
  </>
  return <>{welcome}<PageTitle eyebrow="PRESUPUESTO" title={selected ? `Presupuesto ${selected.anio}` : 'Presupuesto'} action={<Link to="/presupuesto-detallado" className="btn btn-outline-primary">Presupuesto detallado</Link>}>
    {selected ? `Vigencia ${selected.anio}` : 'Seleccione una vigencia.'}
  </PageTitle>{error && <div className="alert alert-danger" role="alert">{error}</div>}
    {!selected ? <Empty title="No hay vigencias disponibles" /> : <>
      <div className="row g-3 mb-4">{[
        { label: 'TOTAL SOLICITADO GENERAL', value: summary?.totalSolicitado ?? 0, note: 'Lo enviado por las dependencias', tone: 'requested' },
        { label: 'TOTAL APROBADO GENERAL', value: summary?.totalAprobado ?? 0, note: 'Suma después de los ajustes', tone: 'approved' },
        { label: 'DIFERENCIA', value: (summary?.totalAprobado ?? 0) - (summary?.totalSolicitado ?? 0), note: 'Aprobado menos solicitado', tone: 'difference' },
      ].map(({ label, value, note, tone }) => <div className="col-md-4" key={label}><div className={`metric-card metric-card--${tone}`}><div className="metric-label">{label}</div><div className="metric-value">{money(value)}</div><div className="metric-note">{note}</div></div></div>)}</div>
      {!rows.length ? <Empty title="No hay dependencias configuradas" /> : <div className="card section-card dependency-card border-0">
        <div className="dependency-card-heading"><div><h2>Respuestas de las dependencias</h2><p>Consulta el estado y abre cada formulario para revisarlo.</p></div>
          <div className="response-counts" aria-label="Resumen de respuestas"><span className="response-count response-count--sent">{responded} {responded === 1 ? 'respondió' : 'respondieron'}</span><span className="response-count response-count--draft">{drafts} en elaboración</span><span className="response-count response-count--pending">{pending} {pending === 1 ? 'falta' : 'faltan'} por responder</span></div>
        </div>
        <div className="table-responsive"><table className="table align-middle mb-0"><thead><tr><th>Dependencia</th><th>Estado</th><th>Solicitado</th><th>Aprobado</th><th>Diferencia</th><th><span className="visually-hidden">Acción</span></th></tr></thead><tbody>
          {rows.map(row => { const state = responseState[row.estado]; return <tr className={`dependency-row dependency-row--${state.tone}`} key={row.areaId}><td><strong>{row.area}</strong></td><td><span className={`response-state response-state--${state.tone}`}>{state.label}</span></td><td>{money(row.solicitado)}</td><td>{money(row.aprobado)}</td><td>{money(row.diferencia)}</td><td><Link to={`/formulacion?areaId=${row.areaId}`} className={`btn btn-sm response-action response-action--${state.tone}`}>{state.action}<span aria-hidden="true">→</span></Link></td></tr> })}
        </tbody></table></div></div>}
    </>}
  </>
}
