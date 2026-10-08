import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AdminUser } from '../../../core/models/api.models';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { apiErrorMessage } from '../../../core/services/http-error';
import { ToastService } from '../../../core/services/toast.service';
import { ModalAutofocus } from '../../../shared/ui/modal-autofocus';
import { PageState } from '../../../shared/ui/page-state';

@Component({
  selector: 'app-admin-users', imports: [ReactiveFormsModule, DatePipe, PageState, ModalAutofocus],
  host: { '(document:keydown.escape)': 'closeOverlay()' },
  template: `
    <header class="page-header"><div><span class="eyebrow">Access management</span><h1>Users</h1><p>Create and maintain administrator and homeowner accounts.</p></div><button class="button button--primary" type="button" (click)="openCreate()">Add user</button></header>
    @if (loading()) { <app-page-state kind="loading" title="Loading users" message="Retrieving account records…" /> }
    @else if (error()) { <app-page-state kind="error" title="Could not load users" [message]="error()" (retry)="load()" /> }
    @else if (!users().length) { <app-page-state kind="empty" title="No users yet" message="Create the first managed account to get started." /> }
    @else {
      <section class="panel panel--table"><div class="table-scroll"><table><thead><tr><th>User</th><th>Role</th><th>Status</th><th>Phone</th><th>Created</th><th><span class="sr-only">Actions</span></th></tr></thead><tbody>
        @for (user of users(); track user.id) { <tr><td data-label="User"><strong>{{ user.name }}</strong><small>{{ user.email }}</small></td><td data-label="Role"><span class="badge">{{ user.role }}</span></td><td data-label="Status"><span [class]="user.enabled ? 'badge badge--success' : 'badge badge--muted'">{{ user.enabled ? 'Enabled' : 'Disabled' }}</span></td><td data-label="Phone">{{ user.phone || '—' }}</td><td data-label="Created">{{ user.createdAt | date:'mediumDate' }}</td><td class="table-actions"><button class="button button--quiet button--small" type="button" (click)="openEdit(user)">Edit</button><button class="button button--danger button--small" type="button" (click)="confirmDelete.set(user)">Delete</button></td></tr> }
      </tbody></table></div><div class="pagination"><span>Page {{ page() + 1 }} of {{ totalPages() || 1 }} · {{ total() }} users</span><div><button class="button button--secondary button--small" type="button" [disabled]="page() === 0" (click)="changePage(-1)">Previous</button><button class="button button--secondary button--small" type="button" [disabled]="page() + 1 >= totalPages()" (click)="changePage(1)">Next</button></div></div></section>
    }
    @if (editing()) { <div class="modal-backdrop" role="presentation"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="user-form-title"><div class="modal__header"><div><span class="eyebrow">{{ editing()?.id ? 'Edit account' : 'New account' }}</span><h2 id="user-form-title">{{ editing()?.id ? editing()?.name : 'Add user' }}</h2></div><button class="icon-button" type="button" aria-label="Close" (click)="editing.set(null)">×</button></div>
      <form [formGroup]="form" (ngSubmit)="save()" class="form-grid"><label class="field"><span>Name</span><input formControlName="name" appModalAutofocus /></label><label class="field"><span>Email</span><input type="email" formControlName="email" /></label><label class="field"><span>Phone</span><input formControlName="phone" /></label><label class="field"><span>Role</span><select formControlName="role"><option value="HOMEOWNER">Homeowner</option><option value="ADMIN">Administrator</option></select></label><label class="field field--wide"><span>{{ editing()?.id ? 'New password (optional)' : 'Password' }}</span><input type="password" formControlName="password" autocomplete="new-password" /><small>8–72 characters{{ editing()?.id ? ', or leave blank to keep the current password' : '' }}.</small></label><label class="check-field field--wide"><input type="checkbox" formControlName="enabled" /><span>Account enabled</span></label>
      <div class="modal__actions field--wide"><button class="button button--secondary" type="button" (click)="editing.set(null)">Cancel</button><button class="button button--primary" type="submit" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save user' }}</button></div></form></section></div> }
    @if (confirmDelete(); as user) { <div class="modal-backdrop"><section class="modal modal--small" role="alertdialog" aria-modal="true" aria-labelledby="delete-user-title"><h2 id="delete-user-title">Delete {{ user.name }}?</h2><p>This permanently removes the account. Related backend constraints may prevent deletion.</p><div class="modal__actions"><button class="button button--secondary" type="button" (click)="confirmDelete.set(null)">Cancel</button><button class="button button--danger" type="button" (click)="remove(user)">Delete user</button></div></section></div> }
  `,
})
export class AdminUsers {
  private readonly api = inject(AdminApiService); private readonly toast = inject(ToastService);
  readonly users = signal<AdminUser[]>([]); readonly loading = signal(true); readonly saving = signal(false); readonly error = signal('');
  readonly page = signal(0); readonly totalPages = signal(0); readonly total = signal(0); readonly editing = signal<Partial<AdminUser> | null>(null); readonly confirmDelete = signal<AdminUser | null>(null);
  readonly form = new FormGroup({ name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(2), Validators.maxLength(120)] }), email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }), phone: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(30)] }), role: new FormControl<'ADMIN' | 'HOMEOWNER'>('HOMEOWNER', { nonNullable: true }), password: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(72)] }), enabled: new FormControl(true, { nonNullable: true }) });
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.api.users(this.page()).subscribe({ next: (result) => { this.users.set(result.content); this.total.set(result.totalElements); this.totalPages.set(result.totalPages); this.loading.set(false); }, error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); } }); }
  changePage(delta: number): void { this.page.update((page) => page + delta); this.load(); }
  openCreate(): void { this.editing.set({}); this.form.reset({ name: '', email: '', phone: '', role: 'HOMEOWNER', password: '', enabled: true }); this.form.controls.password.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(72)]); this.form.controls.password.updateValueAndValidity(); }
  openEdit(user: AdminUser): void { this.editing.set(user); this.form.reset({ name: user.name, email: user.email, phone: user.phone ?? '', role: user.role, password: '', enabled: user.enabled }); this.form.controls.password.setValidators([Validators.minLength(8), Validators.maxLength(72)]); this.form.controls.password.updateValueAndValidity(); }
  closeOverlay(): void { this.editing.set(null); this.confirmDelete.set(null); }
  save(): void { this.form.markAllAsTouched(); if (this.form.invalid || this.saving()) return; const value = this.form.getRawValue(); const current = this.editing(); if (!current) return; this.saving.set(true); const request = current.id ? this.api.updateUser(current.id, { ...value, phone: value.phone || null, password: value.password || null }) : this.api.createUser({ ...value, phone: value.phone || null }); request.pipe(finalize(() => this.saving.set(false))).subscribe({ next: () => { this.toast.show(`User ${current.id ? 'updated' : 'created'}.`, 'success'); this.editing.set(null); this.load(); }, error: (error: unknown) => this.toast.show(apiErrorMessage(error), 'error') }); }
  remove(user: AdminUser): void { this.api.deleteUser(user.id).subscribe({ next: () => { this.toast.show('User deleted.', 'success'); this.confirmDelete.set(null); this.load(); }, error: (error: unknown) => this.toast.show(apiErrorMessage(error), 'error') }); }
}
