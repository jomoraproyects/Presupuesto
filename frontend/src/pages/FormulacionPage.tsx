import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { StateBadge } from '../components/StateBadge'
import { useAuth } from '../context/AuthContext'
import { useVigencia } from '../context/VigenciaContext'
import { useToast } from '../context/ToastContext'
import { areasService, serviciosService } from '../services/catalogos'
import { formulacionService } from '../services/formulacion'
import type { Area, Formulacion, Necesidad, Servicio } from '../types/domain'
import { dateTime, money, number } from '../utils/format'

const normalized = (value: string) => value.toLocaleLowerCase('es').normalize('NFD').replace(/[\u0300-\u036f]/g, '')

function ItemCard({ item, form, services, isAdmin, canWrite, reviewOpen }: {
  item: Necesidad; form: Formulacion; services: Servicio[]; isAdmin: boolean; canWrite: boolean; reviewOpen: boolean
}) {
  const distributions = item.distribuciones.filter(d => d.porcentaje > 0 || (d.porcentajeAprobado ?? 0) > 0)
  const action = isAdmin
    ? reviewOpen ? form.estado === 'APROBADO' ? 'Revisar de nuevo' : 'Revisar / recortar' : 'Ver ítem'
    : canWrite && ['BORRADOR', 'DEVUELTA'].includes(item.estado) ? 'Editar ítem' : 'Ver ítem'
  return <article className="budget-item-card">
    <div className="budget-item-heading">
      <div><h3>{item.descripcion}</h3><StateBadge state={item.estado} /></div>
      <Link to={`/necesidades/${item.id}`} className="btn btn-outline-primary btn-sm">{action}<span aria-hidden="true"> →</span></Link>
    </div>
    <div className="budget-item-values">
      <div className="budget-value-panel"><span className="budget-value-label">Solicitado</span><strong className="budget-value-amount">{money(item.valorTotalSolicitado)}</strong><span className="budget-value-formula">{number(item.cantidadSolicitada)} × {money(item.valorUnitarioSolicitado)}</span></div>
      <div className="budget-value-panel budget-value-panel--approved"><span className="budget-value-label">Aprobado</span><strong className="budget-value-amount">{item.valorTotalAprobado == null ? 'Pendiente' : money(item.valorTotalAprobado)}</strong><span className="budget-value-formula">{item.valorTotalAprobado == null ? 'Sin ajuste todavía' : `${number(item.cantidadAprobada)} × ${money(item.valorUnitarioAprobado)}`}</span></div>
    </div>
    <div className="budget-item-distribution"><span className="budget-distribution-title">Distribución</span>
      {distributions.length === 0 ? <span className="budget-service-chip">General · 100 %</span> : distributions.map(d => {
        const label = services.find(service => service.id === d.servicioId)?.nombre ?? d.servicio
        return <span className="budget-service-chip" key={d.id}>{label}: {number(d.porcentaje)} % solicitado{item.valorTotalAprobado != null && ` · ${number(d.porcentajeAprobado ?? d.porcentaje)} % aprobado`}</span>
      })}
    </div>
  </article>
}

export function FormulacionPage() {
  const { user } = useAuth()
  const { selectedId, selected, loading: vigenciaLoading } = useVigencia()
  const { notify } = useToast()
  const [query, setQuery] = useSearchParams()
  const areaId = user?.rol === 'RESPONSABLE_AREA' ? user.areaId : Number(query.get('areaId')) || null
  const [areas, setAreas] = useState<Area[]>([])
  const [services, setServices] = useState<Servicio[]>([])
  const [form, setForm] = useState<Formulacion | null>(null)
  const [loading, setLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [confirmSend, setConfirmSend] = useState(false)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [selectedConceptId, setSelectedConceptId] = useState<number | null>(null)
  const [onlyWithItems, setOnlyWithItems] = useState(true)
  const isAdmin = user?.rol === 'SUPER_ADMIN'
  const canWrite = user?.rol === 'RESPONSABLE_AREA'
  const reviewOpen = Boolean(selected?.activa && ['ABIERTA', 'EN_REVISION'].includes(selected.estado))

  useEffect(() => {
    if (!canWrite) areasService.list().then(setAreas).catch(reason => setError(apiMessage(reason)))
  }, [canWrite])
  useEffect(() => { serviciosService.list().then(setServices).catch(reason => setError(apiMessage(reason))) }, [])
  useEffect(() => {
    setConfirmSend(false); setSelectedConceptId(null); setSearch('')
    if (!selectedId || !areaId) { setForm(null); return }
    let active = true; setLoading(true); setError(''); setForm(null)
    formulacionService.get(selectedId, areaId).then(value => { if (active) setForm(value) })
      .catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [selectedId, areaId])
  useEffect(() => {
    if (!form || !window.location.hash.startsWith('#rubro-')) return
    const id = Number(window.location.hash.slice(7))
    if (form.rubros.some(r => r.rubroId === id)) setSelectedConceptId(id)
  }, [form])

  async function sendForm() {
    if (!selectedId || !areaId) return
    setSending(true); setError('')
    try { setForm(await formulacionService.send(selectedId, areaId)); setConfirmSend(false); notify('Formulario enviado a Presupuesto.', 'success') }
    catch (reason) { setError(apiMessage(reason)) } finally { setSending(false) }
  }
  async function saveDraft() {
    if (!selectedId || !areaId) return
    setSending(true); setError('')
    try { setForm(await formulacionService.saveDraft(selectedId, areaId)); notify('Borrador guardado.', 'success') }
    catch (reason) { setError(apiMessage(reason)) } finally { setSending(false) }
  }
  async function approveForm() {
    if (!selectedId || !areaId) return
    setSending(true); setError('')
    try { setForm(await formulacionService.approve(selectedId, areaId)); notify('Presupuesto de la dependencia aprobado.', 'success') }
    catch (reason) { setError(apiMessage(reason)) } finally { setSending(false) }
  }
  function changeConcept(id: number | null) {
    setSelectedConceptId(id); setSearch('')
    document.getElementById('concept-switcher-anchor')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const concepts = form?.rubros ?? []
  const selectedIndex = selectedConceptId == null ? -1 : concepts.findIndex(r => r.rubroId === selectedConceptId)
  const visibleConcepts = selectedIndex >= 0 ? [concepts[selectedIndex]] : concepts.filter(r =>
    normalized(r.nombre).includes(normalized(search)) && (!isAdmin || !onlyWithItems || r.cantidadNecesidades > 0))
  return <>
    <PageTitle action={!canWrite ? <Link to="/presupuesto-detallado" className="btn btn-outline-secondary">Volver al Presupuesto detallado</Link> : undefined}
      eyebrow="FORMULACIÓN ANUAL" title={canWrite ? 'Mi formulario' : 'Ítems de la dependencia'}>
      {canWrite ? 'Agrega ítems dentro de cada concepto y guarda tu avance.' : 'Consulta solicitado y aprobado, y ajusta cada ítem desde esta misma vigencia.'}
    </PageTitle>
    {!canWrite && <div className="form-area-picker"><label htmlFor="form-area" className="form-label">Subgerencia / dependencia</label>
      <select id="form-area" className="form-select" value={areaId ?? ''} onChange={event => setQuery(event.target.value ? { areaId: event.target.value } : {})}>
        <option value="">Seleccione una dependencia…</option>{areas.map(a => <option key={a.id} value={a.id}>{a.nombre}{!a.activa && ' (inactiva)'}</option>)}
      </select></div>}
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    {loading || vigenciaLoading ? <Loading label="Cargando conceptos e ítems…" /> : !form
      ? <Empty title="Seleccione una vigencia y una dependencia" /> : <>
      {!form.puedeCrear && canWrite && <div className="alert alert-info">{form.enviada ? 'Formulario enviado a Presupuesto. Puedes consultar lo solicitado y lo aprobado.' : 'La vigencia no está habilitada para agregar ítems.'}</div>}
      {isAdmin && reviewOpen && !form.enviada && <div className="alert alert-info" role="status">{form.estado === 'BORRADOR' ? 'Esta dependencia todavía diligencia su formulario.' : 'Esta dependencia aún no tiene ítems.'} Puedes revisar y recortar los ítems que ya existan; lo solicitado podrá cambiar hasta que envíe el formulario.</div>}
      {isAdmin && reviewOpen && form.enviada && form.estado !== 'APROBADO' && <div className="alert alert-success" role="status">Formulario recibido. Abre «Revisar / recortar» en cualquier ítem. Los totales aprobados se actualizan automáticamente.</div>}
      {isAdmin && reviewOpen && form.estado === 'APROBADO' && <div className="alert alert-info" role="status">Presupuesto aprobado. Puedes abrir cualquier ítem para ajustar de nuevo lo aprobado. Al guardar, la dependencia volverá a revisión hasta que la apruebes otra vez.</div>}
      <div className="budget-form-summary"><div><span className="budget-summary-kicker">Presupuesto {selected?.anio} · {form.estado.replaceAll('_', ' ')}</span><strong>{form.area}</strong>{form.fechaEnvio && <small>Enviado el {dateTime(form.fechaEnvio)} por {form.enviadoPor}</small>}</div>
        <div className="budget-summary-totals"><span>Solicitado <strong>{money(form.solicitado)}</strong></span><span>Aprobado <strong>{money(form.aprobado)}</strong></span></div></div>
      {concepts.length === 0 ? <Empty title="No hay conceptos configurados" /> : <>
        <div id="concept-switcher-anchor" />
        <div id="concept-switcher" className="concept-switcher">
          <div className="concept-switcher-title"><div><strong>Explorar conceptos</strong><small>Cambia de concepto aquí, sin salir del formulario.</small></div><span>{concepts.length} conceptos</span></div>
          <div className="concept-switcher-controls">
            <div className="concept-picker"><label htmlFor="concept-select" className="form-label">Concepto</label><select id="concept-select" className="form-select" value={selectedConceptId ?? ''} onChange={event => changeConcept(event.target.value ? Number(event.target.value) : null)}>
              <option value="">Todos los conceptos</option>{concepts.map(r => <option key={r.rubroId} value={r.rubroId}>{r.orden}. {r.nombre} ({r.cantidadNecesidades})</option>)}
            </select></div>
            <div className="concept-search"><label htmlFor="form-search" className="form-label">Buscar</label><div className="concept-search-input"><Icon name="search" size={18} /><input id="form-search" className="form-control" placeholder="Escribe un concepto…" value={search} onChange={event => { setSearch(event.target.value); setSelectedConceptId(null) }} /></div></div>
            <div className="concept-stepper" aria-label="Navegar por conceptos"><button className="btn btn-outline-secondary" type="button" disabled={selectedIndex <= 0} onClick={() => changeConcept(concepts[selectedIndex - 1].rubroId)} aria-label="Concepto anterior"><Icon name="chevronLeft" /></button><button className="btn btn-outline-secondary" type="button" disabled={selectedIndex < 0 || selectedIndex >= concepts.length - 1} onClick={() => changeConcept(concepts[selectedIndex + 1].rubroId)} aria-label="Concepto siguiente"><Icon name="chevronRight" /></button></div>
          </div>
          {isAdmin && <label className="form-check mt-3 mb-0"><input className="form-check-input" type="checkbox" checked={onlyWithItems} onChange={event => { setOnlyWithItems(event.target.checked); setSelectedConceptId(null) }} /><span>Mostrar solo conceptos con ítems</span></label>}
        </div>
        {visibleConcepts.length === 0 && <Empty title={search ? 'No encontramos conceptos con ese nombre' : 'No hay conceptos con ítems; desactiva el filtro para verlos todos'} />}
        {visibleConcepts.map(r => <section id={`rubro-${r.rubroId}`} key={r.rubroId} className="card section-card concept-review-card border-0 mb-3"><div className="card-body">
          <div className="concept-review-heading"><div><span className="concept-order">Concepto {r.orden}</span><h2>{r.nombre}</h2><span className="text-muted">{r.cantidadNecesidades} {r.cantidadNecesidades === 1 ? 'ítem' : 'ítems'}</span></div>
            {canWrite && form.puedeCrear && r.habilitado && <Link className="btn btn-primary" to={`/necesidades/nueva?rubroId=${r.rubroId}&areaId=${form.areaId}&vigenciaId=${form.vigenciaId}`}>+ Añadir ítem</Link>}</div>
          <div className="concept-review-totals"><span>Solicitado <strong>{money(r.solicitado)}</strong></span><span>Aprobado <strong>{money(r.aprobado)}</strong></span><span>Diferencia <strong>{money(r.diferencia)}</strong></span></div>
          {r.necesidades.length ? <div className="budget-items">{r.necesidades.map(item => <ItemCard key={item.id} item={item} form={form} services={services} isAdmin={isAdmin} canWrite={canWrite} reviewOpen={reviewOpen} />)}</div>
            : <p className="text-muted mb-0">Este concepto aún no tiene ítems.</p>}
        </div></section>)}
      </>}
      <div className="budget-form-summary mb-3"><div><span className="budget-summary-kicker">Total de la dependencia</span><strong>{form.area}</strong></div><div className="budget-summary-totals"><span>Solicitado <strong>{money(form.solicitado)}</strong></span><span>Aprobado <strong>{money(form.aprobado)}</strong></span></div></div>
      {canWrite && form.puedeCrear && <div className="d-flex flex-wrap gap-2 mb-3"><button className="btn btn-outline-primary" disabled={sending} onClick={() => void saveDraft()}>Guardar borrador</button><button className="btn btn-primary" disabled={sending} onClick={() => setConfirmSend(true)}>Enviar a Presupuesto</button></div>}
      {confirmSend && <div className="alert alert-warning mb-3">Se enviará el formulario completo de {form.area}. La dependencia ya no podrá editarlo.<div className="d-flex gap-2 mt-2"><button className="btn btn-primary btn-sm" disabled={sending} onClick={() => void sendForm()}>{sending ? 'Enviando…' : 'Confirmar envío'}</button><button className="btn btn-outline-secondary btn-sm" onClick={() => setConfirmSend(false)}>Cancelar</button></div></div>}
      {isAdmin && reviewOpen && form.enviada && form.estado !== 'APROBADO' && <button className="btn btn-primary mb-3" disabled={sending} onClick={() => void approveForm()}>{sending ? 'Aprobando…' : 'Aprobar presupuesto de la dependencia'}</button>}
    </>}
  </>
}
