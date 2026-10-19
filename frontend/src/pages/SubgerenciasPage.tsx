import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { apiMessage } from '../api/client'
import { Empty, Loading, PageTitle } from '../components/Feedback'
import { useToast } from '../context/ToastContext'
import { useVigencia } from '../context/VigenciaContext'
import { areasService } from '../services/catalogos'
import { configuracionService } from '../services/formulacion'
import { subgerenciasService } from '../services/subgerencias'
import { usuariosService } from '../services/usuarios'
import type { Area, Participante, Usuario } from '../types/domain'

const blank = { codigo: '', nombre: '', descripcion: '', responsable: '', username: '', password: '' }
export function SubgerenciasPage() {
  const { selectedId, vigencias } = useVigencia(); const { notify } = useToast()
  const [form, setForm] = useState(blank); const [yearId, setYearId] = useState<number | null>(null)
  const [areas, setAreas] = useState<Area[]>([]); const [users, setUsers] = useState<Usuario[]>([])
  const [participants, setParticipants] = useState<Participante[]>([])
  const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  const [created, setCreated] = useState<{ area: string; username: string; vigenciaId: number | null } | null>(null)
  const availableYears = vigencias.filter(v => v.activa && ['BORRADOR', 'ABIERTA'].includes(v.estado))
  useEffect(() => { setYearId(vigencias.some(v => v.id === selectedId && v.activa && ['BORRADOR', 'ABIERTA'].includes(v.estado)) ? selectedId : null) }, [selectedId, vigencias])
  const load = useCallback(async () => {
    const [a, u, p] = await Promise.all([areasService.list(), usuariosService.list(), selectedId ? configuracionService.participantes(selectedId) : Promise.resolve([])])
    setAreas(a); setUsers(u); setParticipants(p)
  }, [selectedId])
  useEffect(() => { let active = true; setLoading(true); setError(''); load().catch(reason => { if (active) setError(apiMessage(reason)) }).finally(() => { if (active) setLoading(false) }); return () => { active = false } }, [load])
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setCreated(null)
    try {
      const result = await subgerenciasService.create({ area: { codigo: form.codigo.trim(), nombre: form.nombre.trim(), descripcion: form.descripcion.trim() },
        responsable: { nombre: form.responsable.trim(), username: form.username.trim(), password: form.password }, vigenciaId: yearId })
      setForm(blank); setCreated({ area: result.area.nombre, username: result.responsable.username, vigenciaId: result.vigenciaId })
      notify('Subgerencia y responsable creados.', 'success'); await load()
    } catch (reason) { setError(apiMessage(reason)) } finally { setBusy(false) }
  }
  const field = (key: keyof typeof blank, label: string, max: number, type = 'text') => <div className="col-md-6"><label htmlFor={`sg-${key}`} className="form-label">{label}</label><input id={`sg-${key}`} className="form-control" type={type} required={key !== 'descripcion'} maxLength={max} minLength={key === 'password' ? 11 : undefined} autoComplete={key === 'password' ? 'new-password' : 'off'} value={form[key]} onChange={e => setForm({ ...form, [key]: e.target.value })} /></div>
  return <><PageTitle eyebrow="ADMINISTRACIÓN" title="Subgerencias y responsables" action={<Link to="/admin/areas" className="btn btn-outline-secondary">Editar áreas existentes</Link>}>Cree la subgerencia y su acceso para presentar presupuesto.</PageTitle>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    {created && <div className="alert alert-success" role="status"><strong>{created.area}</strong> creada. Su responsable ingresa con el usuario <strong>{created.username}</strong> y la contraseña que acaba de definir. {created.vigenciaId ? `Habilitada para la vigencia ${vigencias.find(v => v.id === created.vigenciaId)?.anio}.` : 'Habilite su participación desde Configuración anual cuando corresponda.'}</div>}
    <section className="card section-card border-0 mb-3"><div className="card-body"><h2 className="h5 mb-3">Nueva subgerencia con credenciales</h2><form onSubmit={submit}><fieldset disabled={busy}><div className="row g-3">
      {field('codigo', 'Código de subgerencia', 40)}{field('nombre', 'Nombre de subgerencia', 150)}{field('descripcion', 'Descripción', 500)}
      <div className="col-md-6"><label htmlFor="sg-vigencia" className="form-label">Habilitar en vigencia</label><select id="sg-vigencia" className="form-select" value={yearId ?? ''} onChange={e => setYearId(e.target.value ? Number(e.target.value) : null)}><option value="">Solo crear; habilitar después</option>{availableYears.map(v => <option key={v.id} value={v.id}>{v.anio} · {v.nombre}</option>)}</select></div>
      <div className="col-12"><h3 className="h6 mt-2">Responsable y acceso</h3></div>{field('responsable', 'Nombre del responsable', 150)}{field('username', 'Usuario o correo de acceso', 100)}{field('password', 'Contraseña inicial', 100, 'password')}
      <div className="col-md-6 d-flex align-items-center"><p className="small text-muted mb-0">Mínimo 11 caracteres. El responsable solo podrá gestionar su subgerencia. La contraseña no se mostrará después de guardar.</p></div>
      <div className="col-12"><button className="btn btn-primary">{busy ? 'Creando…' : 'Crear subgerencia y responsable'}</button></div>
    </div></fieldset></form></div></section>
    {loading ? <Loading label="Cargando subgerencias…" /> : areas.length === 0 ? <Empty title="Todavía no hay subgerencias" /> : <section className="card section-card border-0"><div className="table-responsive"><table className="table align-middle mb-0"><thead><tr><th>Subgerencia</th><th>Responsables / Usuario</th><th>Estado</th><th>Vigencia seleccionada</th><th></th></tr></thead><tbody>{areas.map(a => {
      const owners = users.filter(u => u.areaId === a.id && u.rol === 'RESPONSABLE_AREA')
      return <tr key={a.id}><td><strong>{a.nombre}</strong><div className="text-muted small">{a.codigo}</div></td><td>{owners.length ? owners.map(u => <div key={u.id}>{u.nombre} · <strong>{u.username}</strong>{!u.activo && ' (usuario inactivo)'}</div>) : 'Sin responsable'}</td><td>{a.activa ? 'Activa' : 'Inactiva'}</td><td>{selectedId ? participants.find(p => p.areaId === a.id)?.participante ? 'Participante' : 'Sin habilitar' : 'Sin vigencia'}</td><td><div className="d-flex flex-wrap gap-2"><Link className="btn btn-outline-primary btn-sm" to="/admin/usuarios">Gestionar accesos</Link><Link className="btn btn-outline-secondary btn-sm" to="/configuracion-vigencia">Participación anual</Link></div></td></tr>
    })}</tbody></table></div></section>}
  </>
}
