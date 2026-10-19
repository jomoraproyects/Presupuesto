import axios from 'axios'

export const TOKEN_KEY = 'presupuesto.jwt'
export const API_URL = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')

export const client = axios.create({ baseURL: API_URL, timeout: 20000 })

client.interceptors.request.use(config => {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(response => response, error => {
  if (axios.isAxiosError(error) && error.response?.status === 401 && !error.config?.url?.endsWith('/auth/login')) {
    sessionStorage.removeItem(TOKEN_KEY)
    window.dispatchEvent(new Event('presupuesto:session-expired'))
  }
  return Promise.reject(error)
})

export function apiMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === 'string' && message.trim()) return message
    if (!error.response) return 'No fue posible conectar con el servidor. Revise que el backend esté disponible.'
    return `No fue posible completar la operación (HTTP ${error.response.status}).`
  }
  return 'No fue posible completar la operación.'
}
