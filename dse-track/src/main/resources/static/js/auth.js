// ═══════════════════════════════════════════
// DSE Tracker — Auth JavaScript
// Connects login/register forms to Spring Boot
// Place this file in your static/js/ folder
// Add <script src="/js/auth.js"></script> to index.html
// ═══════════════════════════════════════════

const API = 'http://localhost:8081';

// ── TOAST helper ──
const showToast = (msg, type = 'success') => {
  const existing = document.getElementById('auth-toast');
  if (existing) existing.remove();

  const el = document.createElement('div');
  el.id = 'auth-toast';
  el.style.cssText = `
    position: fixed; bottom: 24px; right: 24px; z-index: 9999;
    padding: 12px 18px; border-radius: 8px; font-size: 13px; font-weight: 700;
    background: var(--panel); border: 1px solid var(--line);
    color: ${type === 'success' ? 'var(--green)' : 'var(--pink)'};
    border-color: ${type === 'success' ? 'rgba(0,240,122,0.4)' : 'rgba(255,61,118,0.4)'};
    box-shadow: 0 8px 24px rgba(0,0,0,0.2);
    animation: toastIn 200ms ease;
  `;
  el.textContent = msg;
  document.body.appendChild(el);
  setTimeout(() => el?.remove(), 3500);
};

// ── REGISTER ──
const registerForm = document.querySelector('[data-modal="signup"] form');
if (registerForm) {
  registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = registerForm.querySelector('button[type="submit"]');
    const name     = registerForm.querySelector('[name="name"]').value.trim();
    const email    = registerForm.querySelector('[name="email"]').value.trim();
    const password = registerForm.querySelector('[name="password"]').value;

    if (!name || !email || !password) {
      showToast('Please fill in all fields', 'error');
      return;
    }

    if (password.length < 6) {
      showToast('Password must be at least 6 characters', 'error');
      return;
    }

    btn.textContent = 'Creating account…';
    btn.disabled = true;

    try {
      const res = await fetch(`${API}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, password })
      });

      const data = await res.json();

      if (res.ok) {
        // Save token and user info
        localStorage.setItem('dse_token', data.token);
        localStorage.setItem('dse_user', JSON.stringify({
          name: data.name, email: data.email, role: data.role, mustChangePassword: data.mustChangePassword
        }));
        showToast('Account created! Redirecting…');
        const next = data.mustChangePassword ? 'change-password.html' : 'dashboard.html';
        setTimeout(() => window.location.href = next, 1000);
      } else {
        showToast(data.message || 'Registration failed', 'error');
      }
    } catch (err) {
      showToast('Could not connect to server', 'error');
    } finally {
      btn.textContent = 'Create account';
      btn.disabled = false;
    }
  });
}

// ── LOGIN ──
const loginForm = document.querySelector('[data-modal="login"] form');
if (loginForm) {
  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn      = loginForm.querySelector('button[type="submit"]');
    const email    = loginForm.querySelector('[name="email"]').value.trim();
    const password = loginForm.querySelector('[name="password"]').value;

    if (!email || !password) {
      showToast('Enter your email and password', 'error');
      return;
    }

    btn.textContent = 'Logging in…';
    btn.disabled = true;

    try {
      const res = await fetch(`${API}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      const data = await res.json();

      if (res.ok) {
        localStorage.setItem('dse_token', data.token);
        localStorage.setItem('dse_user', JSON.stringify({
          name: data.name, email: data.email, role: data.role, mustChangePassword: data.mustChangePassword
        }));
        showToast('Welcome back! Redirecting…');
        const next = data.mustChangePassword ? 'change-password.html' : 'dashboard.html';
        setTimeout(() => window.location.href = next, 1000);
      } else {
        showToast(data.message || 'Invalid email or password', 'error');
      }
    } catch (err) {
      showToast('Could not connect to server', 'error');
    } finally {
      btn.textContent = 'Log in';
      btn.disabled = false;
    }
  });
}

// ── If already logged in, skip landing page ──
if (localStorage.getItem('dse_token') && window.location.pathname.includes('index')) {
  window.location.href = 'dashboard.html';
}
