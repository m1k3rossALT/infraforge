export type FieldType = 'text' | 'select' | 'textarea'

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

export type FormValues = Record<string, string>
