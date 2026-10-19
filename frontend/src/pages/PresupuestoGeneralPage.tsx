import { Link } from 'react-router'
import { PageTitle } from '../components/Feedback'
import { useVigencia } from '../context/VigenciaContext'

export function PresupuestoGeneralPage() {
  const { selected } = useVigencia()
  return <>
    <PageTitle eyebrow="PRESUPUESTO GENERAL" title={selected ? `Presupuesto General ${selected.anio}` : 'Presupuesto General'}>
      Apartado reservado para la estructura que definiremos después.
    </PageTitle>
    <div className="card section-card border-0"><div className="card-body py-4">
      <h2 className="h5 mb-2">Pendiente de definir</h2>
      <p className="text-muted mb-3">La revisión de subgerencias, conceptos e ítems se realiza en Presupuesto detallado.</p>
      <Link to="/presupuesto-detallado" className="btn btn-outline-primary">Abrir Presupuesto detallado</Link>
    </div></div>
  </>
}
