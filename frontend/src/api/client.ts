import type {
  FormState,
  ProviderSchema,
  ProviderSummary,
  SavedTemplate,
  TemplateSummary,
} from '../types/schema'

const BASE = '/api/v1'

// Token provider — set by AuthContext after login
let getAccessToken: (() => string | null) = () => null

export function setTokenProvider(provider: () => string | null) {
  getAccessToken = provider
}

function authHeaders(): HeadersInit {
  const token = getAccessToken()
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      ...authHeaders(),
      ...(options?.headers ?? {}),
    },
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

async function requestText(path: string, options?: RequestInit): Promise<string> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: { ...authHeaders(), ...(options?.headers ?? {}) },
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.text()
}

async function requestBlob(path: string, options?: RequestInit): Promise<Blob> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: { ...authHeaders(), ...(options?.headers ?? {}) },
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.blob()
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

// ─── Template API ─────────────────────────────────────────────────────────────

type SavePayload = {
  name: string
  providerId: string
  formState: FormState
  description?: string
  tags?: string[]
}

export const templateApi = {
  list: (providerId?: string): Promise<TemplateSummary[]> =>
    request(`/templates${providerId ? `?providerId=${encodeURIComponent(providerId)}` : ''}`),

  getById: (id: string): Promise<SavedTemplate> =>
    request(`/templates/${id}`),

  create: (payload: SavePayload): Promise<SavedTemplate> =>
    request('/templates', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),

  update: (id: string, payload: SavePayload): Promise<SavedTemplate> =>
    request(`/templates/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),

  delete: (id: string): Promise<void> =>
    request(`/templates/${id}`, { method: 'DELETE' }),

  duplicate: (id: string): Promise<SavedTemplate> =>
    request(`/templates/${id}/duplicate`, { method: 'POST' }),

  export: async (id: string, name: string): Promise<void> => {
    const blob = await requestBlob(`/templates/${id}/export`)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${name.replace(/[^a-zA-Z0-9._-]/g, '_')}.zip`
    a.click()
    URL.revokeObjectURL(url)
  },

  import: (file: File, name?: string): Promise<SavedTemplate> => {
    const form = new FormData()
    form.append('file', file)
    if (name) form.append('name', name)
    // Don't set Content-Type — let browser set multipart boundary
    return request('/templates/import', {
      method: 'POST',
      headers: { ...authHeaders() },
      body: form,
    })
  },
}