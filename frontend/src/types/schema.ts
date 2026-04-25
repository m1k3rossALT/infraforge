export type FieldType = 'text' | 'select' | 'textarea' | 'toggle' | 'number'

export interface Field {
  id: string
  label: string
  type: FieldType
  options?: string[]
  required: boolean
  placeholder?: string
  help?: string
  defaultValue?: string
}

export interface Section {
  id: string
  label: string
  optional: boolean
  defaultEnabled: boolean
  repeatable: boolean
  maxInstances: number
  fields: Field[]
}

export interface ProviderSchema {
  id: string
  label: string
  version: string
  fileExtension: string
  sections: Section[]
}

export interface ProviderSummary {
  id: string
  label: string
}

// Per-instance field values (flat map of fieldId → string)
export type InstanceValues = Record<string, string>

// State for one section: enabled flag + list of instances
export interface SectionState {
  enabled: boolean
  instances: InstanceValues[]
}

// Full form state: sectionId → SectionState
export type FormState = Record<string, SectionState>

// ─── Template management types ───────────────────────────────────────────────

export interface TemplateSummary {
  id: string
  name: string
  providerId: string
  description?: string
  tags?: string[]
  updatedAt: string
}

export interface SavedTemplate {
  id: string
  name: string
  providerId: string
  formState: FormState
  description?: string
  tags?: string[]
  updatedAt: string
  generatedAt: string
}