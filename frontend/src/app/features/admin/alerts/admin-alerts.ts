import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Alert, AlertSeverity, AlertType } from '../../../core/models/api.models';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { apiErrorMessage } from '../../../core/services/http-error';
import { PageState } from '../../../shared/ui/page-state';

@Component({
  selector: 'app-admin-alerts', imports: [ReactiveFormsModule, DatePipe, PageState],
  template: `
    <header class="page-header"><div><span class="eyebrow">System events</span><h1>Alerts</h1><p>Review alerts across all accounts and equipment.</p></div></header>
    <section class="filter-bar" aria-label="Alert filters"><label><span>Severity</span><select [formControl]="severity" (change)="resetAndLoad()"><option value="">All severities</option><option value="INFO">Info</option><option value="WARNING">Warning</option><option value="CRITICAL">Critical</option></select></label><label><span>Type</span><select [formControl]="type" (change)="resetAndLoad()"><option value="">All types</option><option value="LOW_BATTERY">Low battery</option><option value="HIGH_CONSUMPTION">High consumption</option><option value="LOW_PRODUCTION">Low production</option><option value="DEVICE_OFFLINE">Device offline</option></select></label><label><span>Read state</span><select [formControl]="read" (change)="resetAndLoad()"><option value="">All alerts</option><option value="false">Unread</option><option value="true">Read</option></select></label><label><span>User ID</span><input type="number" min="1" [formControl]="userId" (change)="resetAndLoad()" placeholder="Any user" /></label></section>
    @if (loading()) { <app-page-state kind="loading" title="Loading alerts" message="Retrieving system events…" /> } @else if (error()) { <app-page-state kind="error" title="Could not load alerts" [message]="error()" (retry)="load()" /> } @else if (!alerts().length) { <app-page-state kind="empty" title="No alerts found" message="No alerts match the selected filters." /> } @else { <section class="alert-list">@for (alert of alerts(); track alert.id) { <article class="alert-row" [class.alert-row--unread]="!alert.read"><span [class]="'severity-mark severity-mark--' + alert.severity.toLowerCase()" aria-hidden="true"></span><div><div class="alert-row__heading"><strong>{{ label(alert.type) }}</strong><span [class]="'badge badge--' + severityClass(alert.severity)">{{ alert.severity }}</span>@if (!alert.read) { <span class="badge badge--primary">Unread</span> }</div><p>{{ alert.message }}</p><small>{{ alert.createdAt | date:'medium' }} · User {{ alert.userId }}@if (alert.siteId) { · Site {{ alert.siteId }} }</small></div></article> }</section><div class="pagination"><span>Page {{ page() + 1 }} of {{ totalPages() || 1 }}</span><div><button class="button button--secondary button--small" [disabled]="page() === 0" (click)="change(-1)">Previous</button><button class="button button--secondary button--small" [disabled]="page() + 1 >= totalPages()" (click)="change(1)">Next</button></div></div> }
  `,
})
export class AdminAlerts {
  private readonly api = inject(AdminApiService);
  readonly alerts = signal<Alert[]>([]); readonly loading = signal(true); readonly error = signal(''); readonly page = signal(0); readonly totalPages = signal(0);
  readonly severity = new FormControl<AlertSeverity | ''>('', { nonNullable: true }); readonly type = new FormControl<AlertType | ''>('', { nonNullable: true }); readonly read = new FormControl<'true' | 'false' | ''>('', { nonNullable: true }); readonly userId = new FormControl<number | null>(null);
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.api.alerts({ page: this.page(), severity: this.severity.value, type: this.type.value, read: this.read.value === '' ? null : this.read.value === 'true', userId: this.userId.value }).subscribe({ next: (result) => { this.alerts.set(result.content); this.totalPages.set(result.totalPages); this.loading.set(false); }, error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); } }); }
  resetAndLoad(): void { this.page.set(0); this.load(); }
  change(delta: number): void { this.page.update((value) => value + delta); this.load(); }
  label(value: string): string { return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()); }
  severityClass(value: AlertSeverity): string { return value === 'CRITICAL' ? 'error' : value === 'WARNING' ? 'warning' : 'info'; }
}
