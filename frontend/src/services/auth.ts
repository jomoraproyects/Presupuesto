import { client } from '../api/client'
import type { Perfil } from '../types/domain'

interface LoginResponse { token: string; tokenType: string; expiresInSeconds: number }

export const authService = {
  async login(username: string, password: string) {
    const { data } = await client.post<LoginResponse>('/auth/login', { username, password })
    return data
  },
  async me() {
    const { data } = await client.get<Perfil>('/auth/me')
    return data
  },
}
