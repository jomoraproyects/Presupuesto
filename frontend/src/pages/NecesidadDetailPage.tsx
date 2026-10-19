import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { apiMessage } from '../api/client'
import { Loading, PageTitle } from '../components/Feedback'
import { StateBadge } from '../components/StateBadge'
import { DistributionEditor } from '../components/DistributionEditor'
import { useAuth } from '../context/AuthContext'
import { useVigencia } from '../context/VigenciaContext'
import { useToast } from '../context/ToastContext'
import { areasService, rubrosService, serviciosService } from '../services/catalogos'
import { formulacionService } from '../services/formulacion'
import { necesidadesService } from '../services/necesidades'
import type { Area, DistribucionInput, Historial, Necesidad, Rubro, Servicio, EstadoFormulario } from '../types/domain'
import { dateTime, money, number, stateLabel } from '../utils/format'

export function NecesidadDetailPage() {
  const { id } = useParams(); const navigate = useNavigate(); const { user } = useAuth(); const { notify } = useToast()
  const { vigencias } = useVigencia()
  const [item, setItem] = useState<Necesidad | null>(null); const [history, setHistory] = useState<Historial[]>([])
  const [areas, setAreas] = useState<Area[]>([]); const [rubros, setRubros] = useState<Rubro[]>([])
  const [services, setServices] = useState<Servicio[]>([]); const [formState, setFormState] = useState<EstadoFormulario | null>(null)
  const [approvedDistribution, setApprovedDistribution] = useState<DistribucionInput[]>([])
  const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  const [cantidad, setCantidad] = useState(''); const [unitario, setUnitario] = useState(''); const [observacion, setObservacion] = useState('')
  const refresh = useCallback(async () => {
    const [n, h] = await Promise.all([necesidadesService.get(Number(id)), necesidadesService.history(Number(id))])
    setItem(n); setHistory(h); setCantidad(String(n.cantidadAprobada ?? n.cantidadSolicitada)); setUnitario(String(n.valorUnitarioAprobado ?? n.valorUnitarioSolicitado))
    setApprovedDistribution(n.distribuciones.filter(d => (d.porcentajeAprobado ?? d.porcentaje) > 0).map(d => ({ servicioId: d.servicioId, porcentaje: d.porcentajeAprobado ?? d.porcentaje })))
    if (user?.rol === 'SUPER_ADMIN') setFormState((await formulacionService.get(n.vigenciaId, n.areaId)).estado)
  }, [id, user?.rol])
  useEffect(() => {
    let active = true; setLoading(true)
    Promise.all([refresh(), areasService.list(), rubrosService.list(), serviciosService.list()]).then(([, a, r, s]) => { if (active) { setAreas(a); setRubros(r); setServices(s) } })
      .catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [refresh])
  const act = async (work: () => Promise<unknown>, success: string) => {
    setBusy(true); setError('')
    try { await work(); await refresh(); notify(success, 'success'); if (success === 'Aprobado actualizado.' && item) navigate(`/formulacion?areaId=${item.areaId}#rubro-${item.rubroPresupuestalId}`); setObservacion('') }
    catch (reason) { setError(apiMessage(reason)) } finally { setBusy(false) }
  }
  const submitAdjustment = (event: FormEvent) => {
    event.preventDefault()
    if (!item) return
    void act(() => necesidadesService.saveApproved(item.id, { cantidadAprobada: Number(cantidad), valorUnitarioAprobado: Number(unitario), observacionPresupuesto: observacion.trim() || undefined, distribucionesAprobadas: approvedDistribution }), 'Aprobado actualizado.')
  }
  if (loading) return <Loading label="Cargando necesidad e historial…" />
  if (!item) return <div className="alert alert-danger">{error || 'No se encontró la necesidad.'}</div>
  const areaName = areas.find(a => a.id === item.areaId)?.nombre ?? `Área ${item.areaId}`
  const rubro = rubros.find(r => r.id === item.rubroPresupuestalId)
  const canArea = user?.rol === 'RESPONSABLE_AREA'
  const canReview = user?.rol === 'SUPER_ADMIN'
  const vigencia = vigencias.find(v => v.id === item.vigenciaId)
  const reviewOpen = Boolean(vigencia?.activa && ['ABIERTA', 'EN_REVISION'].includes(vigencia.estado))
  const editable = canArea && ['BORRADOR', 'DEVUELTA'].includes(item.estado)
  const reviewable = canReview && reviewOpen && ['BORRADOR', 'ENVIADA', 'EN_REVISION', 'APROBADA', 'APROBADA_CON_AJUSTES', 'RECHAZADA'].includes(item.estado)
  const approvedPreview = Number(cantidad) * Number(unitario)
  return <><PageTitle eyebrow={`NECESIDAD #${item.id} · ${areaName}`} title={item.descripcion} action={<Link to={`/formulacion?areaId=${item.areaId}#rubro-${item.rubroPresupuestalId}`} className="btn btn-outline-secondary">Volver al formulario</Link>}><StateBadge state={item.estado} /> <span className="ms-2">{rubro ? `${user?.rol === 'RESPONSABLE_AREA' || rubro.codigo.startsWith('FORM.CONCEPTO.') ? '' : rubro.codigo + ' · '}${rubro.nombre}` : `Rubro ${item.rubroPresupuestalId}`}</span></PageTitle>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    {canReview && item.estado === 'BORRADOR' && <div className="alert alert-info" role="status">Revisión anticipada: esta dependencia todavía puede modificar lo solicitado. Tu ajuste aprobado se conservará al enviar el formulario y podrás revisarlo de nuevo.</div>}
    {canReview && reviewOpen && formState === 'APROBADO' && <div className="alert alert-info" role="status">Esta dependencia ya fue aprobada. Si guardas otro ajuste, volverá a estar en revisión y podrás aprobarla nuevamente.</div>}
    <div className="row g-3 mb-3"><div className="col-lg-6"><div className="card section-card border-0 h-100"><div className="card-body"><h2 className="h5">Solicitado por el área</h2><strong className="detail-amount">{money(item.valorTotalSolicitado)}</strong><dl className="detail-list"><dt>Cantidad</dt><dd>{number(item.cantidadSolicitada)}</dd><dt>Valor unitario</dt><dd>{money(item.valorUnitarioSolicitado)}</dd></dl>{item.valorTotalSolicitadoOriginal != null && <p className="small text-muted mb-0">Primer envío conservado: {number(item.cantidadSolicitadaOriginal)} × {money(item.valorUnitarioSolicitadoOriginal)} = {money(item.valorTotalSolicitadoOriginal)}</p>}</div></div></div>
      <div className="col-lg-6"><div className="card section-card border-0 h-100"><div className="card-body"><h2 className="h5">Aprobado por Presupuesto</h2><strong className="detail-amount">{item.valorTotalAprobado == null ? 'Pendiente' : money(item.valorTotalAprobado)}</strong><dl className="detail-list"><dt>Cantidad</dt><dd>{item.cantidadAprobada == null ? 'Pendiente' : number(item.cantidadAprobada)}</dd><dt>Valor unitario</dt><dd>{item.valorUnitarioAprobado == null ? 'Pendiente' : money(item.valorUnitarioAprobado)}</dd><dt>Diferencia</dt><dd className="fw-bold">{item.valorTotalAprobado == null ? 'Pendiente' : money(item.valorTotalAprobado - item.valorTotalSolicitado)}</dd></dl></div></div></div></div>
    {(item.justificacion || item.observacionPresupuesto) && <div className="card section-card border-0 mb-3"><div className="card-body">{item.justificacion && <><h2 className="h5">Justificación</h2><p className="mb-0 pre-wrap">{item.justificacion}</p></>}{item.observacionPresupuesto && <div className="alert alert-info mt-3 mb-0"><strong>Observación de Presupuesto:</strong> {item.observacionPresupuesto}</div>}</div></div>}
    <div className="card section-card border-0 mb-3"><div className="card-body"><h2 className="h5">Distribución por servicios</h2><div className="table-responsive"><table className="table mb-0"><thead><tr><th>Servicio</th><th>Solicitado</th><th>Aprobado</th></tr></thead><tbody>{item.distribuciones.map(d => <tr key={d.id}><td>{d.servicio}</td><td>{number(d.porcentaje)} % · {money(d.valorSolicitadoDistribuido)}</td><td>{item.valorTotalAprobado == null ? 'Pendiente' : `${number(d.porcentajeAprobado ?? d.porcentaje)} % · ${money(d.valorAprobadoDistribuido)}`}</td></tr>)}</tbody></table></div><p className="small mb-0">General solicitado: {money(item.valorTotalSolicitado - item.distribuciones.reduce((sum, d) => sum + d.valorSolicitadoDistribuido, 0))} · General aprobado: {item.valorTotalAprobado == null ? 'Pendiente' : money(item.valorTotalAprobado - item.distribuciones.reduce((sum, d) => sum + d.valorAprobadoDistribuido, 0))}</p></div></div>
    {editable && <div className="d-flex gap-2 mb-3"><Link to={`/necesidades/${item.id}/editar`} className="btn btn-primary">Editar ítem</Link>{item.estado === 'BORRADOR' && <button className="btn btn-outline-danger" disabled={busy} onClick={() => void act(async () => { await necesidadesService.cancel(item.id); navigate(`/formulacion?areaId=${item.areaId}`) }, 'Borrador cancelado.')}>Quitar ítem</button>}</div>}
    {reviewable && <div className="card section-card border-0 mb-3"><div className="card-body"><h2 className="h5">Cantidad y valor aprobado</h2><p className="text-muted small">Solicitud original: {number(item.cantidadSolicitada)} × {money(item.valorUnitarioSolicitado)} = {money(item.valorTotalSolicitado)}</p><form onSubmit={submitAdjustment}><div className="row g-3"><div className="col-md-6"><label htmlFor="a-cantidad" className="form-label">Cantidad aprobada</label><input id="a-cantidad" className="form-control" type="number" min="0" step="0.0001" value={cantidad} onChange={e => setCantidad(e.target.value)} required /></div><div className="col-md-6"><label htmlFor="a-unitario" className="form-label">Valor unitario aprobado</label><input id="a-unitario" className="form-control" type="number" min="0" step="0.01" value={unitario} onChange={e => setUnitario(e.target.value)} required /></div><div className="col-md-6"><div className="total-preview"><span>Total aprobado</span><strong>{money(approvedPreview)}</strong></div></div><div className="col-md-6"><div className="total-preview"><span>Diferencia</span><strong>{money(approvedPreview - item.valorTotalSolicitado)}</strong></div></div><div className="col-12"><DistributionEditor services={services} total={approvedPreview} value={approvedDistribution} onChange={setApprovedDistribution} /></div><div className="col-12"><label htmlFor="a-observacion" className="form-label">Motivo (opcional)</label><textarea id="a-observacion" className="form-control" rows={2} maxLength={2000} value={observacion} onChange={e => setObservacion(e.target.value)} /></div><div className="col-12"><button className="btn btn-primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar aprobado'}</button></div></div></form></div></div>}
    <div className="card section-card border-0"><div className="card-body"><h2 className="h5 mb-3">Historial</h2>{history.length === 0 ? <p className="text-muted mb-0">Aún no hay movimientos.</p> : <ol className="timeline">{history.map(h => <li key={h.id}><time>{dateTime(h.fecha)}</time><strong>{h.descripcion || h.accion.replaceAll('_', ' ')}</strong><span className="text-muted small">{h.estadoAnterior ? `${stateLabel[h.estadoAnterior]} → ` : ''}{stateLabel[h.estadoNuevo]} · {h.usuario || `Usuario #${h.usuarioId}`}</span>{h.valorAprobadoAnterior !== h.valorAprobadoNuevo && <span className="small">Valor: {money(h.valorAprobadoAnterior ?? h.valorSolicitado)} → {money(h.valorAprobadoNuevo)}</span>}{h.cantidadAnterior !== h.cantidadNueva && <span className="small">Cantidad: {number(h.cantidadAnterior)} → {number(h.cantidadNueva)}</span>}{h.valorUnitarioAnterior !== h.valorUnitarioNuevo && <span className="small">Unitario: {money(h.valorUnitarioAnterior)} → {money(h.valorUnitarioNuevo)}</span>}{h.motivo && <span className="small pre-wrap">Motivo: {h.motivo}</span>}</li>)}</ol>}</div></div>
  </>
}
