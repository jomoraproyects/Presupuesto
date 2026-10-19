import { useEffect, useState } from 'react'
import { conceptosService, rubrosService } from '../services/catalogos'
import type { Rubro } from '../types/domain'
import { apiMessage } from '../api/client'
import { Loading } from './Feedback'

export function RubroSelector({ vigenciaId, value, onChange }: {
  vigenciaId: number | null; value: number | null; onChange: (id: number) => void
}) {
  const [rows, setRows] = useState<Rubro[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true; setLoading(true)
    Promise.all([conceptosService.list(vigenciaId), value ? rubrosService.get(value) : Promise.resolve(null)])
      .then(([catalog, selected]) => {
        if (!active) return
        const available = catalog.filter(r => r.activo)
        // Conservar la categoría de solicitudes históricas sin ofrecer las plantillas anteriores.
        if (selected && !available.some(r => r.id === selected.id)) available.push(selected)
        setRows(available.sort((a, b) => a.orden - b.orden || a.id - b.id)); setError('')
      }).catch(reason => { if (active) setError(apiMessage(reason)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [vigenciaId, value])
  return <div>
    <label htmlFor="n-concepto" className="form-label">Concepto</label>
    {loading ? <Loading label="Cargando conceptos…" /> : error ? <div className="text-danger">{error}</div> :
      <select id="n-concepto" className="form-select" required value={value ?? ''} onChange={e => onChange(Number(e.target.value))}>
        <option value="">Seleccione un concepto…</option>
        {rows.map(r => <option key={r.id} value={r.id}>{r.orden}. {r.nombre}{!r.activo && ' (inactivo)'}</option>)}
      </select>}
  </div>
}
