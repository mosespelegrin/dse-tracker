import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="auth-card card">
        <h1>Reset password</h1>
        <p class="subtitle">Enter the token from your email and a new password.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label>Reset token</label>
            <input type="text" formControlName="token" placeholder="Paste the token from your email" />
          </div>
          <div class="field">
            <label>New password</label>
            <input type="password" formControlName="newPassword" placeholder="At least 6 characters" />
          </div>

          <button class="btn" type="submit" [disabled]="form.invalid || loading()" style="width:100%">
            {{ loading() ? 'Resetting…' : 'Reset password' }}
          </button>
        </form>

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
export class ResetPassword implements OnInit {
  private fb = inject(FormBuilder);
  readonly loading = signal(false);

  form = this.fb.group({
    token: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
  });

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    const tokenFromLink = this.route.snapshot.queryParamMap.get('token');
    if (tokenFromLink) {
      this.form.patchValue({ token: tokenFromLink });
    }
  }

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    const { token, newPassword } = this.form.getRawValue();
    this.auth.resetPassword(token!, newPassword!).subscribe({
      next: () => {
        this.toast.ok('Password reset — please log in');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.err(err?.error?.message || 'Invalid or expired token');
      },
    });
  }
}
