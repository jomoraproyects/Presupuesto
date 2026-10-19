import { client } from '../api/client'
import type { Usuario, UsuarioCreateInput, UsuarioUpdateInput } from '../types/domain'

export const usuariosService = {
  async list(): Promise<Usuario[]> { return (await client.get<Usuario[]>('/usuarios')).data },
  async create(input: UsuarioCreateInput): Promise<Usuario> {
    return (await client.post<Usuario>('/usuarios', input)).data
  },
  async update(id: number, input: UsuarioUpdateInput): Promise<Usuario> {
    return (await client.put<Usuario>(`/usuarios/${id}`, input)).data
  },
  async setActive(id: number, activo: boolean): Promise<Usuario> {
    return (await client.patch<Usuario>(`/usuarios/${id}/estado`, { activo })).data
  },
}
