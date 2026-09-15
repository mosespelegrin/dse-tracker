import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="auth-card card">
        <h1>Forgot password</h1>
        <p class="subtitle">We'll email you a link to reset it, if that address is registered.</p>

        @if (!sent()) {
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="field">
              <label>Email</label>
              <input type="email" formControlName="email" placeholder="you@example.com" />
            </div>
            <button class="btn" type="submit" [disabled]="form.invalid || loading()" style="width:100%">
              {{ loading() ? 'Sending…' : 'Send reset link' }}
            </button>
          </form>
        } @else {
          <p class="sent-msg">If that email is registered, a reset link is on its way. Check your inbox.</p>
        }

        <div class="links">
          <a routerLink="/login">Back to login</a>
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
      h1 {
        font-size: 22px;
      }
      .subtitle {
        color: var(--text-2);
        margin: 6px 0 20px;
        font-size: 13px;
      }
      .sent-msg {
        color: var(--green);
        font-size: 13px;
        margin-bottom: 16px;
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
export class ForgotPassword {
  private fb = inject(FormBuilder);
  readonly loading = signal(false);
  readonly sent = signal(false);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  constructor(
    private auth: AuthService,
    private toast: ToastService,
  ) {}

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    const { email } = this.form.getRawValue();
    this.auth.forgotPassword(email!).subscribe({
      next: () => {
        this.loading.set(false);
        this.sent.set(true);
      },
      error: () => {
        // Backend always responds the same way here regardless of outcome —
        // a network-level error is the only realistic failure to surface.
        this.loading.set(false);
        this.toast.err('Could not reach the server');
      },
    });
  }
}
