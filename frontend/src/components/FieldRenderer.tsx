import { useState } from 'react'
import type { Field } from '../types/schema'

interface Props {
  field: Field
  value: string
  onChange: (value: string) => void
}

export function FieldRenderer({ field, value, onChange }: Props) {
  const [showHelp, setShowHelp] = useState(false)

  return (
    <div style={{ marginBottom: '14px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '5px' }}>
        <label style={{
          fontSize: '12px',
          color: 'var(--color-text-secondary)',
          fontWeight: '400',
        }}>
          {field.label}
          {field.required && (
            <span style={{ color: 'var(--color-text-muted)', marginLeft: '2px' }}>*</span>
          )}
        </label>
        {field.help && (
          <button
            onClick={() => setShowHelp(h => !h)}
            title="Toggle help"
            style={{
              width: '14px',
              height: '14px',
              fontSize: '9px',
              color: 'var(--color-text-muted)',
              background: 'none',
              border: '1px solid var(--color-border)',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              flexShrink: 0,
              lineHeight: 1,
            }}
          >
            ?
          </button>
        )}
      </div>

      {showHelp && field.help && (
        <p style={{
          fontSize: '11px',
          color: 'var(--color-text-secondary)',
          background: 'var(--color-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-sm)',
          padding: '6px 8px',
          marginBottom: '6px',
          lineHeight: '1.5',
        }}>
          {field.help}
        </p>
      )}

      {field.type === 'select' ? (
        <select value={value} onChange={e => onChange(e.target.value)}>
          {!field.required && <option value="">— optional —</option>}
          {field.options?.map(opt => (
            <option key={opt} value={opt}>{opt}</option>
          ))}
        </select>
      ) : field.type === 'textarea' ? (
        <textarea
          value={value}
          onChange={e => onChange(e.target.value)}
          placeholder={field.placeholder}
          rows={4}
          style={{ resize: 'vertical' }}
        />
      ) : (
        <input
          type="text"
          value={value}
          onChange={e => onChange(e.target.value)}
          placeholder={field.placeholder}
        />
      )}
    </div>
  )
}
