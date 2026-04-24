import { useEffect, useState, useCallback } from 'react'
import { api } from './api/client'
import { ProviderTabs } from './components/ProviderTabs'
import { SchemaForm } from './components/SchemaForm'
import { CodePreview } from './components/CodePreview'
import type { ProviderSchema, ProviderSummary, FormState, InstanceValues } from './types/schema'
import { useDebounce } from './hooks/useDebounce'

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

  const debouncedState = useDebounce(formState, 400)

  useEffect(() => {
    api.listProviders()
      .then(list => { setProviders(list); if (list.length > 0) setActiveProviderId(list[0].id) })
      .catch(e => setError(`Failed to load providers: ${e.message}`))
  }, [])

  useEffect(() => {
    if (!activeProviderId) return
    setSchema(null); setGeneratedCode('')
    api.getSchema(activeProviderId)
      .then(s => { setSchema(s); setFormState(buildDefaultState(s)) })
      .catch(e => setError(`Failed to load schema: ${e.message}`))
  }, [activeProviderId])

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

  const filename = schema ? `main${schema.fileExtension}` : 'main.tf'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
      {/* Top bar */}
      <header style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 20px', borderBottom: '1px solid var(--color-border)',
        flexShrink: 0, background: 'var(--color-bg)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <span style={{ fontSize: '13px', fontWeight: '500' }}>InfraForge</span>
          {providers.length > 0 && (
            <ProviderTabs providers={providers} activeId={activeProviderId} onSelect={setActiveProviderId} />
          )}
        </div>
        <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>v0.2.0</span>
      </header>

      {error && (
        <div style={{
          padding: '8px 20px', background: '#fff5f5', borderBottom: '1px solid #fecaca',
          fontSize: '12px', color: '#b91c1c', flexShrink: 0,
        }}>
          {error}
        </div>
      )}

      <main style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <div style={{ flex: 1, borderRight: '1px solid var(--color-border)', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {schema ? (
            <SchemaForm schema={schema} state={formState} onChange={setFormState} onGenerate={handleManualGenerate} />
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
