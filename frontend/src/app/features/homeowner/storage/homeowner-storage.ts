import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Battery, StorageReading } from '../../../core/models/api.models';
import { HomeownerApiService } from '../../../core/services/homeowner-api.service';
import { apiErrorMessage } from '../../../core/services/http-error';
import { PageState } from '../../../shared/ui/page-state';

function dateValue(date: Date): string { return date.toISOString().slice(0, 10); }

@Component({
  selector: 'app-homeowner-storage', imports: [ReactiveFormsModule, DatePipe, DecimalPipe, PageState],
  template: `
    <header class="page-header"><div><span class="eyebrow">Energy storage</span><h1>Battery systems</h1><p>Current state of charge and historical storage readings.</p></div></header>
    @if (loading()) { <app-page-state kind="loading" title="Loading storage" message="Retrieving battery state and history…" /> } @else if (error()) { <app-page-state kind="error" title="Could not load storage" [message]="error()" (retry)="load()" /> } @else {
      @if (batteries().length) { <section class="battery-grid">@for (battery of batteries(); track battery.id) { <article class="battery-card"><div class="battery-card__gauge"><svg viewBox="0 0 120 120" aria-hidden="true"><circle cx="60" cy="60" r="50" class="gauge-track"/><circle cx="60" cy="60" r="50" class="gauge-value" [style.stroke-dasharray]="battery.currentChargePercent * 3.14 + ' 314'"/></svg><span><strong>{{ battery.currentChargePercent | number:'1.0-1' }}%</strong><small>charged</small></span></div><div><span [class]="battery.status === 'ONLINE' ? 'badge badge--success' : 'badge badge--warning'">{{ battery.status }}</span><h2>{{ battery.name }}</h2><p>{{ battery.siteName }} · {{ battery.identifier }}</p><dl><div><dt>Stored energy</dt><dd>{{ battery.currentStoredEnergyKwh }} kWh</dd></div><div><dt>Total capacity</dt><dd>{{ battery.capacityKwh }} kWh</dd></div><div><dt>Last update</dt><dd>{{ battery.lastUpdatedAt | date:'short' }}</dd></div></dl></div></article> }</section> } @else { <app-page-state kind="empty" title="No battery systems" message="No storage assets are currently assigned to your sites." /> }
      @if (batteries().length) { <form class="filter-bar" [formGroup]="filters" (ngSubmit)="loadHistory()"><label><span>Battery</span><select formControlName="batteryId"><option value="">All batteries</option>@for (battery of batteries(); track battery.id) { <option [value]="battery.id">{{ battery.name }}</option> }</select></label><label><span>From</span><input type="date" formControlName="from" /></label><label><span>To</span><input type="date" formControlName="to" /></label><button class="button button--primary" type="submit">Apply range</button></form>
      <section class="panel panel--table"><div class="panel__header"><div><span class="eyebrow">History</span><h2>Storage readings</h2></div></div>@if (history().length) { <div class="table-scroll"><table><thead><tr><th>Time</th><th>Battery</th><th>Site</th><th>Charge</th><th>Stored energy</th></tr></thead><tbody>@for (reading of history(); track reading.id) { <tr><td data-label="Time">{{ reading.timestamp | date:'short' }}</td><td data-label="Battery">{{ reading.batteryIdentifier }}</td><td data-label="Site">{{ reading.siteName }}</td><td data-label="Charge"><span class="inline-progress"><span [style.width.%]="reading.chargePercent"></span></span>{{ reading.chargePercent }}%</td><td data-label="Stored">{{ reading.storedEnergyKwh }} kWh</td></tr> }</tbody></table></div><div class="pagination"><span>Page {{ page() + 1 }} of {{ totalPages() || 1 }}</span><div><button class="button button--secondary button--small" [disabled]="page() === 0" (click)="change(-1)">Previous</button><button class="button button--secondary button--small" [disabled]="page() + 1 >= totalPages()" (click)="change(1)">Next</button></div></div> } @else { <div class="compact-empty"><strong>No storage readings in this range</strong><span>Try another range or battery.</span></div> }</section> }
    }
  `,
})
export class HomeownerStorage {
  private readonly api = inject(HomeownerApiService);
  readonly batteries = signal<Battery[]>([]); readonly history = signal<StorageReading[]>([]); readonly loading = signal(true); readonly error = signal(''); readonly page = signal(0); readonly totalPages = signal(0);
  readonly filters = new FormGroup({ batteryId: new FormControl<number | ''>('', { nonNullable: true }), from: new FormControl(dateValue(new Date(Date.now() - 7 * 86400000)), { nonNullable: true }), to: new FormControl(dateValue(new Date()), { nonNullable: true }) });
  constructor() { this.load(); }
  load(): void { this.loading.set(true); forkJoin({ batteries: this.api.storageStatus(), history: this.api.storageHistory({ size: 50 }) }).subscribe({ next: ({ batteries, history }) => { this.batteries.set(batteries); this.history.set(history.content); this.totalPages.set(history.totalPages); this.loading.set(false); }, error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); } }); }
  loadHistory(): void { const value = this.filters.getRawValue(); this.loading.set(true); this.api.storageHistory({ batteryId: value.batteryId || null, from: new Date(`${value.from}T00:00:00`).toISOString(), to: new Date(`${value.to}T23:59:59.999`).toISOString(), page: this.page(), size: 50 }).subscribe({ next: (result) => { this.history.set(result.content); this.totalPages.set(result.totalPages); this.loading.set(false); }, error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); } }); }
  change(delta: number): void { this.page.update((value) => value + delta); this.loadHistory(); }
}
