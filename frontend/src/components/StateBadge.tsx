import type { EstadoNecesidad } from '../types/domain'
import { stateLabel, stateTone } from '../utils/format'

export function StateBadge({ state }: { state: EstadoNecesidad }) {
  return <span className={`badge text-bg-${stateTone[state]} status-badge`}>{stateLabel[state]}</span>
}
