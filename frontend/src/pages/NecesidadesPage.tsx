import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { StateBadge } from '../components/StateBadge'
import { useAuth } from '../context/AuthContext'
import { useVigencia } from '../context/VigenciaContext'
import { areasService, rubrosService } from '../services/catalogos'
import { necesidadesService } from '../services/necesidades'
import type { Area, EstadoNecesidad, Necesidad, Pagina, Rubro } from '../types/domain'
import { money, number, stateLabel } from '../utils/format'

const states: EstadoNecesidad[] = ['BORRADOR', 'ENVIADA', 'EN_REVISION', 'APROBADA', 'APROBADA_CON_AJUSTES', 'RECHAZADA', 'DEVUELTA']
export function NecesidadesPage() {
  const { user } = useAuth(); const { selectedId, vigencias } = useVigencia()
  const [search, setSearch] = useSearchParams()
  const [rows, setRows] = useState<Pagina<Necesidad> | null>(null)
  const [areas, setAreas] = useState<Area[]>([]); const [rubros, setRubros] = useState<Rubro[]>([])
  const [loading, setLoading] = useState(true); const [error, setError] = useState('')
  const filter = useMemo(() => ({
    vigenciaId: Number(search.get('vigenciaId') || selectedId) || undefined,
    areaId: Number(search.get('areaId')) || undefined, rubroId: Number(search.get('rubroId')) || undefined,
    estado: (search.get('estado') || '') as EstadoNecesidad | '', texto: search.get('texto') || '',
    page: Number(search.get('page')) || 0, size: 10,
  }), [search, selectedId])
  useEffect(() => {
    if (!user) return
    Promise.all([areasService.list(), rubrosService.list()]).then(([a, r]) => { setAreas(a); setRubros(r) }).catch(() => {})
  }, [user])
  useEffect(() => {
    let active = true; setLoading(true); setError('')
    necesidadesService.list(filter).then(value => { if (active) setRows(value) })
      .catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [filter])
  const set = (key: string, value: string) => { const next = new URLSearchParams(search); if (value) next.set(key, value); else next.delete(key); next.delete('page'); setSearch(next) }
  const canWrite = user?.rol === 'RESPONSABLE_AREA' || user?.rol === 'SUPER_ADMIN'
  return <><PageTitle eyebrow="GESTIÓN DE SOLICITUDES" title={user?.rol === 'RESPONSABLE_AREA' ? 'Mis necesidades' : 'Necesidades'} action={canWrite && <Link to="/necesidades/nueva" className="btn btn-primary">Nueva necesidad</Link>}>Consulte, filtre y abra cada solicitud.</PageTitle>
    <div className="card section-card border-0 mb-3"><div className="card-body"><div className="row g-3">
      <div className="col-md-3 col-xl-2"><label className="form-label small" htmlFor="f-vigencia">Vigencia</label><select id="f-vigencia" className="form-select form-select-sm" value={filter.vigenciaId ?? ''} onChange={e => set('vigenciaId', e.target.value)}><option value="">Todas</option>{vigencias.map(v => <option key={v.id} value={v.id}>{v.anio} · {v.nombre}</option>)}</select></div>
      <div className="col-md-3 col-xl-2"><label className="form-label small" htmlFor="f-estado">Estado</label><select id="f-estado" className="form-select form-select-sm" value={filter.estado} onChange={e => set('estado', e.target.value)}><option value="">Todos</option>{states.map(s => <option key={s} value={s}>{stateLabel[s]}</option>)}</select></div>
      {user?.rol !== 'RESPONSABLE_AREA' && <div className="col-md-3 col-xl-2"><label className="form-label small" htmlFor="f-area">Área</label><select id="f-area" className="form-select form-select-sm" value={filter.areaId ?? ''} onChange={e => set('areaId', e.target.value)}><option value="">Todas</option>{areas.map(a => <option key={a.id} value={a.id}>{a.nombre}</option>)}</select></div>}
      {user?.rol !== 'RESPONSABLE_AREA' && <div className="col-md-3 col-xl-2"><label className="form-label small" htmlFor="f-rubro">Rubro</label><select id="f-rubro" className="form-select form-select-sm" value={filter.rubroId ?? ''} onChange={e => set('rubroId', e.target.value)}><option value="">Todos</option>{rubros.map(r => <option key={r.id} value={r.id}>{r.codigo} · {r.nombre}</option>)}</select></div>}
      <div className="col-md-6 col-xl"><label className="form-label small" htmlFor="f-texto">Buscar</label><input id="f-texto" className="form-control form-control-sm" placeholder="Descripción o justificación" value={filter.texto} onChange={e => set('texto', e.target.value)} /></div>
    </div></div></div>
    {error && <div className="alert alert-danger">{error}</div>}{loading ? <Loading /> : !rows?.content.length ? <Empty title="No hay necesidades con estos filtros" /> : <div className="card section-card border-0"><div className="table-responsive"><table className="table table-hover align-middle mb-0"><thead><tr>{user?.rol !== 'RESPONSABLE_AREA' && <th>Área</th>}<th>Descripción</th><th>Rubro</th><th>Cantidad</th><th>Solicitado</th><th>Aprobado</th>{user?.rol !== 'RESPONSABLE_AREA' && <th>Diferencia</th>}<th>Estado</th><th></th></tr></thead><tbody>{rows.content.map(item => <tr key={item.id}>{user?.rol !== 'RESPONSABLE_AREA' && <td>{areas.find(a => a.id === item.areaId)?.nombre ?? `Área ${item.areaId}`}</td>}<td className="fw-medium">{item.descripcion}</td><td>{rubros.find(r => r.id === item.rubroPresupuestalId)?.codigo ?? item.rubroPresupuestalId}</td><td>{number(item.cantidadSolicitada)}</td><td>{money(item.valorTotalSolicitado)}</td><td>{money(item.valorTotalAprobado)}</td>{user?.rol !== 'RESPONSABLE_AREA' && <td>{money((item.valorTotalAprobado ?? 0) - item.valorTotalSolicitado)}</td>}<td><StateBadge state={item.estado} /></td><td><Link to={`/necesidades/${item.id}`} className="btn btn-outline-primary btn-sm">Ver</Link></td></tr>)}</tbody></table></div>
      <div className="card-body border-top d-flex justify-content-between align-items-center"><span className="small text-muted">{rows.totalElements} resultados · Página {rows.page + 1} de {rows.totalPages}</span><div className="btn-group"><button className="btn btn-outline-secondary btn-sm" disabled={rows.page === 0} onClick={() => { const next = new URLSearchParams(search); next.set('page', String(rows.page - 1)); setSearch(next) }}>Anterior</button><button className="btn btn-outline-secondary btn-sm" disabled={rows.page + 1 >= rows.totalPages} onClick={() => { const next = new URLSearchParams(search); next.set('page', String(rows.page + 1)); setSearch(next) }}>Siguiente</button></div></div></div>}</>
}
