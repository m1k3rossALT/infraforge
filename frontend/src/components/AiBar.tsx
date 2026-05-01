import { useRef, useState } from 'react'
import type { AiSettings, AiSuggestions } from '../types/schema'

interface Props {
  providerId: string
  aiSettings: AiSettings | null
  onFill: (suggestions: AiSuggestions) => void
  onOpenSettings: () => void
  onSuggest: (description: string) => Promise<AiSuggestions>
}

const PROVIDER_LABELS: Record<string, string> = {
  gemini:    'Gemini',
  openai:    'OpenAI',
  anthropic: 'Claude',
  mistral:   'Mistral',
  groq:      'Groq',
}

/**
 * Collapsible AI input bar — sits above the schema form in the left pane.
 *
 * States:
 *   - Collapsed: single row showing ✨ icon + provider badge + "Describe…" hint
 *   - Expanded: textarea + Fill button
 *   - No AI configured: clicking opens SettingsDrawer at AI tab
 *   - Loading: spinner, Fill button disabled
 *   - Error: inline error below textarea, dismissible
 */
export function AiBar({ providerId, aiSettings, onFill, onOpenSettings, onSuggest }: Props) {
  const [expanded, setExpanded] = useState(false)
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const isConfigured = aiSettings?.aiProvider && aiSettings?.hasApiKey
  const providerLabel = aiSettings?.aiProvider
    ? (PROVIDER_LABELS[aiSettings.aiProvider] ?? aiSettings.aiProvider)
    : null

  const handleBarClick = () => {
    if (!isConfigured) {
      onOpenSettings()
      return
    }
    setExpanded(e => !e)
    if (!expanded) {
      setTimeout(() => textareaRef.current?.focus(), 50)
    }
  }

  const handleFill = async () => {
    if (!description.trim() || loading) return
    setLoading(true)
    setError(null)
    try {
      const suggestions = await onSuggest(description.trim())
      const hasAnySuggestions = Object.keys(suggestions).length > 0
      if (hasAnySuggestions) {
        onFill(suggestions)
        setExpanded(false)
        setDescription('')
      } else {
        setError('No suggestions found for this description. Try being more specific.')
      }
    } catch (e: any) {
      if (e.message?.includes('429')) {
        setError('Rate limit reached. You can make 10 requests per minute.')
      } else {
        setError('Could not reach AI provider. Check your API key in Settings.')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      e.preventDefault()
      handleFill()
    }
    if (e.key === 'Escape') {
      setExpanded(false)
    }
  }

  return (
    <div style={{
      borderBottom: '1px solid var(--color-border)',
      background: 'var(--color-surface)',
      flexShrink: 0,
    }}>
      {/* Bar header — always visible */}
      <div
        onClick={handleBarClick}
        style={{
          display: 'flex', alignItems: 'center', gap: '8px',
          padding: '8px 20px', cursor: 'pointer',
          userSelect: 'none',
        }}
      >
        <span style={{ fontSize: '13px' }}>✨</span>

        {isConfigured ? (
          <>
            <span style={{ fontSize: '12px', color: 'var(--color-text-muted)', flex: 1 }}>
              {expanded ? 'Describe what you want to build…' : 'Fill form with AI'}
            </span>
            <span style={{
              fontSize: '10px', color: 'var(--color-live)',
              border: '1px solid var(--color-live)',
              borderRadius: '3px', padding: '1px 5px', flexShrink: 0,
            }}>
              {providerLabel}
            </span>
            <span style={{
              fontSize: '11px', color: 'var(--color-text-muted)',
              transition: 'transform 0.15s',
              transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)',
            }}>▾</span>
          </>
        ) : (
          <>
            <span style={{ fontSize: '12px', color: 'var(--color-text-muted)', flex: 1 }}>
              Configure AI to fill the form from a description
            </span>
            <span style={{
              fontSize: '11px', color: 'var(--color-text-secondary)',
              border: '1px solid var(--color-border)',
              borderRadius: '3px', padding: '1px 6px', flexShrink: 0,
            }}>
              Set up →
            </span>
          </>
        )}
      </div>

      {/* Expanded input area */}
      {expanded && isConfigured && (
        <div style={{ padding: '0 20px 12px' }}>
          <textarea
            ref={textareaRef}
            value={description}
            onChange={e => setDescription(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={`Describe your ${providerId} configuration… (⌘↵ to fill)`}
            rows={3}
            disabled={loading}
            style={{
              width: '100%', resize: 'none',
              fontFamily: 'var(--font-sans)', fontSize: '13px',
              color: 'var(--color-text-primary)',
              background: 'var(--color-bg)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              padding: '8px 10px', outline: 'none',
              marginBottom: '8px',
              opacity: loading ? 0.6 : 1,
            }}
            onFocus={e => e.currentTarget.style.borderColor = 'var(--color-border-strong)'}
            onBlur={e => e.currentTarget.style.borderColor = 'var(--color-border)'}
          />

          {error && (
            <p style={{
              fontSize: '11px', color: '#b91c1c',
              marginBottom: '8px', lineHeight: '1.4',
            }}>
              {error}
              <button
                onClick={() => setError(null)}
                style={{ marginLeft: '6px', background: 'none', border: 'none', cursor: 'pointer', color: '#b91c1c', fontSize: '11px' }}
              >✕</button>
            </p>
          )}

          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
            <button
              onClick={() => { setExpanded(false); setError(null) }}
              style={{
                padding: '5px 12px', fontSize: '12px',
                color: 'var(--color-text-secondary)', background: 'none',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)', cursor: 'pointer',
              }}
            >
              Cancel
            </button>
            <button
              onClick={handleFill}
              disabled={!description.trim() || loading}
              style={{
                padding: '5px 14px', fontSize: '12px', fontWeight: '500',
                color: 'var(--color-bg)', background: 'var(--color-accent)',
                border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                opacity: (!description.trim() || loading) ? 0.5 : 1,
                display: 'flex', alignItems: 'center', gap: '6px',
              }}
            >
              {loading ? (
                <>
                  <span style={{
                    width: '10px', height: '10px', border: '2px solid rgba(255,255,255,0.3)',
                    borderTopColor: '#fff', borderRadius: '50%',
                    display: 'inline-block', animation: 'spin 0.7s linear infinite',
                  }} />
                  Filling…
                </>
              ) : 'Fill form'}
            </button>
          </div>
        </div>
      )}

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  )
}
