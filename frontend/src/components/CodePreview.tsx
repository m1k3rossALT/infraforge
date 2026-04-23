import CodeMirror from '@uiw/react-codemirror'
import { useState } from 'react'

interface Props {
  code: string
  filename: string
  isLive: boolean
}

export function CodePreview({ code, filename, isLive }: Props) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  const handleDownload = () => {
    const blob = new Blob([code], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      background: 'var(--color-surface)',
    }}>
      {/* Header bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '10px 16px',
        borderBottom: '1px solid var(--color-border)',
        flexShrink: 0,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{
            fontSize: '12px',
            color: 'var(--color-text-secondary)',
            fontFamily: 'var(--font-mono)',
          }}>
            {filename}
          </span>
          {isLive && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
              <div style={{
                width: '6px', height: '6px',
                borderRadius: '50%',
                background: 'var(--color-live)',
              }} />
              <span style={{ fontSize: '10px', color: 'var(--color-live)', fontFamily: 'var(--font-mono)' }}>
                live
              </span>
            </div>
          )}
        </div>

        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            onClick={handleCopy}
            style={{
              padding: '3px 10px',
              fontSize: '11px',
              color: copied ? 'var(--color-live)' : 'var(--color-text-secondary)',
              background: 'none',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-sm)',
              cursor: 'pointer',
              transition: 'color 0.15s',
            }}
          >
            {copied ? 'Copied' : 'Copy'}
          </button>
          <button
            onClick={handleDownload}
            disabled={!code}
            style={{
              padding: '3px 10px',
              fontSize: '11px',
              color: 'var(--color-text-secondary)',
              background: 'none',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-sm)',
              cursor: code ? 'pointer' : 'not-allowed',
              opacity: code ? 1 : 0.4,
            }}
          >
            Download
          </button>
        </div>
      </div>

      {/* Code pane */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        {code ? (
          <CodeMirror
            value={code}
            editable={false}
            basicSetup={{
              lineNumbers: true,
              foldGutter: false,
              dropCursor: false,
              allowMultipleSelections: false,
              indentOnInput: false,
              highlightActiveLine: false,
            }}
            style={{
              fontSize: '12px',
              fontFamily: 'var(--font-mono)',
              height: '100%',
            }}
          />
        ) : (
          <div style={{
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--color-text-muted)',
            fontSize: '12px',
            fontFamily: 'var(--font-mono)',
          }}>
            Fill in the form to generate a template
          </div>
        )}
      </div>
    </div>
  )
}
