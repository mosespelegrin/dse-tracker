import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';
import { GoogleSigninButton } from '../../core/google-signin-button';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, GoogleSigninButton],
  template: `
    <div class="auth-wrap">
      <div class="auth-card card">
        <div class="auth-brand">
          <img src="logo-mark.png" alt="DSE Tracker" class="auth-logo" />
          <h1>DSE Tracker</h1>
        </div>
        <p class="subtitle">Log in to your portfolio</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label>Email</label>
            <input type="email" formControlName="email" placeholder="you@example.com" />
          </div>
          <div class="field">
            <label>Password</label>
            <div class="password-field">
              <input [type]="showPassword() ? 'text' : 'password'" formControlName="password" placeholder="••••••••" />
              <button type="button" class="password-toggle" (click)="showPassword.set(!showPassword())"
                      [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
                @if (showPassword()) {
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-7 0-11-7-11-7a18.5 18.5 0 0 1 5.06-5.94M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                } @else {
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                }
              </button>
            </div>
          </div>

          <button class="btn" type="submit" [disabled]="form.invalid || loading()" style="width:100%">
            {{ loading() ? 'Logging in…' : 'Log in' }}
          </button>
        </form>

        @if (showResend()) {
          <div class="resend-box">
            <p>Your account isn't verified yet.</p>
            <button class="btn btn-outline btn-sm" type="button" [disabled]="resending()" (click)="resend()">
              {{ resending() ? 'Sending…' : 'Resend verification email' }}
            </button>
          </div>
        }

        <div class="divider"><span>or</span></div>
        <app-google-signin-button (credential)="onGoogleCredential($event)"></app-google-signin-button>

        <div class="links">
          <a routerLink="/forgot-password">Forgot password?</a>
          <a routerLink="/register">Create an account</a>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .auth-wrap {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 16px;
      }
      .auth-card {
        width: 100%;
        max-width: 380px;
      }
      .auth-brand {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 2px;
      }
      .auth-logo {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        object-fit: cover;
        box-shadow: 0 0 14px rgba(0, 190, 255, 0.4);
        border: 1px solid rgba(0, 210, 255, 0.25);
      }
      h1 {
        font-size: 22px;
        margin: 0;
      }
      .subtitle {
        color: var(--text-2);
        margin: 6px 0 20px;
        font-size: 13px;
      }
      .resend-box {
        margin-top: 14px;
        padding: 12px;
        border: 1px solid var(--amber-bg);
        background: var(--amber-bg);
        border-radius: var(--radius-sm);
        text-align: center;
      }
      .resend-box p {
        font-size: 12px;
        color: var(--amber);
        margin-bottom: 8px;
      }
      .divider {
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 18px 0;
        color: var(--text-3);
        font-size: 12px;
      }
      .divider::before,
      .divider::after {
        content: '';
        flex: 1;
        height: 1px;
        background: var(--border);
      }
      .links {
        display: flex;
        justify-content: space-between;
        margin-top: 16px;
        font-size: 12px;
      }
      .links a {
        color: var(--accent);
        text-decoration: none;
      }
    `,
  ],
})
export class Login {
  private fb = inject(FormBuilder);
  readonly loading = signal(false);
  readonly showResend = signal(false);
  readonly resending = signal(false);
  readonly showPassword = signal(false);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  constructor(
    private auth: AuthService,
    private router: Router,
    private toast: ToastService,
  ) {}

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.showResend.set(false);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email!, password!).subscribe({
      next: () => {
        this.toast.ok('Welcome back!');
        this.router.navigate(['/dashboard/portfolio']);
      },
      error: (err) => {
        this.loading.set(false);
        const message = err?.error?.message || 'Invalid email or password';
        this.toast.err(message);
        if (message.toLowerCase().includes('verify your email')) {
          this.showResend.set(true);
        }
      },
    });
  }

  onGoogleCredential(idToken: string) {
    this.loading.set(true);
    this.auth.googleSignIn(idToken).subscribe({
      next: () => {
        this.toast.ok('Signed in with Google');
        this.router.navigate(['/dashboard/portfolio']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.err(err?.error?.message || 'Google sign-in failed');
      },
    });
  }

  resend() {
    const email = this.form.getRawValue().email;
    if (!email) return;
    this.resending.set(true);
    this.auth.resendVerification(email).subscribe({
      next: (res) => {
        this.resending.set(false);
        this.toast.ok(res.message);
      },
      error: () => {
        this.resending.set(false);
        this.toast.err('Could not resend — try again shortly');
      },
    });
  }
}
