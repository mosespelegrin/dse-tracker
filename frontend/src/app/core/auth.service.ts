import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_URL } from './config';
import { AuthResponse } from './models';

interface StoredUser {
  name: string;
  email: string;
}

const TOKEN_KEY = 'dse_token';
const REFRESH_KEY = 'dse_refresh_token';
const USER_KEY = 'dse_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSignal = signal<StoredUser | null>(this.readUser());
  readonly user = this.userSignal.asReadonly();
  readonly isLoggedIn = computed(() => !!this.userSignal());

  constructor(private http: HttpClient) {}

  get token(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  get refreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  }

  register(name: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_URL}/auth/register`, { name, email, password })
      .pipe(tap((res) => this.persistSession(res)));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_URL}/auth/login`, { email, password })
      .pipe(tap((res) => this.persistSession(res)));
  }

  forgotPassword(email: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/reset-password`, { token, newPassword });
  }

  refresh(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_URL}/auth/refresh`, { refreshToken: this.refreshToken })
      .pipe(tap((res) => this.persistSession(res)));
  }

  logout() {
    const rt = this.refreshToken;
    if (rt) {
      // Best-effort — clear local state regardless of whether this call succeeds
      this.http.post(`${API_URL}/auth/logout`, { refreshToken: rt }).subscribe({
        next: () => {},
        error: () => {},
      });
    }
    this.clearSession();
  }

  private persistSession(res: AuthResponse) {
    try {
      if (res.token) localStorage.setItem(TOKEN_KEY, res.token);
      if (res.refreshToken) localStorage.setItem(REFRESH_KEY, res.refreshToken);
      if (res.name && res.email) {
        const user: StoredUser = { name: res.name, email: res.email };
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this.userSignal.set(user);
      }
    } catch {
      // localStorage unavailable (private mode, blocked) — session just won't persist across reloads
    }
  }

  clearSession() {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_KEY);
      localStorage.removeItem(USER_KEY);
    } catch {
      // ignore
    }
    this.userSignal.set(null);
  }

  private readUser(): StoredUser | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
