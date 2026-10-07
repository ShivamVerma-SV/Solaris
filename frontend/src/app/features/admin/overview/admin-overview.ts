import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { apiErrorMessage } from '../../../core/services/http-error';
import { PageState } from '../../../shared/ui/page-state';
import { AppIcon, AppIconName } from '../../../shared/ui/app-icon';

@Component({
  selector: 'app-admin-overview', imports: [PageState, RouterLink, AppIcon],
  template: `
    <header class="page-header"><div><span class="eyebrow">System overview</span><h1>Operations at a glance</h1><p>Current account, equipment and alert totals from the Solaris backend.</p></div></header>
    @if (loading()) { <app-page-state kind="loading" title="Loading operations" message="Collecting the latest system totals…" /> }
    @else if (error()) { <app-page-state kind="error" title="Could not load overview" [message]="error()" (retry)="load()" /> }
    @else {
      <section class="metric-grid" aria-label="System totals">
        @for (metric of metrics(); track metric.label) {
          <a class="metric-card metric-card--link" [routerLink]="metric.path" [attr.aria-label]="'Open ' + metric.label"><div class="metric-card__head"><span>{{ metric.label }}</span><span class="metric-card__symbol"><app-icon [name]="metric.icon" /></span></div><strong>{{ metric.value }}</strong><small>{{ metric.note }} <span aria-hidden="true">→</span></small></a>
        }
      </section>
      <section class="content-grid content-grid--two">
        <article class="panel"><div class="panel__header"><div><span class="eyebrow">System posture</span><h2>Monitoring status</h2></div><span class="badge badge--success">Operational</span></div>
          <div class="health-list"><div><span>Authentication</span><strong>JWT protected</strong></div><div><span>Role enforcement</span><strong>Active</strong></div><div><span>Alert monitoring</span><strong>Configured</strong></div></div>
        </article>
        <article class="panel"><div class="panel__header"><div><span class="eyebrow">Attention</span><h2>Open alerts</h2></div></div><p class="large-number">{{ unreadAlerts() }}</p><p class="muted">Unread alerts across all homeowner accounts. Review severity and ownership from the Alerts workspace.</p><a class="text-link" routerLink="/admin/alerts">Review alerts →</a></article>
      </section>
    }
  `,
})
export class AdminOverview {
  private readonly api = inject(AdminApiService);
  readonly loading = signal(true); readonly error = signal(''); readonly unreadAlerts = signal(0);
  readonly metrics = signal<{ label: string; value: number; note: string; icon: AppIconName; path: string }[]>([]);
  constructor() { this.load(); }
  load(): void {
    this.loading.set(true); this.error.set('');
    forkJoin({ users: this.api.users(0, 1), sites: this.api.sites(0, 1), devices: this.api.devices({ size: 1 }), batteries: this.api.batteries(0, 1), alerts: this.api.alerts({ size: 1, read: false }) }).subscribe({
      next: ({ users, sites, devices, batteries, alerts }) => { this.unreadAlerts.set(alerts.totalElements); this.metrics.set([
        { label: 'Users', value: users.totalElements, note: 'Managed accounts', icon: 'users', path: '/admin/users' }, { label: 'Solar sites', value: sites.totalElements, note: 'Registered installations', icon: 'sites', path: '/admin/sites' },
        { label: 'Devices', value: devices.totalElements, note: 'Field equipment', icon: 'devices', path: '/admin/devices' }, { label: 'Batteries', value: batteries.totalElements, note: 'Storage assets', icon: 'battery', path: '/admin/batteries' },
      ]); this.loading.set(false); },
      error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); },
    });
  }
}
