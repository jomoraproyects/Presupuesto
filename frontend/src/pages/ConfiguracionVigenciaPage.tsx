import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { useVigencia } from '../context/VigenciaContext'
import { rubrosService, serviciosService } from '../services/catalogos'
import { configuracionService } from '../services/formulacion'
import type { Mapeo, Participante, Rubro, Servicio } from '../types/domain'

export function ConfiguracionVigenciaPage() {
  const { user } = useAuth(); const { selectedId, selected, loading: vigenciaLoading } = useVigencia(); const { notify } = useToast()
  const [participants, setParticipants] = useState<Participante[]>([]); const [maps, setMaps] = useState<Mapeo[]>([])
  const [rubros, setRubros] = useState<Rubro[]>([]); const [services, setServices] = useState<Servicio[]>([])
  const [loading, setLoading] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  const [editId, setEditId] = useState<number | null>(null); const [origin, setOrigin] = useState(''); const [service, setService] = useState(''); const [destination, setDestination] = useState('')
  const load = useCallback(async () => {
    if (!selectedId) return
    const [p, m, r, s] = await Promise.all([configuracionService.participantes(selectedId), configuracionService.mapeos(selectedId), rubrosService.list(), serviciosService.list()])
    setParticipants(p); setMaps(m); setRubros(r); setServices(s)
  }, [selectedId])
  useEffect(() => {
    let active = true; setLoading(true); setError(''); setEditId(null); setOrigin(''); setService(''); setDestination('')
    load().catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [load])
  const mutate = async (work: () => Promise<unknown>) => {
    setBusy(true); setError('')
    try { await work(); await load(); notify('Configuración actualizada.', 'success') }
    catch (reason) { setError(apiMessage(reason)) } finally { setBusy(false) }
  }
  function submit(event: FormEvent) {
    event.preventDefault(); if (!selectedId) return
    void mutate(async () => { await configuracionService.saveMapeo(selectedId, { rubroOrigenId: Number(origin), servicioId: Number(service), rubroDestinoId: Number(destination) }, editId ?? undefined); setEditId(null); setOrigin(''); setService(''); setDestination('') })
  }
  if (loading || vigenciaLoading) return <Loading label="Cargando configuración anual…" />
  if (!selectedId || !selected) return <Empty title="Seleccione una vigencia" />
  const parents = new Set(rubros.filter(r => r.activo).map(r => r.rubroPadreId))
  const codes = new Set(rubros.filter(r => r.vigenciaId === selectedId).map(r => r.codigo))
  const available = rubros.filter(r => r.activo && (r.vigenciaId === selectedId || (r.vigenciaId === null && !codes.has(r.codigo))) && !parents.has(r.id))
  const canParticipants = user?.rol === 'SUPER_ADMIN' && ['BORRADOR', 'ABIERTA'].includes(selected.estado) && selected.activa
  const canMaps = ['SUPER_ADMIN'].includes(user?.rol ?? '') && ['BORRADOR', 'ABIERTA', 'EN_REVISION'].includes(selected.estado) && selected.activa
  return <><PageTitle eyebrow="CONFIGURACIÓN ANUAL" title={`Participantes y mapeos · ${selected.anio}`}>{selected.nombre}</PageTitle>{error && <div className="alert alert-danger" role="alert">{error}</div>}
    <section className="card section-card border-0 mb-3"><div className="card-body"><div className="d-flex justify-content-between gap-3"><h2 className="h5">Áreas participantes</h2>{user?.rol === 'SUPER_ADMIN' && <Link to="/admin/areas" className="btn btn-outline-primary btn-sm">Gestionar áreas</Link>}</div>
      <p className="small text-muted">Habilite las áreas que deben presentar presupuesto. Deshabilitar bloquea nuevos ítems y conserva las solicitudes existentes.</p>
      {participants.length === 0 ? <Empty title="No hay áreas configuradas" /> : <div className="row g-3">{participants.map(p => <div className="col-md-6 col-xl-4" key={p.areaId}><label className="form-check"><input className="form-check-input" type="checkbox" checked={p.participante} disabled={busy || !canParticipants || (!p.areaActiva && !p.participante)} onChange={e => void mutate(() => configuracionService.participante(selectedId, p.areaId, e.target.checked))} /><span>{p.area}{!p.areaActiva && ' (área inactiva)'}</span></label></div>)}</div>}
    </div></section>
    <section className="card section-card border-0"><div className="card-body"><h2 className="h5">Destino por categoría y servicio</h2><p className="small text-muted">Seleccione el rubro final que recibirá los valores distribuidos. Los destinos se configuran como rubros excluidos del formulario.</p>
      {canMaps && <form onSubmit={submit} className="mb-3"><div className="row g-3"><div className="col-md-4"><label htmlFor="map-origin" className="form-label">Categoría de origen</label><select id="map-origin" className="form-select" required value={origin} onChange={e => setOrigin(e.target.value)}><option value="">Seleccione…</option>{available.filter(r => r.permiteMovimiento && r.categoriaFormulacion).map(r => <option key={r.id} value={r.id}>{r.nombre} · {r.codigo}</option>)}</select></div><div className="col-md-3"><label htmlFor="map-service" className="form-label">Servicio</label><select id="map-service" className="form-select" required value={service} onChange={e => setService(e.target.value)}><option value="">Seleccione…</option>{services.filter(s => s.activo).map(s => <option key={s.id} value={s.id}>{s.nombre}</option>)}</select></div><div className="col-md-5"><label htmlFor="map-destination" className="form-label">Rubro final / código maestro</label><select id="map-destination" className="form-select" required value={destination} onChange={e => setDestination(e.target.value)}><option value="">Seleccione…</option>{available.filter(r => !r.categoriaFormulacion).map(r => <option key={r.id} value={r.id}>{r.codigo} · {r.nombre}</option>)}</select></div><div className="col-12 d-flex gap-2"><button className="btn btn-primary" disabled={busy}>{editId ? 'Guardar mapeo' : 'Crear mapeo'}</button>{editId && <button type="button" className="btn btn-outline-secondary" onClick={() => { setEditId(null); setOrigin(''); setService(''); setDestination('') }}>Cancelar edición</button>}</div></div></form>}
      {maps.length === 0 ? <p className="text-muted mb-0">Aún no hay mapeos configurados.</p> : <div className="table-responsive"><table className="table align-middle mb-0"><thead><tr><th>Categoría</th><th>Servicio</th><th>Destino</th><th>Activo</th>{canMaps && <th></th>}</tr></thead><tbody>{maps.map(m => <tr key={m.id}><td>{m.rubroOrigen}</td><td>{m.servicio}</td><td>{m.codigoDestino} · {m.rubroDestino}</td><td>{m.activo ? 'Sí' : 'No'}</td>{canMaps && <td><div className="d-flex gap-2"><button className="btn btn-outline-primary btn-sm" onClick={() => { setEditId(m.id); setOrigin(String(m.rubroOrigenId)); setService(String(m.servicioId)); setDestination(String(m.rubroDestinoId)) }}>Editar</button><button className="btn btn-outline-secondary btn-sm" disabled={busy} onClick={() => void mutate(() => configuracionService.mapeoEstado(selectedId, m.id, !m.activo))}>{m.activo ? 'Desactivar' : 'Reactivar'}</button></div></td>}</tr>)}</tbody></table></div>}
    </div></section></>
}
