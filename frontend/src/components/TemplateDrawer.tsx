import { useEffect, useRef, useState } from 'react'
import { prebuiltApi, shareApi, templateApi } from '../api/client'
import type { PrebuiltTemplate, SavedTemplate, TemplateSummary } from '../types/schema'

interface Props {
  open: boolean
  onClose: () => void
  onLoad: (template: SavedTemplate) => void
  onImport: (template: SavedTemplate) => void
  onForkPrebuilt: (template: SavedTemplate) => void
  isAuthenticated: boolean
}

type Filter = 'all' | 'saved' | 'prebuilt' | string  // string = providerId

const PROVIDER_COLORS: Record<string, { bg: string; color: string; label: string }> = {
  terraform:      { bg: '#EEEDFE', color: '#534AB7', label: 'TF' },
  ansible:        { bg: '#FAEEDA', color: '#854F0B', label: 'AN' },
  vagrant:        { bg: '#EAF3DE', color: '#3B6D11', label: 'VA' },
  kubernetes:     { bg: '#E6F1FB', color: '#185FA5', label: 'K8' },
  dockerfile:     { bg: '#E1F5EE', color: '#0F6E56', label: 'DF' },
  'docker-compose': { bg: '#FAECE7', color: '#993C1D', label: 'DC' },
}

function ProviderBadge({ providerId, size = 24 }: { providerId: string; size?: number }) {
  const c = PROVIDER_COLORS[providerId] ?? { bg: '#F1EFE8', color: '#5F5E5A', label: providerId.slice(0, 2).toUpperCase() }
  return (
    <div style={{
      width: size, height: size, borderRadius: 4,
      background: c.bg, color: c.color,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: size * 0.42, fontWeight: 500, flexShrink: 0,
    }}>
      {c.label}
    </div>
  )
}

export function TemplateDrawer({ open, onClose, onLoad, onImport, onForkPrebuilt, isAuthenticated }: Props) {
  const [savedTemplates, setSavedTemplates] = useState<TemplateSummary[]>([])
  const [prebuilts, setPrebuilts] = useState<PrebuiltTemplate[]>([])
  const [loading, setLoading] = useState(false)
  const [search, setSearch] = useState('')
  const [activeFilter, setActiveFilter] = useState<Filter>('all')
  const [feedback, setFeedback] = useState<{ id: string; msg: string } | null>(null)
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null)
  const [forkingId, setForkingId] = useState<string | null>(null)
  const [importing, setImporting] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const showFeedback = (id: string, msg: string) => {
    setFeedback({ id, msg })
    setTimeout(() => setFeedback(null), 2000)
  }

  const loadData = () => {
    setLoading(true)
    const promises: Promise<any>[] = [prebuiltApi.list()]
    if (isAuthenticated) promises.push(templateApi.list())

    Promise.all(promises)
      .then(([pb, saved]) => {
        setPrebuilts(pb ?? [])
        setSavedTemplates(saved ?? [])
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (open) { setSearch(''); setActiveFilter('all'); loadData() }
  }, [open, isAuthenticated])

  // Derive available provider filter chips from loaded data
  const allProviderIds = Array.from(new Set([
    ...savedTemplates.map(t => t.providerId),
    ...prebuilts.map(t => t.providerId),
  ])).sort()

  // Filtering logic
  const matchesSearch = (name: string, desc?: string, tags?: string[]) => {
    if (!search.trim()) return true
    const q = search.toLowerCase()
    return (
      name.toLowerCase().includes(q) ||
      (desc?.toLowerCase().includes(q) ?? false) ||
      (tags?.some(tag => tag.toLowerCase().includes(q)) ?? false)
    )
  }

  const filteredSaved = savedTemplates.filter(t => {
    if (activeFilter === 'prebuilt') return false
    if (activeFilter !== 'all' && activeFilter !== 'saved' && t.providerId !== activeFilter) return false
    return matchesSearch(t.name, t.description, t.tags)
  })

  const filteredPrebuilts = prebuilts.filter(t => {
    if (activeFilter === 'saved') return false
    if (activeFilter !== 'all' && activeFilter !== 'prebuilt' && t.providerId !== activeFilter) return false
    return matchesSearch(t.name, t.description, t.tags)
  })

  const handleLoad = async (id: string) => {
    try {
      const full = await templateApi.getById(id)
      onLoad(full)
      onClose()
    } catch (e) { console.error(e) }
  }

  const handleDelete = async (id: string) => {
    try {
      await templateApi.delete(id)
      setSavedTemplates(prev => prev.filter(t => t.id !== id))
      setDeleteConfirmId(null)
    } catch (e) { console.error(e) }
  }

  const handleDuplicate = async (id: string) => {
    try {
      await templateApi.duplicate(id)
      showFeedback(id, 'Duplicated')
      loadData()
    } catch (e) { console.error(e) }
  }

  const handleExport = async (id: string, name: string) => {
    try {
      await templateApi.export(id, name)
      showFeedback(id, 'Exported')
    } catch (e) { console.error(e) }
  }

  const handleShare = async (id: string) => {
    try {
      const { shareUrl, shareToken } = await shareApi.share(id)
      await navigator.clipboard.writeText(shareUrl)
      setSavedTemplates(prev => prev.map(t => t.id === id ? { ...t, shareToken } : t))
      showFeedback(id, 'Link copied')
    } catch (e) { console.error(e) }
  }

  const handleCopyLink = async (shareToken: string, id: string) => {
    await navigator.clipboard.writeText(`${window.location.origin}/shared/${shareToken}`)
    showFeedback(id, 'Link copied')
  }

  const handleUnshare = async (id: string) => {
    try {
      await shareApi.unshare(id)
      setSavedTemplates(prev => prev.map(t => t.id === id ? { ...t, shareToken: undefined } : t))
      showFeedback(id, 'Unshared')
    } catch (e) { console.error(e) }
  }

  const handleFork = async (prebuilt: PrebuiltTemplate) => {
    if (!isAuthenticated) {
      showFeedback(prebuilt.id, 'Sign in to fork')
      return
    }
    setForkingId(prebuilt.id)
    try {
      const forked = await prebuiltApi.fork(prebuilt.id)
      onForkPrebuilt(forked)
      showFeedback(prebuilt.id, 'Forked!')
      loadData()
    } catch (e) { console.error(e) }
    finally { setForkingId(null) }
  }

  const handleOpenPrebuilt = async (prebuilt: PrebuiltTemplate) => {
    // Build a synthetic SavedTemplate from the prebuilt so App can render it read-only
    const synthetic: SavedTemplate = {
      id: `prebuilt:${prebuilt.id}`,
      name: prebuilt.name,
      providerId: prebuilt.providerId,
      formState: prebuilt.formState,
      description: prebuilt.description,
      tags: prebuilt.tags,
      updatedAt: new Date().toISOString(),
      generatedAt: new Date().toISOString(),
    }
    onLoad(synthetic)
    onClose()
  }

  const handleImportFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImporting(true)
    try {
      const imported = await templateApi.import(file)
      onImport(imported)
      loadData()
      onClose()
    } catch (e) { console.error(e) }
    finally {
      setImporting(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })

  const filterChips: { key: Filter; label: string }[] = [
    { key: 'all', label: 'All' },
    { key: 'saved', label: 'Saved' },
    { key: 'prebuilt', label: 'Prebuilt' },
    ...allProviderIds.map(id => ({
      key: id as Filter,
      label: PROVIDER_COLORS[id]?.label ?? id,
    })),
  ]

  return (
    <>
      {open && (
        <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.15)', zIndex: 40 }} />
      )}

      <div style={{
        position: 'fixed', top: 0, left: 0, bottom: 0, width: '340px',
        background: 'var(--color-bg)', borderRight: '1px solid var(--color-border)',
        zIndex: 50, display: 'flex', flexDirection: 'column',
        transform: open ? 'translateX(0)' : 'translateX(-100%)',
        transition: 'transform 0.22s ease',
        boxShadow: open ? '4px 0 16px rgba(0,0,0,0.08)' : 'none',
      }}>

        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '12px 14px', borderBottom: '1px solid var(--color-border)', flexShrink: 0,
        }}>
          <span style={{ fontSize: '13px', fontWeight: '500' }}>Library</span>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px', color: 'var(--color-text-muted)', lineHeight: 1 }}>×</button>
        </div>

        {/* Search */}
        <div style={{ padding: '10px 14px', borderBottom: '1px solid var(--color-border)', flexShrink: 0 }}>
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search templates…"
            style={{
              width: '100%', padding: '6px 10px', fontSize: '12px',
              border: '0.5px solid var(--color-border)', borderRadius: 'var(--radius-md)',
              background: 'var(--color-surface)', color: 'var(--color-text-primary)',
              outline: 'none',
            }}
          />
        </div>

        {/* Filter chips */}
        <div style={{
          display: 'flex', gap: '6px', padding: '8px 14px',
          borderBottom: '1px solid var(--color-border)',
          overflowX: 'auto', flexShrink: 0,
        }}>
          {filterChips.map(chip => (
            <button
              key={chip.key}
              onClick={() => setActiveFilter(chip.key)}
              style={{
                padding: '3px 10px', fontSize: '11px', borderRadius: '20px',
                border: '0.5px solid var(--color-border)', cursor: 'pointer',
                whiteSpace: 'nowrap', flexShrink: 0,
                background: activeFilter === chip.key ? 'var(--color-accent)' : 'none',
                color: activeFilter === chip.key ? 'var(--color-bg)' : 'var(--color-text-secondary)',
              }}
            >
              {chip.label}
            </button>
          ))}
        </div>

        {/* Import button */}
        {isAuthenticated && (
          <div style={{ padding: '8px 14px', borderBottom: '1px solid var(--color-border)', flexShrink: 0 }}>
            <input ref={fileInputRef} type="file" accept=".tf,.yml,.yaml,Vagrantfile" onChange={handleImportFile} style={{ display: 'none' }} />
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={importing}
              style={{
                width: '100%', padding: '5px 0', fontSize: '12px',
                color: 'var(--color-text-secondary)', background: 'none',
                border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-md)',
                cursor: 'pointer', opacity: importing ? 0.6 : 1,
              }}
            >
              {importing ? 'Importing…' : '↑ Import file'}
            </button>
          </div>
        )}

        {/* List */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
          {loading && (
            <p style={{ padding: '16px 14px', fontSize: '12px', color: 'var(--color-text-muted)' }}>Loading…</p>
          )}

          {/* ── Saved templates ─────────────────────────────── */}
          {!loading && filteredSaved.length > 0 && (
            <>
              <div style={{ padding: '6px 14px', fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--color-text-muted)', fontWeight: '500', background: 'var(--color-surface)' }}>
                My templates
              </div>
              {filteredSaved.map(t => (
                <div key={t.id} style={{ padding: '8px 14px', borderBottom: '0.5px solid var(--color-border)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                    <ProviderBadge providerId={t.providerId} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: '12px', fontWeight: '500', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.name}</div>
                      <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>{formatDate(t.updatedAt)}</div>
                    </div>
                    {t.shareToken && (
                      <span style={{ fontSize: '10px', color: 'var(--color-live)', border: '1px solid var(--color-live)', borderRadius: '3px', padding: '1px 5px', flexShrink: 0 }}>Shared</span>
                    )}
                  </div>

                  {deleteConfirmId === t.id ? (
                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button onClick={() => handleDelete(t.id)} style={{ flex: 1, padding: '4px 0', fontSize: '11px', color: '#b91c1c', background: '#fff5f5', border: '1px solid #fecaca', borderRadius: '4px', cursor: 'pointer' }}>Confirm delete</button>
                      <button onClick={() => setDeleteConfirmId(null)} style={{ flex: 1, padding: '4px 0', fontSize: '11px', color: 'var(--color-text-secondary)', background: 'none', border: '1px solid var(--color-border)', borderRadius: '4px', cursor: 'pointer' }}>Cancel</button>
                    </div>
                  ) : (
                    <>
                      <div style={{ display: 'flex', gap: '5px', marginBottom: '5px' }}>
                        <button onClick={() => handleLoad(t.id)} style={btnStyle('#534AB7', '#EEEDFE')}>Open</button>
                        <button onClick={() => handleDuplicate(t.id)} style={btnStyle()}>{feedback?.id === t.id && feedback.msg === 'Duplicated' ? '✓' : 'Duplicate'}</button>
                        <button onClick={() => handleExport(t.id, t.name)} style={btnStyle()}>{feedback?.id === t.id && feedback.msg === 'Exported' ? '↓ Done' : 'Export'}</button>
                        <button onClick={() => setDeleteConfirmId(t.id)} style={btnStyle()}>✕</button>
                      </div>
                      <div style={{ display: 'flex', gap: '5px' }}>
                        {t.shareToken ? (
                          <>
                            <button onClick={() => handleCopyLink(t.shareToken!, t.id)} style={btnStyle('var(--color-live)')}>
                              {feedback?.id === t.id && feedback.msg === 'Link copied' ? '✓ Copied' : '⎘ Copy link'}
                            </button>
                            <button onClick={() => handleUnshare(t.id)} style={btnStyle()}>Unshare</button>
                          </>
                        ) : (
                          <button onClick={() => handleShare(t.id)} style={btnStyle()}>Share</button>
                        )}
                      </div>
                    </>
                  )}
                </div>
              ))}
            </>
          )}

          {/* ── Prebuilt templates ──────────────────────────── */}
          {!loading && filteredPrebuilts.length > 0 && (
            <>
              <div style={{ padding: '6px 14px', fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--color-text-muted)', fontWeight: '500', background: 'var(--color-surface)' }}>
                Prebuilt templates
              </div>
              {filteredPrebuilts.map(t => (
                <div key={t.id} style={{ padding: '8px 14px', borderBottom: '0.5px solid var(--color-border)' }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', marginBottom: '6px' }}>
                    <ProviderBadge providerId={t.providerId} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: '12px', fontWeight: '500', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.name}</div>
                      {t.description && (
                        <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '2px', lineHeight: '1.4', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                          {t.description}
                        </div>
                      )}
                      {t.tags && t.tags.length > 0 && (
                        <div style={{ display: 'flex', gap: '4px', marginTop: '4px', flexWrap: 'wrap' }}>
                          {t.tags.slice(0, 3).map(tag => (
                            <span key={tag} style={{ fontSize: '10px', padding: '1px 5px', borderRadius: '3px', background: 'var(--color-surface)', border: '0.5px solid var(--color-border)', color: 'var(--color-text-muted)' }}>
                              {tag}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '5px' }}>
                    <button onClick={() => handleOpenPrebuilt(t)} style={btnStyle()}>Preview</button>
                    <button
                      onClick={() => handleFork(t)}
                      disabled={forkingId === t.id}
                      style={btnStyle('#534AB7', '#EEEDFE')}
                    >
                      {forkingId === t.id ? 'Forking…' : feedback?.id === t.id && feedback.msg === 'Forked!' ? '✓ Forked' : !isAuthenticated ? 'Sign in to fork' : 'Fork & edit'}
                    </button>
                  </div>
                </div>
              ))}
            </>
          )}

          {/* Empty state */}
          {!loading && filteredSaved.length === 0 && filteredPrebuilts.length === 0 && (
            <p style={{ padding: '20px 14px', fontSize: '12px', color: 'var(--color-text-muted)', textAlign: 'center' }}>
              {search ? 'No templates match your search.' : 'No templates yet.'}
            </p>
          )}
        </div>
      </div>
    </>
  )
}

function btnStyle(color?: string, bg?: string): React.CSSProperties {
  return {
    flex: 1, padding: '4px 0', fontSize: '11px',
    color: color ?? 'var(--color-text-secondary)',
    background: bg ?? 'none',
    border: `1px solid ${bg ? 'transparent' : 'var(--color-border)'}`,
    borderRadius: '4px', cursor: 'pointer',
  }
}
