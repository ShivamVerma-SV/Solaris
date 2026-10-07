import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Alert } from '../../../core/models/api.models';
import { HomeownerApiService } from '../../../core/services/homeowner-api.service';
import { apiErrorMessage } from '../../../core/services/http-error';
import { ToastService } from '../../../core/services/toast.service';
import { PageState } from '../../../shared/ui/page-state';

@Component({
  selector: 'app-homeowner-alerts', imports: [ReactiveFormsModule, DatePipe, PageState],
  template: `
    <header class="page-header"><div><span class="eyebrow">System events</span><h1>Your alerts</h1><p>Review events generated for your sites and mark them as read.</p></div></header>
    <section class="filter-bar"><label><span>Read state</span><select [formControl]="readFilter" (change)="resetAndLoad()"><option value="">All alerts</option><option value="false">Unread</option><option value="true">Read</option></select></label></section>
    @if (loading()) { <app-page-state kind="loading" title="Loading alerts" message="Retrieving your latest events…" /> } @else if (error()) { <app-page-state kind="error" title="Could not load alerts" [message]="error()" (retry)="load()" /> } @else if (!alerts().length) { <app-page-state kind="empty" title="No alerts found" message="There are no alerts matching this filter." /> } @else { <section class="alert-list">@for (alert of alerts(); track alert.id) { <article class="alert-row" [class.alert-row--unread]="!alert.read"><span [class]="'severity-mark severity-mark--' + alert.severity.toLowerCase()"></span><div><div class="alert-row__heading"><strong>{{ label(alert.type) }}</strong><span [class]="'badge badge--' + severityClass(alert.severity)">{{ alert.severity }}</span>@if (!alert.read) { <span class="badge badge--primary">Unread</span> }</div><p>{{ alert.message }}</p><small>{{ alert.createdAt | date:'medium' }}@if (alert.siteId) { · Site {{ alert.siteId }} }</small></div>@if (!alert.read) { <button class="button button--secondary button--small" type="button" (click)="markRead(alert)">Mark as read</button> }</article> }</section><div class="pagination"><span>Page {{ page() + 1 }} of {{ totalPages() || 1 }}</span><div><button class="button button--secondary button--small" [disabled]="page() === 0" (click)="change(-1)">Previous</button><button class="button button--secondary button--small" [disabled]="page() + 1 >= totalPages()" (click)="change(1)">Next</button></div></div> }
  `,
})
export class HomeownerAlerts {
  private readonly api = inject(HomeownerApiService); private readonly toast = inject(ToastService);
  readonly alerts = signal<Alert[]>([]); readonly loading = signal(true); readonly error = signal(''); readonly page = signal(0); readonly totalPages = signal(0); readonly readFilter = new FormControl<'true' | 'false' | ''>('', { nonNullable: true });
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.api.alerts({ page: this.page(), read: this.readFilter.value === '' ? null : this.readFilter.value === 'true' }).subscribe({ next: (result) => { this.alerts.set(result.content); this.totalPages.set(result.totalPages); this.loading.set(false); }, error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); } }); }
  resetAndLoad(): void { this.page.set(0); this.load(); } change(delta: number): void { this.page.update((value) => value + delta); this.load(); }
  markRead(alert: Alert): void { this.api.markAlertRead(alert.id).subscribe({ next: (updated) => { this.alerts.update((alerts) => alerts.map((item) => item.id === updated.id ? updated : item)); this.toast.show('Alert marked as read.', 'success'); }, error: (error: unknown) => this.toast.show(apiErrorMessage(error), 'error') }); }
  label(value: string): string { return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()); }
  severityClass(value: string): string { return value === 'CRITICAL' ? 'error' : value === 'WARNING' ? 'warning' : 'info'; }
}
