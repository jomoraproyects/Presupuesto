import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { useVigencia } from '../context/VigenciaContext'
import { useToast } from '../context/ToastContext'
import { conceptosService } from '../services/catalogos'
import { configuracionService } from '../services/formulacion'
import type { Rubro } from '../types/domain'

export function CrearPresupuestoPage() {
  const { vigencias, selectedId, select, refresh } = useVigencia()
  const { notify } = useToast()
  const [id, setId] = useState<number | null>(selectedId)
  const [anio, setAnio] = useState(new Date().getFullYear() + 1)
  const [conceptos, setConceptos] = useState<Rubro[]>([])
  const [nombre, setNombre] = useState('')
  const [orden, setOrden] = useState(66)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [deleteMode, setDeleteMode] = useState<'year' | 'all' | null>(null)
  const [confirmation, setConfirmation] = useState('')
  const vigencia = vigencias.find(v => v.id === id)
  useEffect(() => { if (selectedId && id === null && vigencias.some(v => v.id === selectedId)) setId(selectedId) }, [selectedId, id, vigencias])
  useEffect(() => {
    if (!id) { setConceptos([]); return }
    let active = true; setLoading(true); setError('')
    conceptosService.list(id).then(rows => { if (active) { setConceptos(rows); setOrden(Math.max(0, ...rows.map(r => r.orden)) + 1) } })
      .catch(e => { if (active) setError(apiMessage(e)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])
  async function run(work: () => Promise<void>, message: string) {
    setBusy(true); setError('')
    try { await work(); notify(message, 'success') } catch (e) { setError(apiMessage(e)) } finally { setBusy(false) }
  }
  function create(event: FormEvent) {
    event.preventDefault()
    void run(async () => {
      const created = await configuracionService.createBudget(anio)
      await refresh(); select(created.id); setId(created.id)
    }, `Presupuesto ${anio} preparado.`)
  }
  function add(event: FormEvent) {
    event.preventDefault()
    if (!id) return
    void run(async () => {
      await conceptosService.create({ nombre: nombre.trim(), orden, activo: true, vigenciaId: id })
      const rows = await conceptosService.list(id); setConceptos(rows); setNombre(''); setOrden(Math.max(...rows.map(r => r.orden)) + 1)
    }, 'Concepto agregado al presupuesto.')
  }
  function changeConcept(r: Rubro, patch: Partial<Rubro>) {
    if (!id) return
    void run(async () => {
      const updated = await conceptosService.update(r.id, { nombre: r.nombre, orden: patch.orden ?? r.orden,
        activo: patch.activo ?? r.activo, vigenciaId: id })
      setConceptos(rows => rows.map(row => row.id === r.id ? updated : row).sort((a, b) => a.orden - b.orden || a.id - b.id))
    }, 'Concepto actualizado.')
  }
  const expectedConfirmation = deleteMode === 'all' ? 'BORRAR TODOS LOS PRESUPUESTOS' : `ELIMINAR ${vigencia?.anio ?? ''}`
  function confirmDeletion() {
    if (!deleteMode || confirmation !== expectedConfirmation) return
    void run(async () => {
      if (deleteMode === 'all') await configuracionService.resetBudgets(confirmation)
      else if (vigencia) await configuracionService.deleteBudget(vigencia.id, confirmation)
      await refresh()
      setId(null); setDeleteMode(null); setConfirmation(''); setConceptos([])
    }, deleteMode === 'all' ? 'Se eliminaron todas las vigencias y sus formularios.' : 'Presupuesto eliminado.')
  }
  return <><PageTitle eyebrow="PRESUPUESTO ANUAL" title="Crear presupuesto">Prepare los conceptos y envíe el formulario a todas las dependencias activas.</PageTitle>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    <div className="card section-card border-0 mb-3"><div className="card-body"><form className="row g-2 align-items-end" onSubmit={create}>
      <div className="col-md-3"><label className="form-label" htmlFor="nuevo-anio">Nueva vigencia</label><input id="nuevo-anio" className="form-control" type="number" min="2000" value={anio} onChange={e => setAnio(Number(e.target.value))} required /></div>
      <div className="col-md-auto"><button className="btn btn-primary" disabled={busy}>Crear Presupuesto {anio}</button></div>
      <div className="col-md-4"><label className="form-label" htmlFor="presupuesto-elegido">Presupuesto preparado</label><select id="presupuesto-elegido" className="form-select" value={id ?? ''} onChange={e => setId(Number(e.target.value) || null)}><option value="">Seleccione…</option>{vigencias.map(v => <option key={v.id} value={v.id}>{v.nombre} · {v.estado === 'BORRADOR' ? 'Sin enviar' : 'Enviado'}</option>)}</select></div>
    </form></div></div>
    {!vigencia ? <Empty title="Cree o seleccione un presupuesto" /> : loading ? <Loading label="Cargando conceptos…" /> : <>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3"><h2 className="h5 mb-0">{vigencia.nombre} · {conceptos.length} conceptos</h2>
        {vigencia.estado === 'BORRADOR' ? <button className="btn btn-primary" disabled={busy} onClick={() => void run(async () => { await configuracionService.publishBudget(vigencia.id); await refresh() }, 'Presupuesto enviado a las dependencias activas.')}>ENVIAR A DEPENDENCIAS</button> : <span className="badge text-bg-success">Enviado a dependencias</span>}</div>
      {vigencia.estado === 'BORRADOR' && <form onSubmit={add} className="row g-2 align-items-end mb-3"><div className="col-md-7"><label className="form-label" htmlFor="nuevo-concepto">Nuevo concepto</label><input id="nuevo-concepto" className="form-control" maxLength={200} required value={nombre} onChange={e => setNombre(e.target.value)} /></div><div className="col-md-2"><label className="form-label" htmlFor="nuevo-orden">Orden</label><input id="nuevo-orden" type="number" min="0" step="1" className="form-control" value={orden} onChange={e => setOrden(Number(e.target.value))} /></div><div className="col-md-auto"><button className="btn btn-outline-primary" disabled={busy}>Agregar concepto</button></div></form>}
      <div className="card section-card border-0"><div className="table-responsive"><table className="table align-middle mb-0"><thead><tr><th>Orden</th><th>Concepto</th><th>Activo</th><th></th></tr></thead><tbody>{conceptos.map(r => <tr key={r.id}><td>{r.orden}</td><td>{r.nombre}</td><td>{r.activo ? 'Sí' : 'No'}</td><td>{vigencia.estado === 'BORRADOR' && <div className="d-flex gap-2 justify-content-end"><button className="btn btn-outline-secondary btn-sm" disabled={busy} onClick={() => { const value = window.prompt('Nuevo orden', String(r.orden)); if (value !== null && /^\d+$/.test(value)) changeConcept(r, { orden: Number(value) }) }}>Cambiar orden</button><button className="btn btn-outline-primary btn-sm" disabled={busy} onClick={() => { const value = window.prompt('Nombre del concepto', r.nombre); if (value?.trim()) void run(async () => { const updated = await conceptosService.update(r.id, { nombre: value.trim(), orden: r.orden, activo: r.activo, vigenciaId: id }); setConceptos(rows => rows.map(row => row.id === r.id ? updated : row)) }, 'Concepto actualizado.') }}>Editar</button><button className="btn btn-outline-secondary btn-sm" disabled={busy} onClick={() => changeConcept(r, { activo: !r.activo })}>{r.activo ? 'Desactivar' : 'Activar'}</button></div>}</td></tr>)}</tbody></table></div></div>
      <p className="small text-muted mt-2">Los conceptos con el mismo nombre mantienen IDs y posiciones independientes. <Link to="/admin/rubros">Administrar conceptos</Link></p>
    </>}
    {vigencias.length > 0 && <section className="card section-card border-danger-subtle mt-4" aria-labelledby="delete-budget-title"><div className="card-body">
      <h2 id="delete-budget-title" className="h5 mb-2">Eliminar presupuestos</h2>
      <p className="text-muted mb-3">Estas acciones eliminan también los formularios, ítems, distribuciones y revisiones del año elegido. Los usuarios, dependencias y conceptos base se conservan.</p>
      <div className="d-flex flex-wrap gap-2">
        {vigencia && <button type="button" className="btn btn-outline-danger" disabled={busy} onClick={() => { setDeleteMode('year'); setConfirmation('') }}>Eliminar Presupuesto {vigencia.anio}</button>}
        <button type="button" className="btn btn-outline-danger" disabled={busy} onClick={() => { setDeleteMode('all'); setConfirmation('') }}>Reiniciar todo</button>
      </div>
      {deleteMode && <div className="budget-delete-confirm mt-3" role="group" aria-label="Confirmar eliminación de presupuestos">
        <p className="mb-2"><strong>{deleteMode === 'all' ? 'Eliminar todas las vigencias' : `Eliminar Presupuesto ${vigencia?.anio}`}</strong>. Escribe <code>{expectedConfirmation}</code> para confirmar.</p>
        <label className="form-label" htmlFor="budget-delete-confirmation">Confirmación</label>
        <input id="budget-delete-confirmation" className="form-control" autoComplete="off" value={confirmation} onChange={event => setConfirmation(event.target.value)} />
        <div className="d-flex flex-wrap gap-2 mt-3"><button type="button" className="btn btn-danger" disabled={busy || confirmation !== expectedConfirmation} onClick={confirmDeletion}>{busy ? 'Eliminando…' : 'Eliminar definitivamente'}</button><button type="button" className="btn btn-outline-secondary" onClick={() => { setDeleteMode(null); setConfirmation('') }}>Cancelar</button></div>
      </div>}
    </div></section>}
  </>
}
