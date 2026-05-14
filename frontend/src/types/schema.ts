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
  aiHint?: string
}

export interface Section {
  id: string
  label: string
  optional: boolean
  defaultEnabled: boolean
  repeatable: boolean
  maxInstances: number
  fields: Field[]
  aiHint?: string  // natural language description of what this section configures
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

export type InstanceValues = Record<string, string>

export interface SectionState {
  enabled: boolean
  instances: InstanceValues[]
}

export type FormState = Record<string, SectionState>

// ─── Template management types ───────────────────────────────────────────────

export interface TemplateSummary {
  id: string
  name: string
  providerId: string
  description?: string
  tags?: string[]
  updatedAt: string
  shareToken?: string
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
  shareToken?: string
}

// ─── Shared view type ────────────────────────────────────────────────────────

export interface SharedTemplate {
  name: string
  providerId: string
  generatedCode: string
}

// ─── AI types ────────────────────────────────────────────────────────────────

export interface AiSettings {
  aiProvider: string | null
  hasApiKey: boolean
  aiModel: string | null
}

/** Nested suggestions from POST /api/v1/ai/suggest/{providerId} */
export type AiSuggestions = Record<string, Record<string, string>>