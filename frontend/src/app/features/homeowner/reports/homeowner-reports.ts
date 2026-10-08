import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DailyEnergyReport, Site } from '../../../core/models/api.models';
import { HomeownerApiService } from '../../../core/services/homeowner-api.service';
import { apiErrorMessage } from '../../../core/services/http-error';
import { PageState } from '../../../shared/ui/page-state';

function dateValue(date: Date): string { return date.toISOString().slice(0, 10); }

@Component({
  selector: 'app-homeowner-reports', imports: [ReactiveFormsModule, DecimalPipe, PageState],
  template: `
    <header class="page-header"><div><span class="eyebrow">Daily reporting</span><h1>Energy reports</h1><p>Daily backend aggregates for production, consumption and grid exchange.</p></div></header>
    <form class="filter-bar" [formGroup]="filters" (ngSubmit)="load()"><label><span>Site</span><select formControlName="siteId"><option value="">All sites</option>@for (site of sites(); track site.id) { <option [value]="site.id">{{ site.name }}</option> }</select></label><label><span>From</span><input type="date" formControlName="from" /></label><label><span>To</span><input type="date" formControlName="to" /></label><button class="button button--primary" type="submit">Generate report</button></form>
    @if (loading()) { <app-page-state kind="loading" title="Generating report" message="Aggregating daily energy totals…" /> } @else if (error()) { <app-page-state kind="error" title="Could not generate report" [message]="error()" (retry)="load()" /> } @else if (!rows().length) { <app-page-state kind="empty" title="No report data" message="No energy readings were found in this date range." /> } @else {
      <section class="metric-grid"><article class="metric-card metric-card--production"><span>Total production</span><strong>{{ totals().production | number:'1.1-2' }} <small>kWh</small></strong></article><article class="metric-card"><span>Total consumption</span><strong>{{ totals().consumption | number:'1.1-2' }} <small>kWh</small></strong></article><article class="metric-card"><span>Grid balance</span><strong>{{ totals().gridBalance | number:'1.1-2' }} <small>kWh</small></strong><small>Export minus import</small></article><article class="metric-card"><span>Reading count</span><strong>{{ totals().readings }}</strong></article></section>
      <section class="panel panel--table"><div class="panel__header"><div><span class="eyebrow">Daily breakdown</span><h2>{{ rows().length }} report rows</h2></div></div><div class="table-scroll"><table><thead><tr><th>Date</th><th>Site</th><th>Production</th><th>Consumption</th><th>Grid import</th><th>Grid export</th><th>Readings</th></tr></thead><tbody>@for (row of rows(); track row.date + '-' + row.siteId) { <tr><td data-label="Date">{{ row.date }}</td><td data-label="Site">{{ row.siteName }}</td><td data-label="Production">{{ row.productionKwh | number:'1.1-3' }} kWh</td><td data-label="Consumption">{{ row.consumptionKwh | number:'1.1-3' }} kWh</td><td data-label="Import">{{ row.gridImportKwh | number:'1.1-3' }} kWh</td><td data-label="Export">{{ row.gridExportKwh | number:'1.1-3' }} kWh</td><td data-label="Readings">{{ row.readingCount }}</td></tr> }</tbody></table></div></section>
    }
  `,
})
export class HomeownerReports {
  private readonly api = inject(HomeownerApiService);
  readonly sites = signal<Site[]>([]); readonly rows = signal<DailyEnergyReport[]>([]); readonly loading = signal(true); readonly error = signal('');
  readonly filters = new FormGroup({ siteId: new FormControl<number | ''>('', { nonNullable: true }), from: new FormControl(dateValue(new Date(Date.now() - 29 * 86400000)), { nonNullable: true }), to: new FormControl(dateValue(new Date()), { nonNullable: true }) });
  readonly totals = computed(() => this.rows().reduce((sum, row) => ({ production: sum.production + row.productionKwh, consumption: sum.consumption + row.consumptionKwh, gridBalance: sum.gridBalance + row.gridExportKwh - row.gridImportKwh, readings: sum.readings + row.readingCount }), { production: 0, consumption: 0, gridBalance: 0, readings: 0 }));
  constructor() { this.api.sites().subscribe((sites) => this.sites.set(sites)); this.load(); }
  load(): void { const value = this.filters.getRawValue(); this.loading.set(true); this.error.set(''); this.api.reports({ siteId: value.siteId || null, from: value.from, to: value.to }).subscribe({ next: (rows) => { this.rows.set(rows); this.loading.set(false); }, error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); } }); }
}
