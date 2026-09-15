import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="auth-card card">
        <h1>Create your account</h1>
        <p class="subtitle">Start tracking your DSE portfolio</p>

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
            <input type="password" formControlName="password" placeholder="At least 6 characters" />
            @if (form.get('password')?.touched && form.get('password')?.errors?.['minlength']) {
              <span class="field-error">Password must be at least 6 characters</span>
            }
          </div>

          <button class="btn" type="submit" [disabled]="form.invalid || loading()" style="width:100%">
            {{ loading() ? 'Creating account…' : 'Create account' }}
          </button>
        </form>

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
export class Register {
  private fb = inject(FormBuilder);
  readonly loading = signal(false);

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
}
