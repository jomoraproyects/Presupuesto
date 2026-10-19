import type { EstadoNecesidad, Role } from '../types/domain'

const moneyFormatter = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 })
const numberFormatter = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 4 })

export const money = (value: number | null | undefined) => value == null ? '—' : moneyFormatter.format(value)
export const number = (value: number | null | undefined) => value == null ? '—' : numberFormatter.format(value)
export const dateTime = (value: string) => new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

export const roleLabel: Record<Role, string> = {
  SUPER_ADMIN: 'Administrador de presupuesto', RESPONSABLE_AREA: 'Responsable de área', CONSULTA: 'Consulta',
}

export const stateLabel: Record<EstadoNecesidad, string> = {
  BORRADOR: 'Borrador', ENVIADA: 'Enviada', EN_REVISION: 'En revisión', APROBADA: 'Aprobada',
  APROBADA_CON_AJUSTES: 'Aprobada con ajustes', RECHAZADA: 'Rechazada', DEVUELTA: 'Devuelta',
}

export const stateTone: Record<EstadoNecesidad, string> = {
  BORRADOR: 'secondary', ENVIADA: 'info', EN_REVISION: 'warning', APROBADA: 'success',
  APROBADA_CON_AJUSTES: 'primary', RECHAZADA: 'danger', DEVUELTA: 'warning',
}
