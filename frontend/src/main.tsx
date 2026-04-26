import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { AuthProvider } from './auth/AuthContext'
import { SharedView } from './components/SharedView'
import './index.css'

/**
 * Simple path-based routing — no React Router dependency.
 *
 * /shared/:token  → SharedView (public, no auth context needed)
 * anything else   → normal App with AuthProvider
 *
 * nginx is already configured with try_files so all paths are served
 * by index.html and handled here.
 */
const isSharedView = window.location.pathname.startsWith('/shared/')

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    {isSharedView ? (
      <SharedView />
    ) : (
      <AuthProvider>
        <App />
      </AuthProvider>
    )}
  </React.StrictMode>
)
