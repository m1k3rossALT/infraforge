import type {
  FormState,
  ProviderSchema,
  ProviderSummary,
  SavedTemplate,
  SharedTemplate,
  TemplateSummary,
} from '../types/schema'

const BASE = '/api/v1'

// ─── Token providers — set by AuthContext after login ─────────────────────────

let getAccessToken: () => string | null = () => null
let doRefresh: (() => Promise<string | null>) | null = null

export function setTokenProvider(provider: () => string | null) {
  getAccessToken = provider
}

/**
 * Set the refresh function from AuthContext.
 * When a request returns 401, the client will call this once to get a new
 * access token and retry the original request transparently.
 * If refresh fails (token expired/revoked), AuthContext clears auth state
 * and the retry 401 is surfaced as a normal error.
 */
export function setRefreshProvider(provider: () => Promise<string | null>) {
  doRefresh = provider
}

function authHeaders(token?: string | null): HeadersInit {
  const t = token ?? getAccessToken()
  return t ? { 'Authorization': `Bearer ${t}` } : {}
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      ...authHeaders(),
      ...(options?.headers ?? {}),
    },
  })

  // Silent refresh on 401 — attempt once, then retry the original request
  if (res.status === 401 && doRefresh) {
    const newToken = await doRefresh()
    if (newToken) {
      const retryRes = await fetch(`${BASE}${path}`, {
        ...options,
        headers: {
          ...authHeaders(newToken),
          ...(options?.headers ?? {}),
        },
      })
      if (!retryRes.ok) throw new Error(`${retryRes.status} ${retryRes.statusText}`)
      if (retryRes.status === 204) return undefined as T
      return retryRes.json() as Promise<T>
    }
    // Refresh failed — AuthContext already cleared auth state; surface the error
  }

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
    return request('/templates/import', {
      method: 'POST',
      headers: { ...authHeaders() },
      body: form,
    })
  },
}

// ─── Share API ────────────────────────────────────────────────────────────────

export const shareApi = {
  /**
   * Generate a share token for a template. Idempotent.
   * Returns { shareToken, shareUrl } from the backend.
   */
  share: (id: string): Promise<{ shareToken: string; shareUrl: string }> =>
    request(`/templates/${id}/share`, { method: 'POST' }),

  /**
   * Revoke the share token. After this the share URL returns 404.
   */
  unshare: (id: string): Promise<void> =>
    request(`/templates/${id}/share`, { method: 'DELETE' }),

  /**
   * Fetch a shared template by token. No auth required.
   * Used by SharedView — called without the Authorization header.
   */
  getShared: async (token: string): Promise<SharedTemplate> => {
    const res = await fetch(`${BASE}/shared/${token}`)
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
    return res.json()
  },
}