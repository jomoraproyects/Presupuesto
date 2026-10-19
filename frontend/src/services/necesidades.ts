import { client } from '../api/client'
import type { AjusteInput, EstadoNecesidad, Historial, Necesidad, NecesidadInput, Pagina } from '../types/domain'

export interface NecesidadesFilter {
  vigenciaId?: number
  areaId?: number
  servicioId?: number
  rubroId?: number
  estado?: EstadoNecesidad | ''
  texto?: string
  page?: number
  size?: number
}

export const necesidadesService = {
  async list(filter: NecesidadesFilter): Promise<Pagina<Necesidad>> {
    return (await client.get<Pagina<Necesidad>>('/necesidades', { params: filter })).data
  },
  async get(id: number): Promise<Necesidad> { return (await client.get<Necesidad>(`/necesidades/${id}`)).data },
  async history(id: number): Promise<Historial[]> {
    return (await client.get<Historial[]>(`/necesidades/${id}/historial`)).data
  },
  async create(input: NecesidadInput): Promise<Necesidad> {
    return (await client.post<Necesidad>('/necesidades', input)).data
  },
  async update(id: number, input: NecesidadInput): Promise<Necesidad> {
    return (await client.put<Necesidad>(`/necesidades/${id}`, input)).data
  },
  async send(id: number): Promise<Necesidad> { return (await client.post<Necesidad>(`/necesidades/${id}/enviar`)).data },
  async startReview(id: number): Promise<Necesidad> { return (await client.post<Necesidad>(`/necesidades/${id}/iniciar-revision`)).data },
  async adjust(id: number, input: AjusteInput): Promise<Necesidad> {
    return (await client.patch<Necesidad>(`/necesidades/${id}/ajuste`, input)).data
  },
  async saveApproved(id: number, input: AjusteInput): Promise<Necesidad> {
    return (await client.put<Necesidad>(`/necesidades/${id}/aprobado`, input)).data
  },
  async approve(id: number): Promise<Necesidad> { return (await client.post<Necesidad>(`/necesidades/${id}/aprobar`)).data },
  async reject(id: number, observacionPresupuesto: string): Promise<Necesidad> {
    return (await client.post<Necesidad>(`/necesidades/${id}/rechazar`, { observacionPresupuesto })).data
  },
  async returnToArea(id: number, observacionPresupuesto: string): Promise<Necesidad> {
    return (await client.post<Necesidad>(`/necesidades/${id}/devolver`, { observacionPresupuesto })).data
  },
  async cancel(id: number): Promise<void> { await client.delete(`/necesidades/${id}`) },
}
