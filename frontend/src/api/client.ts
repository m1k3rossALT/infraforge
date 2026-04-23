import type { FormValues, ProviderSchema, ProviderSummary } from '../types/schema'

const BASE = '/api'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, options)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json() as Promise<T>
}

async function requestText(path: string, options?: RequestInit): Promise<string> {
  const res = await fetch(`${BASE}${path}`, options)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.text()
}

export const api = {
  listProviders: (): Promise<ProviderSummary[]> =>
    request('/providers'),

  getSchema: (providerId: string): Promise<ProviderSchema> =>
    request(`/providers/${providerId}/schema`),

  generate: (providerId: string, values: FormValues): Promise<string> =>
    requestText(`/providers/${providerId}/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ values }),
    }),
}
