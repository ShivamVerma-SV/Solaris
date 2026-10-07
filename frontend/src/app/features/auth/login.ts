import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/services/http-error';
import { ThemeService } from '../../core/services/theme.service';
import { Brand } from '../../shared/ui/brand';
import { AppIcon } from '../../shared/ui/app-icon';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Brand, AppIcon],
  template: `
    <main class="auth-page">
      <section class="auth-context" aria-label="Solaris product overview">
        <app-brand />
        <div class="auth-context__content">
          <span class="eyebrow eyebrow--light">Solar energy monitoring</span>
          <h1>Clear visibility across your energy system.</h1>
          <p>Monitor production, consumption, storage and system health from one dependable workspace.</p>
          <div class="system-line" aria-hidden="true"><span></span><span></span><span></span><span></span></div>
        </div>
        <p class="auth-context__foot">Secure access · Role-based workspaces · Live operational data</p>
      </section>
      <section class="auth-form-wrap">
        <button class="theme-button auth-theme" type="button" (click)="theme.toggle()" [attr.aria-label]="theme.current() === 'dark' ? 'Use light theme' : 'Use dark theme'">
          <app-icon [name]="theme.current() === 'dark' ? 'sun' : 'moon'" />
        </button>
        <div class="auth-card">
          <div class="auth-card__mobile-brand"><app-brand /></div>
          <span class="eyebrow">Welcome back</span>
          <h2>Sign in to Solaris</h2>
          <p class="muted">Use your account credentials to continue.</p>
          @if (notice()) { <div class="callout callout--success" role="status">{{ notice() }}</div> }
          @if (error()) { <div class="callout callout--error" role="alert">{{ error() }}</div> }
          <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <label class="field">
              <span>Email address</span>
              <input type="email" formControlName="email" autocomplete="email" placeholder="you@example.com" />
              @if (form.controls.email.touched && form.controls.email.invalid) { <small class="field__error">Enter a valid email address.</small> }
            </label>
            <label class="field">
              <span>Password</span>
              <span class="input-action">
                <input [type]="showPassword() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" placeholder="Enter your password" />
                <button type="button" (click)="showPassword.update((value) => !value)" [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">{{ showPassword() ? 'Hide' : 'Show' }}</button>
              </span>
              @if (form.controls.password.touched && form.controls.password.invalid) { <small class="field__error">Password is required.</small> }
            </label>
            <button class="button button--primary button--block" type="submit" [disabled]="loading()">
              @if (loading()) { <span class="button-spinner" aria-hidden="true"></span> Signing in… } @else { Sign in }
            </button>
          </form>
          <p class="auth-switch">New to Solaris? <a routerLink="/register">Create an account</a></p>
        </div>
      </section>
    </main>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly theme = inject(ThemeService);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly notice = signal(this.route.snapshot.queryParamMap.has('registered') ? 'Account created. You can sign in now.' : this.route.snapshot.queryParamMap.has('session') ? 'Your session ended. Please sign in again.' : '');
  readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true); this.error.set('');
    this.auth.login(this.form.getRawValue()).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => void this.router.navigateByUrl(this.auth.redirectForRole()),
      error: (error: unknown) => this.error.set(apiErrorMessage(error, 'Sign in failed.')),
    });
  }
}
