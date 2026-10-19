import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { useVigencia } from '../context/VigenciaContext'
import { consolidadosService } from '../services/consolidados'
import type { ConsolidadoArea, PresupuestoGeneralConcepto, ResumenPresupuesto } from '../types/domain'
import { money } from '../utils/format'
import { responseState } from '../utils/formState'

export function PresupuestoDetalladoPage() {
  const { selected } = useVigencia()
  const [rows, setRows] = useState<PresupuestoGeneralConcepto[]>([])
  const [areas, setAreas] = useState<ConsolidadoArea[]>([])
  const [summary, setSummary] = useState<ResumenPresupuesto | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    if (!selected) { setRows([]); setAreas([]); setSummary(null); return }
    let active = true; setLoading(true); setError(''); setRows([]); setAreas([]); setSummary(null)
    Promise.all([consolidadosService.general(selected.id), consolidadosService.byArea(selected.id), consolidadosService.summary(selected.id)])
      .then(([concepts, dependencies, totals]) => { if (active) { setRows(concepts); setAreas(dependencies); setSummary(totals) } })
      .catch(e => { if (active) setError(apiMessage(e)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [selected?.id])
  const solicitado = summary?.totalSolicitado ?? 0
  const aprobado = summary?.totalAprobado ?? 0
  const reviewOpen = Boolean(selected?.activa && ['ABIERTA', 'EN_REVISION'].includes(selected.estado))
  return <><PageTitle eyebrow="PRESUPUESTO DETALLADO" title={selected ? `Presupuesto detallado ${selected.anio}` : 'Presupuesto detallado'}>Consulta cada dependencia, concepto e ítem para revisar y ajustar lo aprobado.</PageTitle>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    {!selected ? <Empty title="Seleccione una vigencia" /> : loading ? <Loading label="Calculando presupuesto detallado…" /> : <>
      <div className="row g-3 mb-4" aria-label="Totales del presupuesto detallado">
        <div className="col-md-4"><div className="metric-card metric-card--approved"><div className="metric-label">TOTAL PRESUPUESTO APROBADO</div><div className="metric-value">{money(aprobado)}</div><div className="metric-note">Suma de lo aprobado en todas las dependencias después de los recortes y ajustes</div></div></div>
        <div className="col-md-4"><div className="metric-card metric-card--requested"><div className="metric-label">TOTAL SOLICITADO</div><div className="metric-value">{money(solicitado)}</div><div className="metric-note">Suma de los valores solicitados originalmente</div></div></div>
        <div className="col-md-4"><div className="metric-card metric-card--difference"><div className="metric-label">DIFERENCIA</div><div className="metric-value">{money(aprobado - solicitado)}</div><div className="metric-note">Aprobado menos solicitado</div></div></div>
      </div>
      {areas.length > 0 && <section className="card section-card dependency-card border-0 mb-4" aria-labelledby="general-dependencies-title">
        <div className="dependency-card-heading"><div><h2 id="general-dependencies-title">Revisión y recortes por dependencia</h2><p>Abre cualquier dependencia con ítems. También puedes revisar y recortar sus borradores.</p></div></div>
        <div className="table-responsive"><table className="table align-middle mb-0"><thead><tr><th>Dependencia</th><th>Estado</th><th>Solicitado</th><th>Aprobado</th><th><span className="visually-hidden">Acción</span></th></tr></thead><tbody>
          {areas.map(area => { const state = responseState[area.estado]; return <tr className={`dependency-row dependency-row--${state.tone}`} key={area.areaId}><td><strong>{area.area}</strong></td><td><span className={`response-state response-state--${state.tone}`}>{state.label}</span></td><td>{money(area.solicitado)}</td><td>{money(area.aprobado)}</td><td><Link className={`btn btn-sm response-action response-action--${state.tone}`} to={`/formulacion?areaId=${area.areaId}`}>{!reviewOpen || area.estado === 'PENDIENTE' ? 'Ver formulario' : area.estado === 'APROBADO' ? 'Revisar de nuevo' : 'Revisar y recortar'}<span aria-hidden="true">→</span></Link></td></tr> })}
        </tbody></table></div>
      </section>}
      <h2 className="h5 mb-3">Conceptos del presupuesto ({rows.length})</h2>
      {rows.length === 0 && <Empty title="No hay conceptos configurados para esta vigencia" />}
      {rows.map(r => <section className="card section-card concept-card border-0 mb-3" key={r.conceptoId}><div className="card-body"><h2 className="h5 concept-title">{r.orden}. {r.nombre}</h2>
        {r.dependencias.length > 0 && <div className="table-responsive"><table className="table table-sm align-middle"><thead><tr><th>Dependencia</th><th>Solicitado</th><th>Aprobado</th><th>Diferencia</th><th></th></tr></thead><tbody>{r.dependencias.map(d => <tr key={d.areaId}><td>{d.area}</td><td>{money(d.solicitado)}</td><td>{money(d.aprobado)}</td><td>{money(d.diferencia)}</td><td><Link className="btn btn-outline-primary btn-sm" to={`/formulacion?areaId=${d.areaId}#rubro-${r.conceptoId}`}>Ver ítems</Link></td></tr>)}</tbody></table></div>}
        {r.dependencias.length === 0 && <p className="text-muted mb-3">Ninguna dependencia ha añadido ítems a este concepto.</p>}
        <div className="concept-totals"><span>Solicitado <strong>{money(r.solicitado)}</strong></span><span>Aprobado <strong>{money(r.aprobado)}</strong></span><span>Diferencia <strong>{money(r.diferencia)}</strong></span></div>
      </div></section>)}
    </>}
  </>
}
