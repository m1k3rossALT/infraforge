import type { ProviderSchema, FormValues } from '../types/schema'
import { FieldRenderer } from './FieldRenderer'

interface Props {
  schema: ProviderSchema
  values: FormValues
  onChange: (values: FormValues) => void
  onGenerate: () => void
}

export function SchemaForm({ schema, values, onChange, onGenerate }: Props) {
  const handleFieldChange = (fieldId: string, value: string) => {
    onChange({ ...values, [fieldId]: value })
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px' }}>
        {schema.sections.map(section => (
          <div key={section.id} style={{ marginBottom: '24px' }}>
            <p style={{
              fontSize: '11px',
              fontWeight: '500',
              color: 'var(--color-text-muted)',
              textTransform: 'uppercase',
              letterSpacing: '0.07em',
              marginBottom: '12px',
            }}>
              {section.label}
            </p>
            {section.fields.map(field => (
              <FieldRenderer
                key={field.id}
                field={field}
                value={values[field.id] ?? ''}
                onChange={v => handleFieldChange(field.id, v)}
              />
            ))}
          </div>
        ))}
      </div>

      <div style={{
        padding: '14px 20px',
        borderTop: '1px solid var(--color-border)',
        flexShrink: 0,
      }}>
        <button
          onClick={onGenerate}
          style={{
            width: '100%',
            padding: '8px 0',
            fontSize: '13px',
            fontWeight: '500',
            color: 'var(--color-bg)',
            background: 'var(--color-accent)',
            border: 'none',
            borderRadius: 'var(--radius-md)',
            cursor: 'pointer',
            transition: 'background 0.15s',
          }}
          onMouseEnter={e => (e.currentTarget.style.background = 'var(--color-accent-hover)')}
          onMouseLeave={e => (e.currentTarget.style.background = 'var(--color-accent)')}
        >
          Generate
        </button>
      </div>
    </div>
  )
}
