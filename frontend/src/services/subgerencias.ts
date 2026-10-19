import { client } from '../api/client'
import type { Area, AreaInput, Usuario } from '../types/domain'

export interface SubgerenciaInput {
  area: AreaInput
  responsable: { nombre: string; username: string; password: string }
  vigenciaId: number | null
}
export const subgerenciasService = {
  async create(input: SubgerenciaInput): Promise<{ area: Area; responsable: Usuario; vigenciaId: number | null }> {
    return (await client.post('/areas/con-responsable', input)).data
  },
}
