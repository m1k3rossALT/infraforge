import type { ProviderSchema, FormState, Section, InstanceValues } from '../types/schema'
import { FieldRenderer } from './FieldRenderer'

interface Props {
  schema: ProviderSchema
  state: FormState
  onChange: (state: FormState) => void
  onGenerate: () => void
  /** Set of fieldIds filled by AI — triggers brief highlight on the affected fields */
  highlightedFields?: Set<string>
}

function defaultInstance(section: Section): InstanceValues {
  const inst: InstanceValues = {}
  for (const field of section.fields) {
    inst[field.id] = field.defaultValue ?? ''
  }
  return inst
}

export function SchemaForm({ schema, state, onChange, onGenerate, highlightedFields }: Props) {

  const toggleSection = (sectionId: string, enabled: boolean) => {
    onChange({ ...state, [sectionId]: { ...state[sectionId], enabled } })
  }

  const updateField = (sectionId: string, index: number, fieldId: string, value: string) => {
    const sec = state[sectionId]
    const instances = [...sec.instances]
    instances[index] = { ...instances[index], [fieldId]: value }
    onChange({ ...state, [sectionId]: { ...sec, instances } })
  }

  const addInstance = (sectionId: string, sectionDef: Section) => {
    const sec = state[sectionId]
    if (sectionDef.maxInstances > 0 && sec.instances.length >= sectionDef.maxInstances) return
    onChange({ ...state, [sectionId]: { ...sec, instances: [...sec.instances, defaultInstance(sectionDef)] } })
  }

  const removeInstance = (sectionId: string, index: number) => {
    const sec = state[sectionId]
    if (sec.instances.length <= 1) return
    onChange({ ...state, [sectionId]: { ...sec, instances: sec.instances.filter((_, i) => i !== index) } })
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px' }}>
        {schema.sections.map(section => {
          const sec = state[section.id]
          if (!sec) return null
          const isEnabled = sec.enabled

          return (
            <div key={section.id} style={{
              marginBottom: '4px',
              borderBottom: '1px solid var(--color-border)',
              paddingBottom: '16px',
              marginTop: '16px',
            }}>
              {/* Section header */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: isEnabled ? '14px' : '0' }}>
                <span style={{
                  fontSize: '11px', fontWeight: '500',
                  color: isEnabled ? 'var(--color-text-secondary)' : 'var(--color-text-muted)',
                  textTransform: 'uppercase', letterSpacing: '0.07em',
                }}>
                  {section.label}
                </span>
                {section.optional && (
                  <button
                    onClick={() => toggleSection(section.id, !isEnabled)}
                    style={{
                      fontSize: '11px',
                      color: isEnabled ? 'var(--color-text-secondary)' : 'var(--color-text-muted)',
                      background: 'none',
                      border: '1px solid var(--color-border)',
                      borderRadius: '4px',
                      padding: '2px 8px',
                      cursor: 'pointer',
                    }}
                  >
                    {isEnabled ? '− Remove' : '+ Add'}
                  </button>
                )}
              </div>

              {/* Section content */}
              {isEnabled && (
                <>
                  {sec.instances.map((instance, index) => (
                    <div key={index} style={section.repeatable ? {
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      padding: '12px 14px',
                      marginBottom: '10px',
                      background: 'var(--color-surface)',
                    } : {}}>

                      {section.repeatable && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                          <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                            {section.label} {index + 1}
                          </span>
                          {sec.instances.length > 1 && (
                            <button
                              onClick={() => removeInstance(section.id, index)}
                              style={{
                                fontSize: '11px', color: 'var(--color-text-muted)',
                                background: 'none', border: 'none', cursor: 'pointer', padding: '0',
                              }}
                            >
                              Remove
                            </button>
                          )}
                        </div>
                      )}

                      {section.fields.map(field => (
                        <FieldRenderer
                          key={field.id}
                          field={field}
                          value={instance[field.id] ?? ''}
                          onChange={v => updateField(section.id, index, field.id, v)}
                          highlighted={highlightedFields?.has(field.id) ?? false}
                        />
                      ))}
                    </div>
                  ))}

                  {section.repeatable && (
                    <button
                      onClick={() => addInstance(section.id, section)}
                      disabled={section.maxInstances > 0 && sec.instances.length >= section.maxInstances}
                      style={{
                        width: '100%', padding: '6px 0', fontSize: '12px',
                        color: 'var(--color-text-muted)', background: 'none',
                        border: '1px dashed var(--color-border)',
                        borderRadius: 'var(--radius-md)', cursor: 'pointer', marginTop: '4px',
                      }}
                    >
                      + Add {section.label}
                    </button>
                  )}
                </>
              )}
            </div>
          )
        })}
      </div>

      {/* Generate button */}
      <div style={{ padding: '14px 20px', borderTop: '1px solid var(--color-border)', flexShrink: 0 }}>
        <button
          onClick={onGenerate}
          style={{
            width: '100%', padding: '8px 0', fontSize: '13px', fontWeight: '500',
            color: 'var(--color-bg)', background: 'var(--color-accent)',
            border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
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
