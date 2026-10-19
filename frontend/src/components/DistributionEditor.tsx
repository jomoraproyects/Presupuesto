import type { DistribucionInput, Servicio } from '../types/domain'
import { money, number } from '../utils/format'

const labels: Record<string, string> = { '21': 'Acueducto', '22': 'Alcantarillado', '23': 'Aseo' }

export function DistributionEditor({ services, total, value, onChange }: {
  services: Servicio[]; total: number; value: DistribucionInput[]; onChange: (rows: DistribucionInput[]) => void
}) {
  const rows = services.filter(service => ['21', '22', '23'].includes(service.codigo) &&
    (service.activo || value.some(row => row.servicioId === service.id)))
    .map(service => ({ service, selected: value.find(row => row.servicioId === service.id) }))
  const totalBasisPoints = value.reduce((sum, row) => sum + Math.round(row.porcentaje * 100), 0)
  const valid = value.length > 0 && totalBasisPoints === 10000 && value.every(row => row.porcentaje >= 0 && row.porcentaje <= 100)
  function toggle(id: number, checked: boolean) {
    const next = checked ? [...value, { servicioId: id, porcentaje: 0 }] : value.filter(row => row.servicioId !== id)
    // Un servicio recibe el 100 %. Al agregar otro, el usuario decide el reparto.
    onChange(next.length === 1 ? [{ ...next[0], porcentaje: 100 }] : next)
  }
  const change = (id: number, percentage: number) => onChange(value.map(row =>
    row.servicioId === id ? { ...row, porcentaje: percentage } : row))

  // Vista previa en centavos, con el mismo reparto de restos que el backend.
  const cents = Math.round(total * 100)
  const shares = value.map(row => {
    const exact = Number.isSafeInteger(cents) && Number.isFinite(row.porcentaje)
      ? BigInt(cents) * BigInt(Math.round(row.porcentaje * 100)) : 0n
    return { id: row.servicioId, cents: Number(exact / 10000n), remainder: Number(exact % 10000n) }
  })
  if (valid && Number.isSafeInteger(cents) && cents >= 0) {
    const remaining = cents - shares.reduce((sum, share) => sum + share.cents, 0)
    const ordered = [...shares].sort((a, b) => b.remainder - a.remainder || a.id - b.id)
    for (let i = 0; i < remaining; i++) ordered[i % ordered.length].cents += 1
  }
  return <fieldset className="distribution-editor">
    <legend className="form-label mb-2">Servicios opcionales</legend>
    <div className="distribution-options">
      {rows.map(({ service, selected }) => {
        const name = labels[service.codigo] ?? service.nombre
        return <div className={`distribution-option ${selected ? 'is-selected' : ''}`} key={service.id}>
          <label className="form-check mb-0"><input className="form-check-input" type="checkbox"
            checked={Boolean(selected)} disabled={!service.activo && !selected}
            onChange={event => toggle(service.id, event.target.checked)} /><span>{name}</span></label>
          {selected && <div className="mt-2">
            {value.length === 1 ? <span className="small">100 % automático</span> :
              <div className="input-group input-group-sm"><input type="number" min="0" max="100" step="0.01"
                className="form-control" required aria-label={`Porcentaje de ${name}`} value={selected.porcentaje}
                onChange={event => change(service.id, Number(event.target.value))} /><span className="input-group-text">%</span></div>}
            <div className="small fw-semibold mt-1">{money((shares.find(share => share.id === service.id)?.cents ?? 0) / 100)}</div>
          </div>}
        </div>
      })}
    </div>
    {value.length === 0 ? <p className="form-text mb-0">Sin servicios seleccionados: todo queda en el concepto general.</p> :
      <p className={`small mb-0 mt-2 ${valid ? 'text-success' : 'text-danger'}`} role={valid ? undefined : 'alert'}>
        {valid ? `Distribuido: ${money(cents / 100)} · 100 %` : `La suma debe ser 100 %. Actual: ${number(totalBasisPoints / 100)} %.`}
      </p>}
  </fieldset>
}
