import { useEffect, useState } from 'react'
import type { Field } from '../types/schema'

interface Props {
  field: Field
  value: string
  onChange: (value: string) => void
  /** When true, briefly highlights the field to indicate AI has just filled it */
  highlighted?: boolean
}

export function FieldRenderer({ field, value, onChange, highlighted = false }: Props) {
  const [showHelp, setShowHelp] = useState(false)
  const [isHighlighted, setIsHighlighted] = useState(false)

  // Flash highlight when the highlighted prop becomes true
  useEffect(() => {
    if (highlighted) {
      setIsHighlighted(true)
      const timer = setTimeout(() => setIsHighlighted(false), 1800)
      return () => clearTimeout(timer)
    }
  }, [highlighted])

  const highlightStyle: React.CSSProperties = isHighlighted ? {
    outline: '2px solid var(--color-live)',
    borderRadius: 'var(--radius-md)',
    transition: 'outline 0.3s ease',
  } : {}

  const labelRow = (
    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: field.type === 'toggle' ? '0' : '5px' }}>
      <label style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
        {field.label}
        {field.required && <span style={{ color: 'var(--color-text-muted)', marginLeft: '2px' }}>*</span>}
        {isHighlighted && (
          <span style={{ marginLeft: '6px', fontSize: '10px', color: 'var(--color-live)' }}>✨</span>
        )}
      </label>
      {field.help && (
        <button
          onClick={() => setShowHelp(h => !h)}
          style={{
            width: '14px', height: '14px', fontSize: '9px',
            color: 'var(--color-text-muted)', background: 'none',
            border: '1px solid var(--color-border)', borderRadius: '50%',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            cursor: 'pointer', flexShrink: 0, lineHeight: 1,
          }}
        >?</button>
      )}
    </div>
  )

  const helpText = showHelp && field.help && (
    <p style={{
      fontSize: '11px', color: 'var(--color-text-secondary)',
      background: 'var(--color-surface)', border: '1px solid var(--color-border)',
      borderRadius: 'var(--radius-sm)', padding: '6px 8px', marginBottom: '6px', lineHeight: '1.5',
    }}>
      {field.help}
    </p>
  )

  if (field.type === 'toggle') {
    return (
      <div style={{ marginBottom: '14px', ...highlightStyle }}>
        {helpText}
        <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
              {field.label}
              {field.required && <span style={{ color: 'var(--color-text-muted)', marginLeft: '2px' }}>*</span>}
              {isHighlighted && <span style={{ marginLeft: '6px', fontSize: '10px', color: 'var(--color-live)' }}>✨</span>}
            </span>
            {field.help && (
              <button
                onClick={e => { e.preventDefault(); setShowHelp(h => !h) }}
                style={{
                  width: '14px', height: '14px', fontSize: '9px',
                  color: 'var(--color-text-muted)', background: 'none',
                  border: '1px solid var(--color-border)', borderRadius: '50%',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  cursor: 'pointer', flexShrink: 0, lineHeight: 1,
                }}
              >?</button>
            )}
          </div>
          <input
            type="checkbox"
            checked={value === 'true'}
            onChange={e => onChange(e.target.checked ? 'true' : 'false')}
            style={{ width: '15px', height: '15px', cursor: 'pointer', accentColor: 'var(--color-accent)' }}
          />
        </label>
      </div>
    )
  }

  if (field.type === 'select') {
    return (
      <div style={{ marginBottom: '14px', ...highlightStyle }}>
        {labelRow}
        {helpText}
        <select value={value} onChange={e => onChange(e.target.value)}>
          {!field.required && <option value="">— optional —</option>}
          {field.options?.map(opt => (
            <option key={opt} value={opt}>{opt}</option>
          ))}
        </select>
      </div>
    )
  }

  if (field.type === 'textarea') {
    return (
      <div style={{ marginBottom: '14px', ...highlightStyle }}>
        {labelRow}
        {helpText}
        <textarea
          value={value}
          onChange={e => onChange(e.target.value)}
          placeholder={field.placeholder}
          rows={4}
          style={{ resize: 'vertical', fontFamily: 'var(--font-mono)', fontSize: '12px' }}
        />
      </div>
    )
  }

  if (field.type === 'number') {
    return (
      <div style={{ marginBottom: '14px', ...highlightStyle }}>
        {labelRow}
        {helpText}
        <input
          type="number"
          value={value}
          onChange={e => onChange(e.target.value)}
          placeholder={field.placeholder}
          min="0"
        />
      </div>
    )
  }

  // default: text
  return (
    <div style={{ marginBottom: '14px', ...highlightStyle }}>
      {labelRow}
      {helpText}
      <input
        type="text"
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={field.placeholder}
      />
    </div>
  )
}
