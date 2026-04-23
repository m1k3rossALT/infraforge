import { useEffect, useState, useCallback } from 'react'
import { api } from './api/client'
import { ProviderTabs } from './components/ProviderTabs'
import { SchemaForm } from './components/SchemaForm'
import { CodePreview } from './components/CodePreview'
import type { ProviderSchema, ProviderSummary, FormValues } from './types/schema'
import { useDebounce } from './hooks/useDebounce'

function buildDefaultValues(schema: ProviderSchema): FormValues {
  const values: FormValues = {}
  for (const section of schema.sections) {
    for (const field of section.fields) {
      values[field.id] = field.defaultValue ?? ''
    }
  }
  return values
}

export default function App() {
  const [providers, setProviders] = useState<ProviderSummary[]>([])
  const [activeProviderId, setActiveProviderId] = useState<string | null>(null)
  const [schema, setSchema] = useState<ProviderSchema | null>(null)
  const [formValues, setFormValues] = useState<FormValues>({})
  const [generatedCode, setGeneratedCode] = useState('')
  const [error, setError] = useState<string | null>(null)

  // Debounce form values so we don't call /generate on every keystroke
  const debouncedValues = useDebounce(formValues, 400)

  // Load provider list on mount
  useEffect(() => {
    api.listProviders()
      .then(list => {
        setProviders(list)
        if (list.length > 0) setActiveProviderId(list[0].id)
      })
      .catch(e => setError(`Failed to load providers: ${e.message}`))
  }, [])

  // Load schema when active provider changes
  useEffect(() => {
    if (!activeProviderId) return
    setSchema(null)
    setGeneratedCode('')
    api.getSchema(activeProviderId)
      .then(s => {
        setSchema(s)
        setFormValues(buildDefaultValues(s))
      })
      .catch(e => setError(`Failed to load schema: ${e.message}`))
  }, [activeProviderId])

  // Auto-generate when debounced form values change
  useEffect(() => {
    if (!activeProviderId || Object.keys(debouncedValues).length === 0) return
    api.generate(activeProviderId, debouncedValues)
      .then(code => { setGeneratedCode(code); setError(null) })
      .catch(e => setError(`Generation error: ${e.message}`))
  }, [activeProviderId, debouncedValues])

  const handleManualGenerate = useCallback(() => {
    if (!activeProviderId) return
    api.generate(activeProviderId, formValues)
      .then(code => { setGeneratedCode(code); setError(null) })
      .catch(e => setError(`Generation error: ${e.message}`))
  }, [activeProviderId, formValues])

  const filename = schema ? `main${schema.fileExtension}` : 'output.tf'

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      overflow: 'hidden',
    }}>
      {/* Top bar */}
      <header style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '10px 20px',
        borderBottom: '1px solid var(--color-border)',
        flexShrink: 0,
        background: 'var(--color-bg)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <span style={{ fontSize: '13px', fontWeight: '500', color: 'var(--color-text)' }}>
            InfraForge
          </span>
          {providers.length > 0 && (
            <ProviderTabs
              providers={providers}
              activeId={activeProviderId}
              onSelect={setActiveProviderId}
            />
          )}
        </div>
        <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
          v0.1.0
        </span>
      </header>

      {/* Error banner */}
      {error && (
        <div style={{
          padding: '8px 20px',
          background: '#fff5f5',
          borderBottom: '1px solid #fecaca',
          fontSize: '12px',
          color: '#b91c1c',
          flexShrink: 0,
        }}>
          {error}
        </div>
      )}

      {/* Main split pane */}
      <main style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Form pane */}
        <div style={{
          flex: 1,
          borderRight: '1px solid var(--color-border)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}>
          {schema ? (
            <SchemaForm
              schema={schema}
              values={formValues}
              onChange={setFormValues}
              onGenerate={handleManualGenerate}
            />
          ) : (
            <div style={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--color-text-muted)',
              fontSize: '12px',
            }}>
              {providers.length === 0 ? 'Loading providers…' : 'Loading schema…'}
            </div>
          )}
        </div>

        {/* Code preview pane */}
        <div style={{ flex: 1, overflow: 'hidden' }}>
          <CodePreview
            code={generatedCode}
            filename={filename}
            isLive={true}
          />
        </div>
      </main>
    </div>
  )
}
