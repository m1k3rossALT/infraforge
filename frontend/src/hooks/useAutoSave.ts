import { useEffect, useRef, useState } from 'react'
import { templateApi } from '../api/client'
import type { FormState, SavedTemplate } from '../types/schema'

export type SaveStatus = 'idle' | 'saving' | 'saved' | 'error'

interface UseAutoSaveOptions {
  templateId: string | null
  templateName: string
  providerId: string | null
  formState: FormState
  onSaved: (template: SavedTemplate) => void
  delayMs?: number
  enabled?: boolean  // When false, auto-save is disabled (e.g. unauthenticated)
}

/**
 * Auto-saves the current template when:
 *   1. The template has a name (unnamed templates are never auto-saved)
 *   2. The form state has not changed for `delayMs` milliseconds (default 30s)
 *
 * Returns the current save status for display in the UI.
 */
export function useAutoSave({
  templateId,
  templateName,
  providerId,
  formState,
  onSaved,
  delayMs = 30000,
  enabled = true,
}: UseAutoSaveOptions): SaveStatus {
  const [status, setStatus] = useState<SaveStatus>('idle')
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastSavedStateRef = useRef<string>('')

  useEffect(() => {
    // Only auto-save when enabled and a name has been given
    if (enabled === false || !templateName.trim() || !providerId) return

    const currentState = JSON.stringify(formState)

    // Skip if nothing changed since last save
    if (currentState === lastSavedStateRef.current) return

    // Clear any existing timer — reset the inactivity window
    if (timerRef.current) clearTimeout(timerRef.current)

    setStatus('idle')

    timerRef.current = setTimeout(async () => {
      setStatus('saving')
      try {
        const payload = { name: templateName, providerId, formState }
        const saved = templateId
          ? await templateApi.update(templateId, payload)
          : await templateApi.create(payload)

        lastSavedStateRef.current = currentState
        setStatus('saved')
        onSaved(saved)

        // Reset to idle after 3 seconds
        setTimeout(() => setStatus('idle'), 3000)
      } catch {
        setStatus('error')
        setTimeout(() => setStatus('idle'), 4000)
      }
    }, delayMs)

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  }, [formState, templateName, providerId, templateId, delayMs, onSaved])

  return status
}