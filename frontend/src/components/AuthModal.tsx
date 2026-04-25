import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'

interface Props {
  open: boolean
  onClose: () => void
}

type Mode = 'login' | 'register'

export function AuthModal({ open, onClose }: Props) {
  const { login, register } = useAuth()
  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (!open) return null

  const handleSubmit = async () => {
    if (!email.trim() || !password.trim()) return
    setError(null)
    setLoading(true)
    try {
      if (mode === 'login') {
        await login(email, password)
      } else {
        await register(email, password)
      }
      setEmail('')
      setPassword('')
      onClose()
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {/* Backdrop */}
      <div
        onClick={onClose}
        style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.2)', zIndex: 60 }}
      />

      {/* Modal */}
      <div style={{
        position: 'fixed', top: '50%', left: '50%',
        transform: 'translate(-50%, -50%)',
        width: '360px', background: 'var(--color-bg)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: '0 8px 32px rgba(0,0,0,0.12)',
        zIndex: 61, padding: '24px',
      }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '0' }}>
            {(['login', 'register'] as Mode[]).map((m, i) => (
              <button
                key={m}
                onClick={() => { setMode(m); setError(null) }}
                style={{
                  padding: '5px 14px', fontSize: '12px', cursor: 'pointer',
                  fontWeight: mode === m ? '500' : '400',
                  color: mode === m ? 'var(--color-text-primary)' : 'var(--color-text-secondary)',
                  background: mode === m ? 'var(--color-surface)' : 'var(--color-bg)',
                  border: '1px solid var(--color-border)',
                  borderLeft: i === 0 ? '1px solid var(--color-border)' : 'none',
                  borderRadius: i === 0 ? 'var(--radius-md) 0 0 var(--radius-md)' : '0 var(--radius-md) var(--radius-md) 0',
                }}
              >
                {m === 'login' ? 'Sign in' : 'Create account'}
              </button>
            ))}
          </div>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px', color: 'var(--color-text-muted)' }}>×</button>
        </div>

        {/* Fields */}
        <div style={{ marginBottom: '12px' }}>
          <label style={{ fontSize: '12px', color: 'var(--color-text-secondary)', display: 'block', marginBottom: '5px' }}>Email</label>
          <input
            type="email"
            value={email}
            onChange={e => setEmail(e.target.value)}
            placeholder="you@example.com"
            autoFocus
            onKeyDown={e => e.key === 'Enter' && handleSubmit()}
          />
        </div>

        <div style={{ marginBottom: '16px' }}>
          <label style={{ fontSize: '12px', color: 'var(--color-text-secondary)', display: 'block', marginBottom: '5px' }}>
            Password {mode === 'register' && <span style={{ color: 'var(--color-text-muted)' }}>(min 8 characters)</span>}
          </label>
          <input
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            placeholder="••••••••"
            onKeyDown={e => e.key === 'Enter' && handleSubmit()}
          />
        </div>

        {/* Error */}
        {error && (
          <p style={{ fontSize: '12px', color: '#b91c1c', marginBottom: '12px', padding: '8px', background: '#fff5f5', borderRadius: 'var(--radius-sm)', border: '1px solid #fecaca' }}>
            {error}
          </p>
        )}

        {/* Submit */}
        <button
          onClick={handleSubmit}
          disabled={loading || !email.trim() || !password.trim()}
          style={{
            width: '100%', padding: '8px 0', fontSize: '13px', fontWeight: '500',
            color: 'var(--color-bg)', background: 'var(--color-accent)',
            border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
            opacity: loading || !email.trim() || !password.trim() ? 0.6 : 1,
          }}
        >
          {loading ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
        </button>
      </div>
    </>
  )
}
