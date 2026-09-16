// Read from window.__env (see public/env.js), which the Docker image
// overwrites at container startup with real values — the same build works
// in any environment without rebuilding.
declare global {
  interface Window {
    __env?: { apiUrl?: string; googleClientId?: string };
  }
}

export const API_URL = window.__env?.apiUrl || 'http://localhost:8081';

// Public OAuth Client ID from Google Cloud Console — safe to expose in
// frontend code (it's not a secret). Empty until GOOGLE_CLIENT_ID is set.
export const GOOGLE_CLIENT_ID = window.__env?.googleClientId || '';
