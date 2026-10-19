import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { apiMessage } from '../api/client'
import { DistributionEditor } from '../components/DistributionEditor'
import { Loading, PageTitle } from '../components/Feedback'
import { RubroSelector } from '../components/RubroSelector'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { useVigencia } from '../context/VigenciaContext'
import { areasService, serviciosService } from '../services/catalogos'
import { necesidadesService } from '../services/necesidades'
import type { Area, DistribucionInput, Servicio } from '../types/domain'
import { money } from '../utils/format'

export function NecesidadFormPage() {
  const { id } = useParams(); const editing = Boolean(id)
  const [query] = useSearchParams()
  const { user } = useAuth(); const { selectedId, vigencias } = useVigencia(); const { notify } = useToast(); const navigate = useNavigate()
  const [vigenciaId, setVigenciaId] = useState<number | null>(Number(query.get('vigenciaId')) || null); const [areaId, setAreaId] = useState<number | null>(Number(query.get('areaId')) || null)
  const [rubroId, setRubroId] = useState<number | null>(Number(query.get('rubroId')) || null)
  const [descripcion, setDescripcion] = useState('')
  const [cantidad, setCantidad] = useState('1'); const [unitario, setUnitario] = useState('0')
  const [distribuciones, setDistribuciones] = useState<DistribucionInput[]>([])
  const [areas, setAreas] = useState<Area[]>([]); const [services, setServices] = useState<Servicio[]>([])
  const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false); const [error, setError] = useState('')
  useEffect(() => { if (!editing && selectedId && vigenciaId === null) setVigenciaId(selectedId) }, [editing, selectedId, vigenciaId])
  useEffect(() => {
    let active = true; setLoading(true)
    Promise.all([areasService.list(), serviciosService.list(), id ? necesidadesService.get(Number(id)) : Promise.resolve(null)])
      .then(([a, s, item]) => { if (!active) return; setAreas(a); setServices(s)
        if (item) { setVigenciaId(item.vigenciaId); setAreaId(item.areaId); setRubroId(item.rubroPresupuestalId); setDescripcion(item.descripcion); setCantidad(String(item.cantidadSolicitada)); setUnitario(String(item.valorUnitarioSolicitado)); setDistribuciones(item.aplicaDistribucion ? item.distribuciones.map(d => ({ servicioId: d.servicioId, porcentaje: d.porcentaje })) : [])
          if (!['BORRADOR', 'DEVUELTA'].includes(item.estado)) setError('Esta necesidad ya no puede editarse.') }
      }).catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id, editing])
  const scaledQuantity = Math.round(Number(cantidad) * 10000)
  const unitCents = Math.round(Number(unitario) * 100)
  // Cantidad admite cuatro decimales y precio dos; redondear el producto una sola vez.
  const total = Number.isSafeInteger(scaledQuantity) && Number.isSafeInteger(unitCents)
    ? Number((BigInt(scaledQuantity) * BigInt(unitCents) + 5000n) / 10000n) / 100
    : Math.round(Number(cantidad) * Number(unitario) * 100) / 100
  const aplicaDistribucion = distribuciones.length > 0
  const distributionValid = distribuciones.reduce((sum, d) => sum + Math.round(d.porcentaje * 100), 0) === 10000
  async function submit(event: FormEvent) {
    event.preventDefault(); setError('')
    if (!vigenciaId || !rubroId) { setError('Seleccione una vigencia y un concepto.'); return }
    if (user?.rol === 'SUPER_ADMIN' && !areaId) { setError('Seleccione un área.'); return }
    if (aplicaDistribucion && !distributionValid) { setError('La distribución debe sumar exactamente 100 %.'); return }
    setSaving(true)
    try {
      const input = { vigenciaId, ...(user?.rol === 'SUPER_ADMIN' ? { areaId: areaId! } : {}), rubroPresupuestalId: rubroId, descripcion: descripcion.trim(), cantidadSolicitada: Number(cantidad), valorUnitarioSolicitado: Number(unitario), distribuciones, aplicaDistribucion }
      const item = editing ? await necesidadesService.update(Number(id), input) : await necesidadesService.create(input)
      notify(editing ? 'Ítem actualizado.' : 'Ítem guardado en el concepto.', 'success'); navigate(`/formulacion?areaId=${item.areaId}#rubro-${item.rubroPresupuestalId}`)
    } catch (reason) { setError(apiMessage(reason)) } finally { setSaving(false) }
  }
  if (loading) return <Loading label="Preparando formulario…" />
  return <><PageTitle eyebrow="FORMULARIO" title={editing ? 'Editar ítem' : 'Agregar ítem'} action={<Link className="btn btn-outline-secondary" to={`/formulacion${areaId ? `?areaId=${areaId}` : ''}`}>Volver al formulario</Link>}>El total solicitado se calcula a partir de cantidad y valor unitario.</PageTitle>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    <form onSubmit={submit}><div className="card section-card border-0 mb-3"><div className="card-body"><h2 className="h5 mb-3">Datos de la solicitud</h2><div className="row g-3">
      <div className="col-md-6"><label className="form-label" htmlFor="n-vigencia">Vigencia</label><select id="n-vigencia" className="form-select" required disabled={editing} value={vigenciaId ?? ''} onChange={e => { setVigenciaId(Number(e.target.value)); setRubroId(null) }}><option value="">Seleccione…</option>{vigencias.map(v => <option key={v.id} value={v.id}>{v.anio} · {v.nombre}</option>)}</select></div>
      {user?.rol === 'SUPER_ADMIN' && <div className="col-md-6"><label className="form-label" htmlFor="n-area">Área</label><select id="n-area" className="form-select" required disabled={editing} value={areaId ?? ''} onChange={e => setAreaId(Number(e.target.value))}><option value="">Seleccione…</option>{areas.filter(a => a.activa).map(a => <option key={a.id} value={a.id}>{a.nombre}</option>)}</select></div>}
      <div className="col-12"><RubroSelector vigenciaId={vigenciaId} value={rubroId} onChange={setRubroId} /></div>
      <div className="col-12"><label htmlFor="n-desc" className="form-label">Descripción del ítem</label><input id="n-desc" className="form-control" required maxLength={500} value={descripcion} onChange={e => setDescripcion(e.target.value)} placeholder="Ejemplo: Tubería PVC" /><div className="form-text">Escribe el bien o servicio específico que necesita la dependencia.</div></div>
      <div className="col-md-4"><label htmlFor="n-cantidad" className="form-label">Cantidad</label><input id="n-cantidad" className="form-control" type="number" inputMode="decimal" min="0.0001" step="0.0001" required value={cantidad} onChange={e => setCantidad(e.target.value)} /></div>
      <div className="col-md-4"><label htmlFor="n-unitario" className="form-label">Valor unitario</label><input id="n-unitario" className="form-control" type="number" inputMode="decimal" min="0" step="0.01" required value={unitario} onChange={e => setUnitario(e.target.value)} /><div className="form-text">Valor de una unidad en pesos.</div></div>
      <div className="col-md-4"><label className="form-label">Total calculado</label><div className="item-total" aria-live="polite"><strong>{money(total)}</strong></div><div className="form-text">Cantidad × valor unitario.</div></div>
      <div className="col-12"><DistributionEditor services={services} total={total} value={distribuciones} onChange={rows => { setDistribuciones(rows); setError('') }} /></div>
    </div></div></div>
    <div className="d-flex justify-content-end gap-2"><Link to={`/formulacion${areaId ? `?areaId=${areaId}` : ''}`} className="btn btn-outline-secondary">Cancelar</Link><button className="btn btn-primary" disabled={saving}>{saving ? 'Guardando…' : 'Guardar borrador'}</button></div></form></>
}
