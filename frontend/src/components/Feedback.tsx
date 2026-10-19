import type { ReactNode } from 'react'

export function Loading({ label = 'Cargando información…' }: { label?: string }) {
  return <div className="state-box" role="status"><div className="spinner-border text-primary" aria-hidden="true" /><span>{label}</span></div>
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return <div className="state-box empty-state"><div className="empty-icon" aria-hidden="true">◇</div><strong>{title}</strong>{children && <div>{children}</div>}</div>
}

export function PageTitle({ eyebrow, title, action, children }: { eyebrow?: string; title: string; action?: ReactNode; children?: ReactNode }) {
  return <div className="page-title"><div><div className="page-eyebrow">{eyebrow}</div><h1>{title}</h1>{children && <p className="text-muted mb-0">{children}</p>}</div>{action && <div>{action}</div>}</div>
}
