import { useEffect, useState } from 'react'
import { templateApi } from '../api/client'
import type { SavedTemplate, TemplateSummary } from '../types/schema'

interface Props {
  open: boolean
  onClose: () => void
  onLoad: (template: SavedTemplate) => void
}

export function TemplateDrawer({ open, onClose, onLoad }: Props) {
  const [templates, setTemplates] = useState<TemplateSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [feedback, setFeedback] = useState<{ id: string; msg: string } | null>(null)
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null)

  const showFeedback = (id: string, msg: string) => {
    setFeedback({ id, msg })
    setTimeout(() => setFeedback(null), 2000)
  }

  const loadList = () => {
    setLoading(true)
    templateApi.list()
      .then(setTemplates)
      .catch(console.error)
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (open) loadList()
  }, [open])

  const handleLoad = async (id: string) => {
    try {
      const full = await templateApi.getById(id)
      onLoad(full)
      onClose()
    } catch (e) {
      console.error('Failed to load template', e)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await templateApi.delete(id)
      setTemplates(prev => prev.filter(t => t.id !== id))
      setDeleteConfirmId(null)
    } catch (e) {
      console.error('Failed to delete template', e)
    }
  }

  const handleDuplicate = async (id: string) => {
    try {
      await templateApi.duplicate(id)
      showFeedback(id, 'Duplicated')
      loadList()
    } catch (e) {
      console.error('Failed to duplicate template', e)
    }
  }

  const formatDate = (iso: string) => {
    const d = new Date(iso)
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
  }

  return (
    <>
      {/* Backdrop */}
      {open && (
        <div
          onClick={onClose}
          style={{
            position: 'fixed', inset: 0,
            background: 'rgba(0,0,0,0.15)',
            zIndex: 40,
          }}
        />
      )}

      {/* Drawer */}
      <div style={{
        position: 'fixed',
        top: 0, left: 0, bottom: 0,
        width: '320px',
        background: 'var(--color-bg)',
        borderRight: '1px solid var(--color-border)',
        zIndex: 50,
        display: 'flex',
        flexDirection: 'column',
        transform: open ? 'translateX(0)' : 'translateX(-100%)',
        transition: 'transform 0.22s ease',
        boxShadow: open ? '4px 0 16px rgba(0,0,0,0.08)' : 'none',
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '12px 16px',
          borderBottom: '1px solid var(--color-border)',
          flexShrink: 0,
        }}>
          <span style={{ fontSize: '13px', fontWeight: '500' }}>Template Library</span>
          <button
            onClick={onClose}
            style={{
              background: 'none', border: 'none', cursor: 'pointer',
              fontSize: '18px', color: 'var(--color-text-muted)', lineHeight: 1,
            }}
          >×</button>
        </div>

        {/* List */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
          {loading && (
            <p style={{ padding: '16px', fontSize: '12px', color: 'var(--color-text-muted)' }}>
              Loading…
            </p>
          )}

          {!loading && templates.length === 0 && (
            <p style={{ padding: '16px', fontSize: '12px', color: 'var(--color-text-muted)' }}>
              No saved templates yet. Name your current template to save it.
            </p>
          )}

          {templates.map(t => (
            <div
              key={t.id}
              style={{
                padding: '10px 16px',
                borderBottom: '1px solid var(--color-border)',
                background: 'var(--color-bg)',
              }}
            >
              {/* Name and provider */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                <span style={{ fontSize: '13px', fontWeight: '500', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {t.name}
                </span>
                <span style={{
                  fontSize: '10px', color: 'var(--color-text-muted)',
                  border: '1px solid var(--color-border)', borderRadius: '3px',
                  padding: '1px 5px', flexShrink: 0,
                }}>
                  {t.providerId}
                </span>
              </div>

              {/* Date */}
              <p style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginBottom: '8px' }}>
                {formatDate(t.updatedAt)}
              </p>

              {/* Actions */}
              {deleteConfirmId === t.id ? (
                <div style={{ display: 'flex', gap: '6px' }}>
                  <button
                    onClick={() => handleDelete(t.id)}
                    style={{
                      flex: 1, padding: '4px 0', fontSize: '11px',
                      color: '#b91c1c', background: '#fff5f5',
                      border: '1px solid #fecaca', borderRadius: '4px', cursor: 'pointer',
                    }}
                  >
                    Confirm delete
                  </button>
                  <button
                    onClick={() => setDeleteConfirmId(null)}
                    style={{
                      flex: 1, padding: '4px 0', fontSize: '11px',
                      color: 'var(--color-text-secondary)', background: 'none',
                      border: '1px solid var(--color-border)', borderRadius: '4px', cursor: 'pointer',
                    }}
                  >
                    Cancel
                  </button>
                </div>
              ) : (
                <div style={{ display: 'flex', gap: '6px' }}>
                  <button
                    onClick={() => handleLoad(t.id)}
                    style={{
                      flex: 1, padding: '4px 0', fontSize: '11px',
                      color: 'var(--color-text-primary)', background: 'var(--color-surface)',
                      border: '1px solid var(--color-border)', borderRadius: '4px', cursor: 'pointer',
                    }}
                  >
                    Open
                  </button>
                  <button
                    onClick={() => handleDuplicate(t.id)}
                    style={{
                      flex: 1, padding: '4px 0', fontSize: '11px',
                      color: feedback?.id === t.id ? 'var(--color-live)' : 'var(--color-text-secondary)',
                      background: 'none',
                      border: '1px solid var(--color-border)', borderRadius: '4px', cursor: 'pointer',
                    }}
                  >
                    {feedback?.id === t.id ? feedback.msg : 'Duplicate'}
                  </button>
                  <button
                    onClick={() => setDeleteConfirmId(t.id)}
                    style={{
                      padding: '4px 8px', fontSize: '11px',
                      color: 'var(--color-text-muted)', background: 'none',
                      border: '1px solid var(--color-border)', borderRadius: '4px', cursor: 'pointer',
                    }}
                  >
                    ✕
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
