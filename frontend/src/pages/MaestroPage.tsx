import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { useAuth } from '../context/AuthContext'
import { useVigencia } from '../context/VigenciaContext'
import { formulacionService } from '../services/formulacion'
import type { Maestro, MaestroRubro } from '../types/domain'
import { money } from '../utils/format'

export function MaestroPage() {
  const { selectedId, selected, loading: vigenciaLoading } = useVigencia(); const { user } = useAuth()
  const [data, setData] = useState<Maestro | null>(null); const [loading, setLoading] = useState(false); const [error, setError] = useState('')
  useEffect(() => {
    if (!selectedId) { setData(null); return }
    let active = true; setLoading(true); setError(''); setData(null)
    formulacionService.maestro(selectedId).then(value => { if (active) setData(value) })
      .catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [selectedId])
  const render = (node: MaestroRubro): ReactNode => <>
    <tr key={node.rubroId}><td style={{ paddingLeft: `${1 + node.nivel * 1.4}rem` }} className={node.hijos.length ? 'fw-bold' : ''}>{node.codigo} · {node.nombre}</td><td>{money(node.solicitado)}</td><td>{money(node.aprobado)}</td><td>{money(node.diferencia)}</td></tr>
    {node.hijos.length > 0 && (node.solicitadoGeneral > 0 || node.aprobadoGeneral > 0) && <tr><td style={{ paddingLeft: `${1 + (node.nivel + 1) * 1.4}rem` }}>Valor general sin asignar a servicios</td><td>{money(node.solicitadoGeneral)}</td><td>{money(node.aprobadoGeneral)}</td><td>{money(node.aprobadoGeneral - node.solicitadoGeneral)}</td></tr>}
    {node.hijos.map(child => <Rows key={child.rubroId} node={child} />)}
  </>
  function Rows({ node }: { node: MaestroRubro }) { return render(node) }
  return <><PageTitle eyebrow="CONSOLIDADO MAESTRO" title="Estructura del presupuesto final">{selected ? `${selected.anio} · ${selected.nombre}` : 'Seleccione una vigencia.'}</PageTitle>
    {error && <div className="alert alert-danger">{error}</div>}{loading || vigenciaLoading ? <Loading label="Calculando maestro…" /> : !data ? <Empty title="No hay vigencia seleccionada" /> : <>
      <div className="total-preview mb-3"><span>Solicitado {money(data.totalSolicitado)}</span><strong>Aprobado {money(data.totalAprobado)}</strong></div>
      {!data.mapeoCompleto && <div className="alert alert-warning"><strong>Hay mapeos pendientes.</strong> Los importes permanecen en su categoría de origen hasta configurar el destino por servicio. {user?.rol !== 'CONSULTA' && <Link to="/configuracion-vigencia">Configurar mapeos</Link>}
        <ul className="mb-0 mt-2">{data.mapeosPendientes.map(p => <li key={`${p.rubroOrigenId}-${p.servicioId}`}>{p.rubroOrigen} · {p.servicio}: aprobado {money(p.aprobado)}</li>)}</ul></div>}
      <div className="card section-card border-0"><div className="table-responsive"><table className="table mb-0"><thead><tr><th>Código / Rubro</th><th>Solicitado</th><th>Aprobado</th><th>Diferencia</th></tr></thead><tbody>{data.rubros.map(node => <Rows key={node.rubroId} node={node} />)}</tbody></table></div></div>
    </>}</>
}
