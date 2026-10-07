import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Profile } from '../../../core/models/api.models';
import { HomeownerApiService } from '../../../core/services/homeowner-api.service';
import { apiErrorMessage } from '../../../core/services/http-error';
import { ToastService } from '../../../core/services/toast.service';
import { PageState } from '../../../shared/ui/page-state';

function passwordChangeValidator(control: AbstractControl): ValidationErrors | null {
  const currentPassword = control.get('currentPassword')?.value;
  const newPassword = control.get('newPassword')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  const errors: ValidationErrors = {};
  if (newPassword !== confirmPassword) errors['passwordMismatch'] = true;
  if (currentPassword && newPassword && currentPassword === newPassword) errors['passwordUnchanged'] = true;
  return Object.keys(errors).length ? errors : null;
}

@Component({
  selector: 'app-homeowner-profile', imports: [ReactiveFormsModule, DatePipe, PageState],
  template: `
    <header class="page-header"><div><span class="eyebrow">Account</span><h1>Your profile</h1><p>Manage your contact information and account password.</p></div></header>
    @if (loading()) { <app-page-state kind="loading" title="Loading profile" message="Retrieving your account…" /> } @else if (error()) { <app-page-state kind="error" title="Could not load profile" [message]="error()" (retry)="load()" /> } @else {
      <div class="profile-layout">
        <aside class="panel profile-summary"><span class="profile-avatar">{{ initials() }}</span><h2>{{ profile()?.name }}</h2><p>{{ profile()?.email }}</p><span class="badge badge--primary">Homeowner</span><dl><div><dt>Member since</dt><dd>{{ profile()?.createdAt | date:'mediumDate' }}</dd></div><div><dt>Last updated</dt><dd>{{ profile()?.updatedAt | date:'medium' }}</dd></div></dl></aside>
        <div class="profile-panels">
          <section class="panel"><div class="panel__header"><div><span class="eyebrow">Contact information</span><h2>Profile details</h2></div></div><form [formGroup]="form" (ngSubmit)="save()" class="form-stack"><label class="field"><span>Full name</span><input formControlName="name" autocomplete="name" />@if (form.controls.name.touched && form.controls.name.invalid) { <small class="field__error">Enter 2–120 characters.</small> }</label><label class="field"><span>Email address</span><input type="email" formControlName="email" autocomplete="email" />@if (form.controls.email.touched && form.controls.email.invalid) { <small class="field__error">Enter a valid email address.</small> }</label><label class="field"><span>Phone number</span><input type="tel" formControlName="phone" autocomplete="tel" /><small>Optional, up to 30 characters.</small></label><div class="form-actions"><button class="button button--primary" type="submit" [disabled]="saving() || form.pristine">{{ saving() ? 'Saving…' : 'Save changes' }}</button></div></form></section>
          <section class="panel">
            <div class="panel__header"><div><span class="eyebrow">Security</span><h2>Change password</h2><p>Confirm your current password before choosing a replacement.</p></div><button class="button button--quiet button--small" type="button" [attr.aria-pressed]="showPasswords()" (click)="showPasswords.update((shown) => !shown)">{{ showPasswords() ? 'Hide passwords' : 'Show passwords' }}</button></div>
            @if (passwordError()) { <div class="callout callout--error" role="alert">{{ passwordError() }}</div> }
            <form [formGroup]="passwordForm" (ngSubmit)="changePassword()" class="form-stack" novalidate>
              <label class="field"><span>Current password</span><input [type]="showPasswords() ? 'text' : 'password'" formControlName="currentPassword" autocomplete="current-password" />@if (passwordForm.controls.currentPassword.touched && passwordForm.controls.currentPassword.invalid) { <small class="field__error">Enter your current password.</small> }</label>
              <label class="field"><span>New password</span><input [type]="showPasswords() ? 'text' : 'password'" formControlName="newPassword" autocomplete="new-password" />@if (passwordForm.controls.newPassword.touched && passwordForm.controls.newPassword.invalid) { <small class="field__error">Use 8–72 characters.</small> } @else if (passwordForm.controls.newPassword.touched && passwordForm.hasError('passwordUnchanged')) { <small class="field__error">Choose a password different from your current password.</small> }</label>
              <label class="field"><span>Confirm new password</span><input [type]="showPasswords() ? 'text' : 'password'" formControlName="confirmPassword" autocomplete="new-password" />@if (passwordForm.controls.confirmPassword.touched && (passwordForm.controls.confirmPassword.invalid || passwordForm.hasError('passwordMismatch'))) { <small class="field__error">New passwords must match.</small> }</label>
              <div class="form-actions"><button class="button button--primary" type="submit" [disabled]="changingPassword()">{{ changingPassword() ? 'Changing password…' : 'Change password' }}</button></div>
            </form>
          </section>
        </div>
      </div>
    }
  `,
})
export class HomeownerProfile {
  private readonly api = inject(HomeownerApiService); private readonly toast = inject(ToastService);
  readonly profile = signal<Profile | null>(null); readonly loading = signal(true); readonly saving = signal(false); readonly changingPassword = signal(false); readonly error = signal(''); readonly passwordError = signal(''); readonly showPasswords = signal(false);
  readonly form = new FormGroup({ name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(2), Validators.maxLength(120)] }), email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(254)] }), phone: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(30)] }) });
  readonly passwordForm = new FormGroup({ currentPassword: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)] }), newPassword: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)] }), confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }) }, { validators: passwordChangeValidator });
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.api.profile().subscribe({ next: (profile) => { this.setProfile(profile); this.loading.set(false); }, error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); } }); }
  initials(): string { return this.profile()?.name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() ?? 'S'; }
  save(): void { this.form.markAllAsTouched(); if (this.form.invalid || this.saving()) return; const value = this.form.getRawValue(); this.saving.set(true); this.api.updateProfile({ ...value, phone: value.phone || null }).pipe(finalize(() => this.saving.set(false))).subscribe({ next: (profile) => { this.setProfile(profile); this.toast.show('Profile updated.', 'success'); }, error: (error: unknown) => this.toast.show(apiErrorMessage(error), 'error') }); }
  changePassword(): void { this.passwordForm.markAllAsTouched(); if (this.passwordForm.invalid || this.changingPassword()) return; const { currentPassword, newPassword } = this.passwordForm.getRawValue(); this.changingPassword.set(true); this.passwordError.set(''); this.api.changePassword({ currentPassword, newPassword }).pipe(finalize(() => this.changingPassword.set(false))).subscribe({ next: () => { this.passwordForm.reset({ currentPassword: '', newPassword: '', confirmPassword: '' }); this.showPasswords.set(false); this.toast.show('Password changed successfully.', 'success'); }, error: (error: unknown) => this.passwordError.set(apiErrorMessage(error, 'Could not change your password.')) }); }
  private setProfile(profile: Profile): void { this.profile.set(profile); this.form.reset({ name: profile.name, email: profile.email, phone: profile.phone ?? '' }); }
}
