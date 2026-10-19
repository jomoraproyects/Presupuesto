import { client } from '../api/client'
import type { ConsolidadoArea, ConsolidadoRubro, ConsolidadoServicio, PresupuestoGeneralConcepto, ResumenArea, ResumenPresupuesto } from '../types/domain'

const base = (id: number) => `/presupuesto/vigencias/${id}`

export const consolidadosService = {
  async general(id: number): Promise<PresupuestoGeneralConcepto[]> {
    return (await client.get<PresupuestoGeneralConcepto[]>(`${base(id)}/general`)).data
  },
  async summary(id: number): Promise<ResumenPresupuesto> {
    return (await client.get<ResumenPresupuesto>(`${base(id)}/resumen`)).data
  },
  async myArea(id: number): Promise<ResumenArea> {
    return (await client.get<ResumenArea>(`${base(id)}/mi-area`)).data
  },
  async byArea(id: number): Promise<ConsolidadoArea[]> {
    return (await client.get<ConsolidadoArea[]>(`${base(id)}/por-area`)).data
  },
  async byService(id: number): Promise<ConsolidadoServicio[]> {
    return (await client.get<ConsolidadoServicio[]>(`${base(id)}/por-servicio`)).data
  },
  async byRubro(id: number): Promise<ConsolidadoRubro[]> {
    return (await client.get<ConsolidadoRubro[]>(`${base(id)}/por-rubro`)).data
  },
}
