// Read from window.__env (see public/env.js), which the Docker image
// overwrites at container startup with the real backend URL — the same
// build works in any environment without rebuilding.
declare global {
  interface Window {
    __env?: { apiUrl?: string };
  }
}

export const API_URL = window.__env?.apiUrl || 'http://localhost:8081';
