import type { EstadoFormulario } from '../types/domain'

export const responseState: Record<EstadoFormulario, { label: string; action: string; tone: string }> = {
  PENDIENTE: { label: 'Falta por responder', action: 'Ver formulario', tone: 'pending' },
  BORRADOR: { label: 'En elaboración', action: 'Revisar borrador', tone: 'draft' },
  ENVIADO: { label: 'Respondido', action: 'Revisar respuesta', tone: 'sent' },
  EN_REVISION: { label: 'En revisión', action: 'Continuar revisión', tone: 'review' },
  APROBADO: { label: 'Aprobado', action: 'Ver aprobado', tone: 'approved' },
}
