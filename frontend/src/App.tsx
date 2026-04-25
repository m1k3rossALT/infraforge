import { useCallback, useEffect, useState } from 'react'
import { api, templateApi } from './api/client'
import { CodePreview } from './components/CodePreview'
import { ProviderTabs } from './components/ProviderTabs'
import { SchemaForm } from './components/SchemaForm'
import { TemplateDrawer } from './components/TemplateDrawer'
import { useAutoSave } from './hooks/useAutoSave'
import { useDebounce } from './hooks/useDebounce'
import type {
  FormState,
  InstanceValues,
  ProviderSchema,
  ProviderSummary,
  SavedTemplate,
} from './types/schema'

function buildDefaultState(schema: ProviderSchema): FormState {
  const state: FormState = {}
  for (const section of schema.sections) {
    const defaultInst: InstanceValues = {}
    for (const field of section.fields) {
      defaultInst[field.id] = field.defaultValue ?? ''
    }
    state[section.id] = {
      enabled: section.optional ? (section.defaultEnabled ?? false) : true,
      instances: [defaultInst],
    }
  }
  return state
}

export default function App() {
  const [providers, setProviders] = useState<ProviderSummary[]>([])
  const [activeProviderId, setActiveProviderId] = useState<string | null>(null)
  const [schema, setSchema] = useState<ProviderSchema | null>(null)
  const [formState, setFormState] = useState<FormState>({})
  const [generatedCode, setGeneratedCode] = useState('')
  const [error, setError] = useState<string | null>(null)

  // Template state
  const [templateId, setTemplateId] = useState<string | null>(null)
  const [templateName, setTemplateName] = useState('')
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [manualSaving, setManualSaving] = useState(false)
  const [manualSaved, setManualSaved] = useState(false)

  const debouncedState = useDebounce(formState, 400)

  // Auto-save
  const saveStatus = useAutoSave({
    templateId,
    templateName,
    providerId: activeProviderId,
    formState,
    onSaved: (saved: SavedTemplate) => setTemplateId(saved.id),
    delayMs: 30000,
  })

  // Load providers
  useEffect(() => {
    api.listProviders()
      .then(list => {
        setProviders(list)
        if (list.length > 0) setActiveProviderId(list[0].id)
      })
      .catch(e => setError(`Failed to load providers: ${e.message}`))
  }, [])

  // Load schema on provider change
  useEffect(() => {
    if (!activeProviderId) return
    setSchema(null)
    setGeneratedCode('')
    api.getSchema(activeProviderId)
      .then(s => {
        setSchema(s)
        setFormState(buildDefaultState(s))
      })
      .catch(e => setError(`Failed to load schema: ${e.message}`))
  }, [activeProviderId])

  // Auto-generate on form change
  useEffect(() => {
    if (!activeProviderId || Object.keys(debouncedState).length === 0) return
    api.generate(activeProviderId, debouncedState)
      .then(code => { setGeneratedCode(code); setError(null) })
      .catch(e => setError(`Generation error: ${e.message}`))
  }, [activeProviderId, debouncedState])

  const handleManualGenerate = useCallback(() => {
    if (!activeProviderId) return
    api.generate(activeProviderId, formState)
      .then(code => { setGeneratedCode(code); setError(null) })
      .catch(e => setError(`Generation error: ${e.message}`))
  }, [activeProviderId, formState])

  // Manual save
  const handleManualSave = useCallback(async () => {
    if (!templateName.trim() || !activeProviderId) return
    setManualSaving(true)
    try {
      const payload = { name: templateName, providerId: activeProviderId, formState }
      const saved = templateId
        ? await templateApi.update(templateId, payload)
        : await templateApi.create(payload)
      setTemplateId(saved.id)
      setManualSaved(true)
      setTimeout(() => setManualSaved(false), 2000)
    } catch (e: any) {
      setError(`Save failed: ${e.message}`)
    } finally {
      setManualSaving(false)
    }
  }, [templateName, activeProviderId, formState, templateId])

  // Load a saved template from the drawer
  const handleLoadTemplate = useCallback((template: SavedTemplate) => {
    const matchingProvider = providers.find(p => p.id === template.providerId)
    if (!matchingProvider) return
    setActiveProviderId(template.providerId)
    setTemplateId(template.id)
    setTemplateName(template.name)
    setFormState(template.formState as FormState)
  }, [providers])

  // Handle an imported template
  const handleImportTemplate = useCallback((template: SavedTemplate) => {
    setActiveProviderId(template.providerId)
    setTemplateId(template.id)
    setTemplateName(template.name)
    setFormState(template.formState as FormState)
  }, [])

  // Clear template (new document)
  const handleClearTemplate = () => {
    setTemplateId(null)
    setTemplateName('')
    if (schema) setFormState(buildDefaultState(schema))
  }

  const filename = schema ? `main${schema.fileExtension}` : 'main.tf'

  // Save status label
  const statusLabel = () => {
    if (manualSaving || saveStatus === 'saving') return '● Saving…'
    if (manualSaved || saveStatus === 'saved') return '✓ Saved'
    if (saveStatus === 'error') return '⚠ Save failed'
    if (templateName && templateId) return '● Unsaved changes'
    return ''
  }

  const statusColor = () => {
    if (manualSaved || saveStatus === 'saved') return 'var(--color-live)'
    if (saveStatus === 'error') return '#b91c1c'
    return 'var(--color-text-muted)'
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>

      {/* Template drawer */}
      <TemplateDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onLoad={handleLoadTemplate}
        onImport={handleImportTemplate}
      />

      {/* Top bar */}
      <header style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '8px 16px',
        borderBottom: '1px solid var(--color-border)',
        flexShrink: 0, background: 'var(--color-bg)',
        gap: '12px',
      }}>
        {/* Left: library + brand + tabs */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', minWidth: 0 }}>
          <button
            onClick={() => setDrawerOpen(true)}
            title="Template library"
            style={{
              padding: '4px 10px', fontSize: '12px',
              color: 'var(--color-text-secondary)',
              background: 'none', border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)', cursor: 'pointer', flexShrink: 0,
            }}
          >
            Library
          </button>
          <span style={{ fontSize: '13px', fontWeight: '500', flexShrink: 0 }}>InfraForge</span>
          {providers.length > 0 && (
            <ProviderTabs providers={providers} activeId={activeProviderId} onSelect={id => {
              setActiveProviderId(id)
              setTemplateId(null)
              setTemplateName('')
            }} />
          )}
        </div>

        {/* Centre: template name + save status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, justifyContent: 'center', minWidth: 0 }}>
          <input
            type="text"
            value={templateName}
            onChange={e => setTemplateName(e.target.value)}
            placeholder="Name this template to save…"
            style={{
              fontSize: '13px', color: 'var(--color-text-primary)',
              background: 'none', border: 'none', outline: 'none',
              width: '260px', textAlign: 'center',
              borderBottom: templateName ? '1px solid var(--color-border)' : '1px solid transparent',
              padding: '2px 4px',
            }}
            onFocus={e => e.currentTarget.style.borderBottomColor = 'var(--color-border-strong)'}
            onBlur={e => e.currentTarget.style.borderBottomColor = templateName ? 'var(--color-border)' : 'transparent'}
          />
          {templateName && (
            <button
              onClick={handleClearTemplate}
              title="Clear template"
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-text-muted)', fontSize: '14px', padding: '0 2px' }}
            >×</button>
          )}
          {statusLabel() && (
            <span style={{ fontSize: '11px', color: statusColor(), flexShrink: 0 }}>
              {statusLabel()}
            </span>
          )}
        </div>

        {/* Right: save button + version */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
          {templateName && (
            <button
              onClick={handleManualSave}
              disabled={manualSaving}
              style={{
                padding: '4px 12px', fontSize: '12px', fontWeight: '500',
                color: 'var(--color-bg)', background: 'var(--color-accent)',
                border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                opacity: manualSaving ? 0.6 : 1,
              }}
            >
              {manualSaved ? 'Saved ✓' : 'Save'}
            </button>
          )}
          <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>v0.3.0</span>
        </div>
      </header>

      {/* Error banner */}
      {error && (
        <div style={{
          padding: '8px 16px', background: '#fff5f5',
          borderBottom: '1px solid #fecaca',
          fontSize: '12px', color: '#b91c1c', flexShrink: 0,
        }}>
          {error}
          <button onClick={() => setError(null)} style={{ marginLeft: '8px', background: 'none', border: 'none', cursor: 'pointer', color: '#b91c1c', fontSize: '12px' }}>✕</button>
        </div>
      )}

      {/* Main split pane */}
      <main style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <div style={{ flex: 1, borderRight: '1px solid var(--color-border)', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {schema ? (
            <SchemaForm
              schema={schema}
              state={formState}
              onChange={setFormState}
              onGenerate={handleManualGenerate}
            />
          ) : (
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--color-text-muted)', fontSize: '12px' }}>
              {providers.length === 0 ? 'Loading providers…' : 'Loading schema…'}
            </div>
          )}
        </div>
        <div style={{ flex: 1, overflow: 'hidden' }}>
          <CodePreview code={generatedCode} filename={filename} isLive={true} />
        </div>
      </main>
    </div>
  )
}
