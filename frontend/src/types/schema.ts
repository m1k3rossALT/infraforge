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
  aiHint?: string
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

// ─── Template types ───────────────────────────────────────────────────────────

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

// ─── Prebuilt template types ──────────────────────────────────────────────────

/**
 * A curated read-only template loaded from the backend's resources/prebuilts/*.json.
 * Users can fork a prebuilt into their personal library via the Fork button.
 */
export interface PrebuiltTemplate {
  id: string
  name: string
  description?: string
  providerId: string
  tags?: string[]
  formState: FormState
}

// ─── Shared view type ─────────────────────────────────────────────────────────

export interface SharedTemplate {
  name: string
  providerId: string
  generatedCode: string
}

// ─── AI types ─────────────────────────────────────────────────────────────────

export interface AiSettings {
  aiProvider: string | null
  hasApiKey: boolean
  aiModel: string | null
}

export type AiSuggestions = Record<string, Record<string, string>>