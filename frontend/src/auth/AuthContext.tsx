import { createContext, useCallback, useContext, useEffect, useState } from 'react'

interface AuthUser {
  id: string
  email: string
  role: string
}

interface AuthState {
  user: AuthUser | null
  accessToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refreshAccessToken: () => Promise<string | null>
}

const AuthContext = createContext<AuthContextValue | null>(null)

/**
 * Access token lives in memory only — never localStorage (XSS risk).
 * Refresh token is sent to the backend on refresh calls.
 * On page reload, we attempt a silent refresh using the stored refresh token.
 */

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [refreshToken, setRefreshToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const setAuth = (data: { accessToken: string; refreshToken: string; user: AuthUser }) => {
    setAccessToken(data.accessToken)
    setRefreshToken(data.refreshToken)
    setUser(data.user)
    // Persist refresh token in sessionStorage — survives page reload, not cross-tab
    sessionStorage.setItem('infraforge_rt', data.refreshToken)
  }

  const clearAuth = () => {
    setAccessToken(null)
    setRefreshToken(null)
    setUser(null)
    sessionStorage.removeItem('infraforge_rt')
  }

  // Silent refresh on app load
  useEffect(() => {
    const stored = sessionStorage.getItem('infraforge_rt')
    if (!stored) { setIsLoading(false); return }

    fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: stored }),
    })
      .then(r => r.ok ? r.json() : null)
      .then(data => { if (data) setAuth(data) })
      .catch(() => {})
      .finally(() => setIsLoading(false))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || 'Login failed')
    }
    const data = await res.json()
    setAuth(data)
  }, [])

  const register = useCallback(async (email: string, password: string) => {
    const res = await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || 'Registration failed')
    }
    const data = await res.json()
    setAuth(data)
  }, [])

  const logout = useCallback(async () => {
    const rt = refreshToken || sessionStorage.getItem('infraforge_rt')
    if (rt) {
      await fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: rt }),
      }).catch(() => {})
    }
    clearAuth()
  }, [refreshToken])

  const refreshAccessToken = useCallback(async (): Promise<string | null> => {
    const rt = refreshToken || sessionStorage.getItem('infraforge_rt')
    if (!rt) return null
    const res = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt }),
    })
    if (!res.ok) { clearAuth(); return null }
    const data = await res.json()
    setAuth(data)
    return data.accessToken
  }, [refreshToken])

  return (
    <AuthContext.Provider value={{
      user, accessToken, isAuthenticated: !!user, isLoading,
      login, register, logout, refreshAccessToken,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
