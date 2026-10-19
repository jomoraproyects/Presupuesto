export type Role = 'SUPER_ADMIN' | 'RESPONSABLE_AREA' | 'CONSULTA'
export type EstadoVigencia = 'BORRADOR' | 'ABIERTA' | 'EN_REVISION' | 'APROBADA' | 'CERRADA'
export type EstadoNecesidad = 'BORRADOR' | 'ENVIADA' | 'EN_REVISION' | 'APROBADA' | 'APROBADA_CON_AJUSTES' | 'RECHAZADA' | 'DEVUELTA'

export interface Perfil {
  id: number
  username: string
  nombre: string
  rol: Role
  areaId: number | null
}

export interface Area {
  id: number
  codigo: string
  nombre: string
  descripcion: string | null
  activa: boolean
  areaPadreId: number | null
}

export interface Servicio {
  id: number
  codigo: string
  nombre: string
  descripcion: string | null
  activo: boolean
}

export interface Vigencia {
  id: number
  anio: number
  nombre: string
  fechaInicio: string
  fechaFin: string
  presupuestoDisponible: number
  estado: EstadoVigencia
  activa: boolean
}

export interface Rubro {
  id: number
  codigo: string
  nombre: string
  descripcion: string | null
  rubroPadreId: number | null
  nivel: number
  permiteMovimiento: boolean
  categoriaFormulacion: boolean
  activo: boolean
  vigenciaId: number | null
  orden: number
}

export type RubroNodo = Pick<Rubro, 'id' | 'codigo' | 'nombre' | 'descripcion' | 'nivel' | 'permiteMovimiento' | 'categoriaFormulacion' | 'vigenciaId' | 'orden'> & {
  hijos: RubroNodo[]
}

export interface Distribucion {
  id: number
  servicioId: number
  servicio: string
  porcentaje: number
  valorSolicitadoDistribuido: number
  valorAprobadoDistribuido: number
  porcentajeAprobado: number | null
}

export interface DistribucionInput { servicioId: number; porcentaje: number }

export interface Necesidad {
  id: number
  vigenciaId: number
  areaId: number
  rubroPresupuestalId: number
  descripcion: string
  justificacion: string
  unidadMedida: string
  cantidadSolicitada: number
  valorUnitarioSolicitado: number
  valorTotalSolicitado: number
  cantidadSolicitadaOriginal: number | null
  valorUnitarioSolicitadoOriginal: number | null
  valorTotalSolicitadoOriginal: number | null
  cantidadAprobada: number | null
  valorUnitarioAprobado: number | null
  valorTotalAprobado: number | null
  estado: EstadoNecesidad
  observacionPresupuesto: string | null
  creadoPorId: number
  revisadoPorId: number | null
  activa: boolean
  version: number
  distribuciones: Distribucion[]
  aplicaDistribucion: boolean
  fechaCreacion: string
  fechaActualizacion: string
}

export interface NecesidadInput {
  vigenciaId: number
  areaId?: number
  rubroPresupuestalId: number
  descripcion: string
  justificacion?: string
  unidadMedida?: string
  cantidadSolicitada: number
  valorUnitarioSolicitado: number
  distribuciones: DistribucionInput[]
  aplicaDistribucion: boolean
}

export interface AjusteInput {
  cantidadAprobada?: number
  valorUnitarioAprobado?: number
  observacionPresupuesto?: string
  distribucionesAprobadas?: DistribucionInput[]
}

export interface Pagina<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Historial {
  id: number
  usuarioId: number
  accion: string
  descripcion: string
  estadoAnterior: EstadoNecesidad | null
  estadoNuevo: EstadoNecesidad
  valorSolicitado: number
  valorAprobadoAnterior: number | null
  valorAprobadoNuevo: number | null
  detalleAnterior: string | null
  detalleNuevo: string
  fecha: string
  usuario: string
  cantidadAnterior: number | null
  cantidadNueva: number | null
  valorUnitarioAnterior: number | null
  valorUnitarioNuevo: number | null
  motivo: string | null
}

export interface ResumenPresupuesto {
  vigenciaId: number
  presupuestoDisponible: number
  totalSolicitado: number
  totalAprobado: number
  diferenciaContraTecho: number
}

export interface ResumenArea {
  vigenciaId: number
  areaId: number
  area: string
  totalSolicitado: number
  totalAprobado: number
  diferencia: number
  numeroNecesidades: number
  porEstado: Record<EstadoNecesidad, number>
}

export type EstadoFormulario = 'PENDIENTE' | 'BORRADOR' | 'ENVIADO' | 'EN_REVISION' | 'APROBADO'
export interface ConsolidadoArea { areaId: number; area: string; solicitado: number; aprobado: number; diferencia: number; enviada: boolean; estado: EstadoFormulario }
export interface PresupuestoGeneralConcepto { conceptoId: number; nombre: string; orden: number; solicitado: number; aprobado: number; diferencia: number; dependencias: { areaId: number; area: string; solicitado: number; aprobado: number; diferencia: number }[] }
export interface ConsolidadoServicio { servicioId: number; servicio: string; solicitado: number; aprobado: number; diferencia: number }
export interface ConsolidadoRubro { rubroId: number; rubroPadreId: number | null; codigo: string; rubro: string; nivel: number; solicitado: number; aprobado: number; diferencia: number }

export interface Usuario {
  id: number
  username: string
  nombre: string
  rol: Role
  areaId: number | null
  activo: boolean
}

export interface UsuarioCreateInput { username: string; password: string; nombre: string; rol: Role; areaId?: number }
export interface UsuarioUpdateInput { nombre: string; rol: Role; areaId?: number }
export interface AreaInput { codigo: string; nombre: string; descripcion?: string; areaPadreId?: number | null }
export interface ServicioInput { codigo: string; nombre: string; descripcion?: string }
export interface VigenciaInput { anio: number; nombre: string; fechaInicio: string; fechaFin: string; presupuestoDisponible: number; estado: EstadoVigencia }
export interface RubroInput { codigo: string; nombre: string; descripcion?: string; rubroPadreId?: number | null; permiteMovimiento: boolean; categoriaFormulacion: boolean; vigenciaId?: number | null; orden: number }
export interface ConceptoInput { nombre: string; descripcion?: string; orden: number; activo?: boolean; vigenciaId: number | null }

export interface Participante { areaId: number; codigo: string; area: string; areaActiva: boolean; participante: boolean }
export interface Mapeo { id: number; vigenciaId: number; rubroOrigenId: number; rubroOrigen: string; servicioId: number; servicio: string; rubroDestinoId: number; codigoDestino: string; rubroDestino: string; activo: boolean }
export interface MapeoInput { rubroOrigenId: number; servicioId: number; rubroDestinoId: number }
export interface FormulacionRubro { rubroId: number; codigo: string; nombre: string; habilitado: boolean; solicitado: number; aprobado: number; diferencia: number; cantidadNecesidades: number; necesidades: Necesidad[]; orden: number }
export interface Formulacion { vigenciaId: number; areaId: number; area: string; puedeCrear: boolean; enviada: boolean; solicitado: number; aprobado: number; diferencia: number; rubros: FormulacionRubro[]; fechaEnvio: string | null; enviadoPorId: number | null; enviadoPor: string | null; disponible: boolean; estado: EstadoFormulario }
export interface MaestroRubro { rubroId: number; codigo: string; nombre: string; nivel: number; solicitado: number; aprobado: number; diferencia: number; hijos: MaestroRubro[]; solicitadoGeneral: number; aprobadoGeneral: number }
export interface MapeoPendiente { rubroOrigenId: number; rubroOrigen: string; servicioId: number; servicio: string; solicitado: number; aprobado: number }
export interface Maestro { vigenciaId: number; totalSolicitado: number; totalAprobado: number; diferencia: number; mapeoCompleto: boolean; mapeosPendientes: MapeoPendiente[]; rubros: MaestroRubro[] }
