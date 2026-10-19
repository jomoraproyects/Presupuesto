import { client } from '../api/client'
import type { Area, AreaInput, ConceptoInput, Rubro, RubroInput, RubroNodo, Servicio, ServicioInput, Vigencia, VigenciaInput } from '../types/domain'

function catalog<T, I>(path: string) {
  return {
    async list(): Promise<T[]> { return (await client.get<T[]>(path)).data },
    async get(id: number): Promise<T> { return (await client.get<T>(`${path}/${id}`)).data },
    async create(input: I): Promise<T> { return (await client.post<T>(path, input)).data },
    async update(id: number, input: I): Promise<T> { return (await client.put<T>(`${path}/${id}`, input)).data },
    async setActive(id: number, activo: boolean): Promise<T> {
      return (await client.patch<T>(`${path}/${id}/estado`, { activo })).data
    },
  }
}

export const areasService = catalog<Area, AreaInput>('/areas')
export const serviciosService = catalog<Servicio, ServicioInput>('/servicios')
export const vigenciasService = catalog<Vigencia, VigenciaInput>('/vigencias')
export const rubrosService = {
  ...catalog<Rubro, RubroInput>('/rubros'),
  async tree(vigenciaId?: number): Promise<RubroNodo[]> {
    return (await client.get<RubroNodo[]>('/rubros/arbol', { params: vigenciaId ? { vigenciaId } : {} })).data
  },
}
export const conceptosService = {
  ...catalog<Rubro, ConceptoInput>('/rubros/conceptos'),
  async list(vigenciaId?: number | null): Promise<Rubro[]> {
    return (await client.get<Rubro[]>('/rubros/conceptos', { params: vigenciaId ? { vigenciaId } : {} })).data
  },
}
