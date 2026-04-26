import { useEffect, useState } from 'react'
import { shareApi } from '../api/client'
import type { SharedTemplate } from '../types/schema'

/**
 * Public read-only view for shared templates.
 * Rendered when the URL matches /shared/:token.
 * No auth required — access is controlled by the share token itself.
 *
 * Deliberately minimal: shows the generated code only.
 * The recipient can copy it, download it, or follow the CTA to build their own.
 */
export function SharedView() {
  const token = window.location.pathname.split('/shared/')[1]?.split('/')[0]

  const [template, setTemplate] = useState<SharedTemplate | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!token) { setNotFound(true); setLoading(false); return }

    shareApi.getShared(token)
      .then(setTemplate)
      .catch(e => {
        if (e.message.startsWith('404')) setNotFound(true)
        else setNotFound(true) // treat any error as not found for the public view
      })
      .finally(() => setLoading(false))
  }, [token])

  const handleCopy = async () => {
    if (!template) return
    await navigator.clipboard.writeText(template.generatedCode)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleDownload = () => {
    if (!template) return
    const blob = new Blob([template.generatedCode], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = template.name.replace(/[^a-zA-Z0-9._-]/g, '_') + getExtension(template.providerId)
    a.click()
    URL.revokeObjectURL(url)
  }

  if (loading) {
    return (
      <div style={styles.centered}>
        <span style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>Loading…</span>
      </div>
    )
  }

  if (notFound || !template) {
    return (
      <div style={styles.centered}>
        <div style={{ textAlign: 'center' }}>
          <p style={{ fontSize: '14px', color: 'var(--color-text-primary)', marginBottom: '8px' }}>
            This shared template is no longer available.
          </p>
          <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '20px' }}>
            The link may have been revoked or the template deleted.
          </p>
          <a href="/" style={styles.ctaButton}>Build your own with InfraForge →</a>
        </div>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden', background: 'var(--color-bg)' }}>

      {/* Header */}
      <header style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '8px 16px', borderBottom: '1px solid var(--color-border)',
        flexShrink: 0, gap: '12px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
          <span style={{ fontSize: '13px', fontWeight: '500', flexShrink: 0 }}>InfraForge</span>
          <span style={{ fontSize: '12px', color: 'var(--color-text-muted)', flexShrink: 0 }}>·</span>
          <span style={{ fontSize: '13px', color: 'var(--color-text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {template.name}
          </span>
          <span style={{
            fontSize: '10px', color: 'var(--color-text-muted)',
            border: '1px solid var(--color-border)', borderRadius: '3px',
            padding: '1px 5px', flexShrink: 0,
          }}>
            {template.providerId}
          </span>
          <span style={{
            fontSize: '10px', color: 'var(--color-live)',
            border: '1px solid var(--color-live)', borderRadius: '3px',
            padding: '1px 5px', flexShrink: 0,
          }}>
            Shared view
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
          <button
            onClick={handleCopy}
            style={styles.actionButton}
          >
            {copied ? '✓ Copied' : 'Copy'}
          </button>
          <button
            onClick={handleDownload}
            style={styles.actionButton}
          >
            Download
          </button>
          <a href="/" style={styles.ctaButton}>
            Build your own →
          </a>
        </div>
      </header>

      {/* Code view */}
      <main style={{ flex: 1, overflow: 'auto', padding: '0' }}>
        <pre style={{
          margin: 0,
          padding: '20px 24px',
          fontSize: '13px',
          lineHeight: '1.6',
          fontFamily: 'var(--font-mono, "JetBrains Mono", "Fira Code", monospace)',
          color: 'var(--color-text-primary)',
          whiteSpace: 'pre',
          overflowX: 'auto',
        }}>
          {template.generatedCode}
        </pre>
      </main>
    </div>
  )
}

function getExtension(providerId: string): string {
  const map: Record<string, string> = {
    terraform: '.tf',
    ansible: '.yml',
    vagrant: 'file', // Vagrantfile has no dot extension
  }
  return map[providerId] ?? '.txt'
}

const styles = {
  centered: {
    height: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  } as React.CSSProperties,

  actionButton: {
    padding: '4px 10px',
    fontSize: '12px',
    color: 'var(--color-text-secondary)',
    background: 'none',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-md)',
    cursor: 'pointer',
  } as React.CSSProperties,

  ctaButton: {
    padding: '4px 12px',
    fontSize: '12px',
    fontWeight: '500' as const,
    color: 'var(--color-bg)',
    background: 'var(--color-accent)',
    border: 'none',
    borderRadius: 'var(--radius-md)',
    cursor: 'pointer',
    textDecoration: 'none',
    display: 'inline-block',
  } as React.CSSProperties,
}
