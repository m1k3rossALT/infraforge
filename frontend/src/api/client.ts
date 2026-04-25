import type { FormState, ProviderSchema, ProviderSummary, SavedTemplate, TemplateSummary } from '../types/schema'

const BASE = '/api'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, options)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  // Handle 204 No Content (delete)
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

async function requestText(path: string, options?: RequestInit): Promise<string> {
  const res = await fetch(`${BASE}${path}`, options)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.text()
}

// ─── Provider API ─────────────────────────────────────────────────────────────

export const api = {
  listProviders: (): Promise<ProviderSummary[]> =>
    request('/providers'),

  getSchema: (providerId: string): Promise<ProviderSchema> =>
    request(`/providers/${providerId}/schema`),

  generate: (providerId: string, state: FormState): Promise<string> =>
    requestText(`/providers/${providerId}/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sections: state }),
    }),
}

// ─── Template API (v1) ────────────────────────────────────────────────────────

type SavePayload = {
  name: string
  providerId: string
  formState: FormState
  description?: string
  tags?: string[]
}

export const templateApi = {
  list: (providerId?: string): Promise<TemplateSummary[]> =>
    request(`/v1/templates${providerId ? `?providerId=${encodeURIComponent(providerId)}` : ''}`),

  getById: (id: string): Promise<SavedTemplate> =>
    request(`/v1/templates/${id}`),

  create: (payload: SavePayload): Promise<SavedTemplate> =>
    request('/v1/templates', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),

  update: (id: string, payload: SavePayload): Promise<SavedTemplate> =>
    request(`/v1/templates/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),

  delete: (id: string): Promise<void> =>
    request(`/v1/templates/${id}`, { method: 'DELETE' }),

  duplicate: (id: string): Promise<SavedTemplate> =>
    request(`/v1/templates/${id}/duplicate`, { method: 'POST' }),
}