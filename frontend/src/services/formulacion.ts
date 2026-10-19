import { client } from '../api/client'
import type { Formulacion, Maestro, Mapeo, MapeoInput, Participante, Vigencia, VigenciaInput } from '../types/domain'
export const formulacionService = {
  async send(vigenciaId: number, areaId: number): Promise<Formulacion> {
    return (await client.post<Formulacion>(`/formulacion/vigencias/${vigenciaId}/enviar`, null, { params: { areaId } })).data
  },
  async saveDraft(vigenciaId: number, areaId: number): Promise<Formulacion> {
    return (await client.post<Formulacion>(`/formulacion/vigencias/${vigenciaId}/borrador`, null, { params: { areaId } })).data
  },
  async get(vigenciaId: number, areaId?: number): Promise<Formulacion> {
    return (await client.get<Formulacion>(`/formulacion/vigencias/${vigenciaId}`, { params: { areaId } })).data
  },
  async approve(vigenciaId: number, areaId: number): Promise<Formulacion> {
    return (await client.post<Formulacion>(`/formulacion/vigencias/${vigenciaId}/aprobar`, null, { params: { areaId } })).data
  },
  async maestro(vigenciaId: number): Promise<Maestro> {
    return (await client.get<Maestro>(`/presupuesto/vigencias/${vigenciaId}/maestro`)).data
  },
}
const base = (id: number) => `/vigencias/${id}`
export const configuracionService = {
  async createBudget(anio: number): Promise<Vigencia> {
    return (await client.post<Vigencia>('/vigencias/presupuestos', { anio })).data
  },
  async publishBudget(vigenciaId: number): Promise<Vigencia> {
    return (await client.post<Vigencia>(`/vigencias/${vigenciaId}/enviar-dependencias`)).data
  },
  async deleteBudget(vigenciaId: number, confirmacion: string): Promise<void> {
    await client.delete(`/vigencias/${vigenciaId}`, { data: { confirmacion } })
  },
  async resetBudgets(confirmacion: string): Promise<void> {
    await client.delete('/vigencias/presupuestos', { data: { confirmacion } })
  },
  async participantes(vigenciaId: number): Promise<Participante[]> { return (await client.get<Participante[]>(`${base(vigenciaId)}/participantes`)).data },
  async participante(vigenciaId: number, areaId: number, activo: boolean): Promise<Participante> {
    return (await client.put<Participante>(`${base(vigenciaId)}/participantes/${areaId}`, { activo })).data
  },
  async mapeos(vigenciaId: number): Promise<Mapeo[]> { return (await client.get<Mapeo[]>(`${base(vigenciaId)}/mapeos`)).data },
  async saveMapeo(vigenciaId: number, input: MapeoInput, id?: number): Promise<Mapeo> {
    return (id ? await client.put<Mapeo>(`${base(vigenciaId)}/mapeos/${id}`, input) : await client.post<Mapeo>(`${base(vigenciaId)}/mapeos`, input)).data
  },
  async mapeoEstado(vigenciaId: number, id: number, activo: boolean): Promise<Mapeo> {
    return (await client.patch<Mapeo>(`${base(vigenciaId)}/mapeos/${id}/estado`, { activo })).data
  },
  async copy(vigenciaId: number, vigencia: VigenciaInput, copiarParticipantes: boolean): Promise<Vigencia> {
    return (await client.post<Vigencia>(`${base(vigenciaId)}/copiar-configuracion`, { vigencia, copiarParticipantes })).data
  },
}
