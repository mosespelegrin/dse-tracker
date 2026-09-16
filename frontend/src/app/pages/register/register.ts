import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';
import { GoogleSigninButton } from '../../core/google-signin-button';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, GoogleSigninButton],
  template: `
    <div class="auth-wrap">
      <div class="auth-card card">
        <div class="auth-brand">
          <img src="logo-mark.png" alt="DSE Tracker" class="auth-logo" />
          <h1>Create your account</h1>
        </div>
        <p class="subtitle">Start tracking your DSE portfolio with DSE Tracker</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label>Name</label>
            <input type="text" formControlName="name" placeholder="Your name" />
          </div>
          <div class="field">
            <label>Email</label>
            <input type="email" formControlName="email" placeholder="you@example.com" />
          </div>
          <div class="field">
            <label>Password</label>
            <div class="password-field">
              <input [type]="showPassword() ? 'text' : 'password'" formControlName="password" placeholder="At least 6 characters" />
              <button type="button" class="password-toggle" (click)="showPassword.set(!showPassword())"
                      [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
                @if (showPassword()) {
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-7 0-11-7-11-7a18.5 18.5 0 0 1 5.06-5.94M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                } @else {
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                }
              </button>
            </div>
            @if (form.get('password')?.touched && form.get('password')?.errors?.['minlength']) {
              <span class="field-error">Password must be at least 6 characters</span>
            }
          </div>

          <button class="btn" type="submit" [disabled]="form.invalid || loading()" style="width:100%">
            {{ loading() ? 'Creating account…' : 'Create account' }}
          </button>
        </form>

        <div class="divider"><span>or</span></div>
        <app-google-signin-button (credential)="onGoogleCredential($event)"></app-google-signin-button>

        <div class="links">
          <a routerLink="/login">Already have an account? Log in</a>
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
        font-size: 20px;
        margin: 0;
      }
      .subtitle {
        color: var(--text-2);
        margin: 6px 0 20px;
        font-size: 13px;
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
export class Register {
  private fb = inject(FormBuilder);
  readonly loading = signal(false);
  readonly showPassword = signal(false);

  form = this.fb.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  constructor(
    private auth: AuthService,
    private router: Router,
    private toast: ToastService,
  ) {}

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    const { name, email, password } = this.form.getRawValue();
    this.auth.register(name!, email!, password!).subscribe({
      next: (res) => {
        this.toast.ok(res.message || 'Account created!');
        this.router.navigate(['/dashboard/portfolio']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.err(err?.error?.message || 'Registration failed');
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
}
