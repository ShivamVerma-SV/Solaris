import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/services/http-error';
import { ThemeService } from '../../core/services/theme.service';
import { Brand } from '../../shared/ui/brand';
import { AppIcon } from '../../shared/ui/app-icon';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  return control.get('password')?.value === control.get('confirmPassword')?.value ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, Brand, AppIcon],
  template: `
    <main class="auth-page">
      <section class="auth-context" aria-label="Solaris product overview">
        <app-brand />
        <div class="auth-context__content">
          <span class="eyebrow eyebrow--light">Your system, understood</span>
          <h1>Start monitoring what your solar system is doing.</h1>
          <p>Your homeowner account provides secure access to the sites, devices and energy data assigned to you.</p>
          <div class="system-line" aria-hidden="true"><span></span><span></span><span></span><span></span></div>
        </div>
        <p class="auth-context__foot">Homeowner registration · No role selection required</p>
      </section>
      <section class="auth-form-wrap auth-form-wrap--register">
        <button class="theme-button auth-theme" type="button" (click)="theme.toggle()" [attr.aria-label]="theme.current() === 'dark' ? 'Use light theme' : 'Use dark theme'"><app-icon [name]="theme.current() === 'dark' ? 'sun' : 'moon'" /></button>
        <div class="auth-card">
          <div class="auth-card__mobile-brand"><app-brand /></div>
          <span class="eyebrow">Create account</span>
          <h2>Join Solaris</h2>
          <p class="muted">Set up secure access to your homeowner workspace.</p>
          @if (error()) { <div class="callout callout--error" role="alert">{{ error() }}</div> }
          <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <label class="field"><span>Full name</span><input formControlName="name" autocomplete="name" placeholder="Your name" />
              @if (form.controls.name.touched && form.controls.name.invalid) { <small class="field__error">Name must be between 2 and 120 characters.</small> }
            </label>
            <label class="field"><span>Email address</span><input type="email" formControlName="email" autocomplete="email" placeholder="you@example.com" />
              @if (form.controls.email.touched && form.controls.email.invalid) { <small class="field__error">Enter a valid email address.</small> }
            </label>
            <label class="field"><span>Password</span><input type="password" formControlName="password" autocomplete="new-password" placeholder="At least 8 characters" />
              @if (form.controls.password.touched && form.controls.password.invalid) { <small class="field__error">Use 8–72 characters.</small> }
            </label>
            <label class="field"><span>Confirm password</span><input type="password" formControlName="confirmPassword" autocomplete="new-password" placeholder="Repeat your password" />
              @if (form.controls.confirmPassword.touched && (form.controls.confirmPassword.invalid || form.hasError('passwordMismatch'))) { <small class="field__error">Passwords must match.</small> }
            </label>
            <button class="button button--primary button--block" type="submit" [disabled]="loading()">@if (loading()) { <span class="button-spinner" aria-hidden="true"></span> Creating account… } @else { Create account }</button>
          </form>
          <p class="auth-switch">Already have an account? <a routerLink="/login">Sign in</a></p>
        </div>
      </section>
    </main>
  `,
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly theme = inject(ThemeService);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(2), Validators.maxLength(120)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)] }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  }, { validators: passwordsMatch });

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.loading()) return;
    const { name, email, password } = this.form.getRawValue();
    this.loading.set(true); this.error.set('');
    this.auth.register({ name, email, password }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => void this.router.navigate(['/login'], { queryParams: { registered: '1' } }),
      error: (error: unknown) => this.error.set(apiErrorMessage(error, 'Registration failed.')),
    });
  }
}
