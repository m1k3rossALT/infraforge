import { useEffect, useState } from 'react'
import { aiApi } from '../api/client'
import type { AiSettings } from '../types/schema'

interface Props {
  open: boolean
  onClose: () => void
  userEmail: string | undefined
  onAiSettingsChange: (settings: AiSettings | null) => void
  initialTab?: 'profile' | 'ai'
}

type Tab = 'profile' | 'ai'

const AI_PROVIDERS = [
  { value: 'gemini',    label: 'Google Gemini',    models: ['gemini-2.0-flash', 'gemini-1.5-pro'] },
  { value: 'openai',   label: 'OpenAI',            models: ['gpt-4o-mini', 'gpt-4o'] },
  { value: 'anthropic',label: 'Anthropic Claude',  models: ['claude-haiku-4-5-20251001', 'claude-sonnet-4-6'] },
  { value: 'mistral',  label: 'Mistral',           models: ['mistral-small-latest', 'mistral-large-latest'] },
  { value: 'groq',     label: 'Groq (Llama)',      models: ['llama-3.3-70b-versatile', 'llama-3.1-8b-instant'] },
]

/**
 * Right-side settings drawer.
 *
 * Structure: tabbed (Profile | AI Provider).
 * Adding new tabs in the future only requires adding to the Tab type and
 * rendering a new tab button + content section — no structural changes.
 *
 * Mirrors TemplateDrawer in animation and layout for consistency.
 */
export function SettingsDrawer({ open, onClose, userEmail, onAiSettingsChange, initialTab = 'profile' }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>(initialTab)

  // AI settings state
  const [aiSettings, setAiSettings] = useState<AiSettings | null>(null)
  const [selectedProvider, setSelectedProvider] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [selectedModel, setSelectedModel] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saved' | 'error'>('idle')
  const [removing, setRemoving] = useState(false)
  const [testStatus, setTestStatus] = useState<'idle' | 'testing' | 'ok' | 'fail'>('idle')

  // Sync initialTab when it changes (e.g. AiBar opens settings at AI tab)
  useEffect(() => {
    if (open) setActiveTab(initialTab)
  }, [open, initialTab])

  // Load AI settings when drawer opens
  useEffect(() => {
    if (!open) return
    aiApi.getSettings()
      .then(s => {
        setAiSettings(s)
        if (s.aiProvider) setSelectedProvider(s.aiProvider)
        if (s.aiModel) setSelectedModel(s.aiModel)
      })
      .catch(() => setAiSettings(null))
  }, [open])

  const providerDef = AI_PROVIDERS.find(p => p.value === selectedProvider)

  const handleSaveAi = async () => {
    if (!selectedProvider || !apiKey.trim()) return
    setSaving(true)
    setSaveStatus('idle')
    try {
      const updated = await aiApi.saveSettings(selectedProvider, apiKey.trim(), selectedModel || undefined)
      setAiSettings(updated)
      onAiSettingsChange(updated)
      setApiKey('')  // clear from UI — never display raw key after save
      setSaveStatus('saved')
      setTimeout(() => setSaveStatus('idle'), 2500)
    } catch {
      setSaveStatus('error')
    } finally {
      setSaving(false)
    }
  }

  const handleRemoveAi = async () => {
    setRemoving(true)
    try {
      await aiApi.deleteSettings()
      setAiSettings(null)
      onAiSettingsChange(null)
      setSelectedProvider('')
      setSelectedModel('')
      setApiKey('')
    } catch {
      // silent — settings still shown
    } finally {
      setRemoving(false)
    }
  }

  const handleTestConnection = async () => {
    if (!aiSettings?.hasApiKey) return
    setTestStatus('testing')
    try {
      // A minimal suggest call — empty suggestions = connected fine
      await aiApi.suggest('terraform', 'test connection')
      setTestStatus('ok')
      setTimeout(() => setTestStatus('idle'), 3000)
    } catch {
      setTestStatus('fail')
      setTimeout(() => setTestStatus('idle'), 3000)
    }
  }

  return (
    <>
      {/* Backdrop */}
      {open && (
        <div
          onClick={onClose}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.15)', zIndex: 40 }}
        />
      )}

      {/* Drawer */}
      <div style={{
        position: 'fixed', top: 0, right: 0, bottom: 0, width: '340px',
        background: 'var(--color-bg)',
        borderLeft: '1px solid var(--color-border)',
        zIndex: 50, display: 'flex', flexDirection: 'column',
        transform: open ? 'translateX(0)' : 'translateX(100%)',
        transition: 'transform 0.22s ease',
        boxShadow: open ? '-4px 0 16px rgba(0,0,0,0.08)' : 'none',
      }}>

        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '12px 16px', borderBottom: '1px solid var(--color-border)', flexShrink: 0,
        }}>
          <span style={{ fontSize: '13px', fontWeight: '500' }}>Settings</span>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px', color: 'var(--color-text-muted)', lineHeight: 1 }}>×</button>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', borderBottom: '1px solid var(--color-border)', flexShrink: 0 }}>
          {(['profile', 'ai'] as Tab[]).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                flex: 1, padding: '9px 0', fontSize: '12px',
                color: activeTab === tab ? 'var(--color-text-primary)' : 'var(--color-text-muted)',
                background: 'none', border: 'none',
                borderBottom: activeTab === tab ? '2px solid var(--color-accent)' : '2px solid transparent',
                cursor: 'pointer', fontWeight: activeTab === tab ? '500' : '400',
                textTransform: 'capitalize',
              }}
            >
              {tab === 'ai' ? '✨ AI Provider' : 'Profile'}
            </button>
          ))}
        </div>

        {/* Content */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '20px 16px' }}>

          {/* ── Profile tab ─────────────────────────────────────── */}
          {activeTab === 'profile' && (
            <div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ fontSize: '11px', color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.07em', display: 'block', marginBottom: '6px' }}>
                  Email
                </label>
                <p style={{ fontSize: '13px', color: 'var(--color-text-primary)', padding: '7px 0' }}>
                  {userEmail ?? '—'}
                </p>
              </div>

              <div style={{
                padding: '12px', background: 'var(--color-surface)',
                border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)',
                fontSize: '12px', color: 'var(--color-text-muted)',
              }}>
                Change password and additional profile settings coming soon.
              </div>
            </div>
          )}

          {/* ── AI Provider tab ──────────────────────────────────── */}
          {activeTab === 'ai' && (
            <div>
              {/* Current status */}
              {aiSettings?.hasApiKey ? (
                <div style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '10px 12px', marginBottom: '20px',
                  background: 'var(--color-surface)', border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-md)',
                }}>
                  <div>
                    <span style={{ fontSize: '12px', color: 'var(--color-live)' }}>● </span>
                    <span style={{ fontSize: '12px', color: 'var(--color-text-primary)' }}>
                      {AI_PROVIDERS.find(p => p.value === aiSettings.aiProvider)?.label ?? aiSettings.aiProvider}
                    </span>
                    {aiSettings.aiModel && (
                      <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginLeft: '8px' }}>
                        {aiSettings.aiModel}
                      </span>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: '6px' }}>
                    <button
                      onClick={handleTestConnection}
                      disabled={testStatus === 'testing'}
                      style={{
                        padding: '3px 8px', fontSize: '11px',
                        color: testStatus === 'ok' ? 'var(--color-live)' : testStatus === 'fail' ? '#b91c1c' : 'var(--color-text-secondary)',
                        background: 'none', border: '1px solid var(--color-border)',
                        borderRadius: '4px', cursor: 'pointer',
                      }}
                    >
                      {testStatus === 'testing' ? 'Testing…' : testStatus === 'ok' ? '✓ Connected' : testStatus === 'fail' ? '✕ Failed' : 'Test'}
                    </button>
                    <button
                      onClick={handleRemoveAi}
                      disabled={removing}
                      style={{
                        padding: '3px 8px', fontSize: '11px', color: '#b91c1c',
                        background: 'none', border: '1px solid #fecaca',
                        borderRadius: '4px', cursor: 'pointer',
                      }}
                    >
                      {removing ? 'Removing…' : 'Remove'}
                    </button>
                  </div>
                </div>
              ) : (
                <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '20px' }}>
                  Add your API key to enable AI-assisted form filling.
                </p>
              )}

              {/* Provider selector */}
              <div style={{ marginBottom: '14px' }}>
                <label style={labelStyle}>AI Provider</label>
                <select
                  value={selectedProvider}
                  onChange={e => { setSelectedProvider(e.target.value); setSelectedModel('') }}
                  style={{ width: '100%' }}
                >
                  <option value="">— Select provider —</option>
                  {AI_PROVIDERS.map(p => (
                    <option key={p.value} value={p.value}>{p.label}</option>
                  ))}
                </select>
              </div>

              {/* API key input */}
              {selectedProvider && (
                <div style={{ marginBottom: '14px' }}>
                  <label style={labelStyle}>
                    API Key
                    {aiSettings?.hasApiKey && aiSettings.aiProvider === selectedProvider && (
                      <span style={{ color: 'var(--color-text-muted)', fontWeight: '400', marginLeft: '6px' }}>
                        (enter new key to replace)
                      </span>
                    )}
                  </label>
                  <input
                    type="password"
                    value={apiKey}
                    onChange={e => setApiKey(e.target.value)}
                    placeholder={aiSettings?.hasApiKey ? '••••••••••••••••' : 'Paste your API key…'}
                    style={{ width: '100%', fontFamily: 'var(--font-mono)', fontSize: '12px' }}
                    autoComplete="off"
                  />
                  <p style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '4px' }}>
                    Stored encrypted. Never returned after saving.
                  </p>
                </div>
              )}

              {/* Model selector */}
              {providerDef && (
                <div style={{ marginBottom: '20px' }}>
                  <label style={labelStyle}>Model <span style={{ fontWeight: '400', color: 'var(--color-text-muted)' }}>(optional)</span></label>
                  <select
                    value={selectedModel}
                    onChange={e => setSelectedModel(e.target.value)}
                    style={{ width: '100%' }}
                  >
                    <option value="">— Use provider default —</option>
                    {providerDef.models.map(m => (
                      <option key={m} value={m}>{m}</option>
                    ))}
                  </select>
                </div>
              )}

              {/* Save button */}
              {selectedProvider && (
                <button
                  onClick={handleSaveAi}
                  disabled={saving || !apiKey.trim()}
                  style={{
                    width: '100%', padding: '8px 0', fontSize: '13px', fontWeight: '500',
                    color: 'var(--color-bg)', background: 'var(--color-accent)',
                    border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                    opacity: (saving || !apiKey.trim()) ? 0.5 : 1,
                  }}
                >
                  {saving ? 'Saving…' : saveStatus === 'saved' ? '✓ Saved' : saveStatus === 'error' ? 'Save failed — try again' : 'Save API key'}
                </button>
              )}

              {/* Groq free tier note */}
              {selectedProvider === 'groq' && (
                <p style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '10px', lineHeight: '1.5' }}>
                  Groq offers a free tier with generous limits — a good option to try AI at no cost.
                </p>
              )}
            </div>
          )}
        </div>
      </div>
    </>
  )
}

const labelStyle: React.CSSProperties = {
  fontSize: '11px', color: 'var(--color-text-muted)',
  textTransform: 'uppercase', letterSpacing: '0.07em',
  display: 'block', marginBottom: '6px', fontWeight: '500',
}
