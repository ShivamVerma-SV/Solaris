import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { apiErrorMessage } from '../../../core/services/http-error';
import { ToastService } from '../../../core/services/toast.service';
import { PageState } from '../../../shared/ui/page-state';

@Component({
  selector: 'app-admin-settings', imports: [ReactiveFormsModule, PageState],
  template: `
    <header class="page-header"><div><span class="eyebrow">Monitoring policy</span><h1>System settings</h1><p>Configure the thresholds and notification switches used by backend alert monitoring.</p></div></header>
    @if (loading()) { <app-page-state kind="loading" title="Loading settings" message="Retrieving monitoring policy…" /> } @else if (error()) { <app-page-state kind="error" title="Could not load settings" [message]="error()" (retry)="load()" /> } @else {
      <form [formGroup]="form" (ngSubmit)="save()" class="settings-layout">
        <section class="panel"><div class="panel__header"><div><span class="eyebrow">Thresholds</span><h2>Alert trigger values</h2></div></div><div class="form-grid"><label class="field"><span>Low battery threshold (%)</span><input type="number" min="0" max="100" step="0.1" formControlName="lowBatteryThreshold" /><small>0–100 percent</small></label><label class="field"><span>High consumption (kWh)</span><input type="number" min="0" step="0.001" formControlName="highConsumptionThresholdKwh" /></label><label class="field"><span>Low production (kWh)</span><input type="number" min="0" step="0.001" formControlName="lowProductionThresholdKwh" /></label><label class="field"><span>Device offline after (minutes)</span><input type="number" min="1" max="10080" formControlName="deviceOfflineThresholdMinutes" /><small>1 minute to 7 days</small></label></div></section>
        <section class="panel"><div class="panel__header"><div><span class="eyebrow">Rules</span><h2>Alert and notification controls</h2></div></div><div class="switch-list"><label><span><strong>Production alerts</strong><small>Flag low production readings</small></span><input type="checkbox" role="switch" formControlName="productionAlertsEnabled" /></label><label><span><strong>Consumption alerts</strong><small>Flag high consumption readings</small></span><input type="checkbox" role="switch" formControlName="consumptionAlertsEnabled" /></label><label><span><strong>Battery alerts</strong><small>Monitor configured charge threshold</small></span><input type="checkbox" role="switch" formControlName="batteryAlertsEnabled" /></label><label><span><strong>Offline device alerts</strong><small>Monitor device last-seen timestamps</small></span><input type="checkbox" role="switch" formControlName="deviceOfflineAlertEnabled" /></label><label><span><strong>Email notifications</strong><small>Enable backend email notification delivery</small></span><input type="checkbox" role="switch" formControlName="emailNotificationsEnabled" /></label></div></section>
        <div class="settings-actions"><span class="muted">Changes apply to backend monitoring after saving.</span><button class="button button--primary" type="submit" [disabled]="saving()">{{ saving() ? 'Saving settings…' : 'Save settings' }}</button></div>
      </form>
    }
  `,
})
export class AdminSettings {
  private readonly api = inject(AdminApiService); private readonly toast = inject(ToastService);
  readonly loading = signal(true); readonly saving = signal(false); readonly error = signal('');
  readonly form = new FormGroup({ lowBatteryThreshold: new FormControl<number | null>(null, [Validators.required, Validators.min(0), Validators.max(100)]), highConsumptionThresholdKwh: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]), lowProductionThresholdKwh: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]), productionAlertsEnabled: new FormControl(false, { nonNullable: true }), consumptionAlertsEnabled: new FormControl(false, { nonNullable: true }), batteryAlertsEnabled: new FormControl(false, { nonNullable: true }), deviceOfflineAlertEnabled: new FormControl(false, { nonNullable: true }), deviceOfflineThresholdMinutes: new FormControl<number | null>(null, [Validators.required, Validators.min(1), Validators.max(10080)]), emailNotificationsEnabled: new FormControl(false, { nonNullable: true }) });
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.api.settings().subscribe({ next: (settings) => { this.form.reset(settings); this.loading.set(false); }, error: (error: unknown) => { this.error.set(apiErrorMessage(error)); this.loading.set(false); } }); }
  save(): void { this.form.markAllAsTouched(); const value = this.form.getRawValue(); if (this.form.invalid || value.lowBatteryThreshold === null || value.highConsumptionThresholdKwh === null || value.lowProductionThresholdKwh === null || value.deviceOfflineThresholdMinutes === null) return; this.saving.set(true); this.api.updateSettings({ ...value, lowBatteryThreshold: value.lowBatteryThreshold, highConsumptionThresholdKwh: value.highConsumptionThresholdKwh, lowProductionThresholdKwh: value.lowProductionThresholdKwh, deviceOfflineThresholdMinutes: value.deviceOfflineThresholdMinutes }).pipe(finalize(() => this.saving.set(false))).subscribe({ next: (settings) => { this.form.reset(settings); this.toast.show('System settings updated.', 'success'); }, error: (error: unknown) => this.toast.show(apiErrorMessage(error), 'error') }); }
}
