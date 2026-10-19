import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { useVigencia } from '../context/VigenciaContext'
import { consolidadosService } from '../services/consolidados'
import type { ConsolidadoArea, ConsolidadoRubro, ConsolidadoServicio } from '../types/domain'
import { money } from '../utils/format'

type Row = ConsolidadoArea | ConsolidadoServicio | ConsolidadoRubro
const labels = { areas: 'Por dependencias', servicios: 'Por servicios', rubros: 'Consolidado por concepto' }
export function ConsolidadoPage() {
  const { kind } = useParams(); const { selected } = useVigencia()
  const [rows, setRows] = useState<Row[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState('')
  useEffect(() => {
    if (!selected || !kind || !(kind in labels)) { setLoading(false); return }
    let active = true; setLoading(true); setError(''); setRows([])
    const request = kind === 'areas' ? consolidadosService.byArea(selected.id) : kind === 'servicios' ? consolidadosService.byService(selected.id) : consolidadosService.byRubro(selected.id)
    request.then(value => { if (active) setRows(value) }).catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [selected?.id, kind])
  if (!kind || !(kind in labels)) return <Empty title="Consolidado no disponible" />
  const orderedRows = kind === 'rubros' ? (() => {
    const rubros = rows as ConsolidadoRubro[]
    const walk = (parent: number | null): ConsolidadoRubro[] => rubros.filter(r => r.rubroPadreId === parent).flatMap(r => [r, ...walk(r.rubroId)])
    return walk(null)
  })() : rows
  return <><PageTitle eyebrow="CONSOLIDADO" title={labels[kind as keyof typeof labels]}>{selected ? `${selected.anio} · ${selected.nombre}` : 'Seleccione una vigencia.'}</PageTitle>
    {error && <div className="alert alert-danger">{error}</div>}{loading ? <Loading label="Consultando consolidado…" /> : !rows.length ? <Empty title="No hay valores consolidados para esta vigencia" /> : <div className="card section-card border-0"><div className="table-responsive"><table className="table align-middle mb-0"><thead><tr><th>{kind === 'areas' ? 'Área' : kind === 'servicios' ? 'Servicio' : 'Concepto'}</th><th>Solicitado</th><th>Aprobado</th><th>Diferencia</th>{kind === 'areas' && <th></th>}</tr></thead><tbody>{orderedRows.map(row => {
      const key = 'areaId' in row ? row.areaId : 'servicioId' in row ? row.servicioId : row.rubroId
      const title = 'area' in row ? row.area : 'servicio' in row ? row.servicio : row.codigo.startsWith('FORM.CONCEPTO.') ? row.rubro : `${row.codigo} · ${row.rubro}`
      return <tr key={key}><td style={'nivel' in row ? { paddingLeft: `${1 + row.nivel * 1.4}rem` } : {}} className={'nivel' in row && row.nivel === 0 ? 'fw-bold' : ''}>{title}</td><td>{money(row.solicitado)}</td><td>{money(row.aprobado)}</td><td>{money(row.diferencia)}</td>{'areaId' in row && <td><Link to={`/formulacion?areaId=${row.areaId}`} className="btn btn-outline-primary btn-sm">Ver necesidades</Link></td>}</tr>
    })}</tbody></table></div></div>}</>
}
