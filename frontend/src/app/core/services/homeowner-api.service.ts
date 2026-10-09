import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Alert, Battery, ChangePasswordRequest, DailyEnergyReport, EnergyReading, EnergySummary, PageResponse, Profile, Site, StorageReading, TimeRangeQuery, UpdateProfileRequest } from '../models/api.models';

function params(values: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(values)) if (value !== null && value !== undefined && value !== '') result = result.set(key, value);
  return result;
}

@Service()
export class HomeownerApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/homeowner`;

  profile() { return this.http.get<Profile>(`${this.base}/profile`); }
  updateProfile(request: UpdateProfileRequest) { return this.http.put<Profile>(`${this.base}/profile`, request); }
  changePassword(request: ChangePasswordRequest) { return this.http.put<void>(`${this.base}/profile/password`, request); }
  sites() { return this.http.get<Site[]>(`${this.base}/sites`); }
  energySummary(query: TimeRangeQuery = {}) { return this.http.get<EnergySummary>(`${this.base}/energy/summary`, { params: params({ ...query }) }); }
  energyReadings(query: TimeRangeQuery = {}) { 
    return this.http.get<PageResponse<EnergyReading>>(`${this.base}/energy/readings`, { params: params({ page: query.page ?? 0,
       size: query.size ?? 50, siteId: query.siteId, from: query.from, to: query.to }) }); 
  }
  storageStatus() { return this.http.get<Battery[]>(`${this.base}/storage/status`); }
  storageHistory(query: { page?: number; size?: number; batteryId?: number | null; from?: string; to?: string } = {}) {
     return this.http.get<PageResponse<StorageReading>>(`${this.base}/storage/history`, { params: params({ page: query.page ?? 0,
       size: query.size ?? 50, ...query 
      }) }); 
    }
  alerts(query: { page?: number; size?: number; read?: boolean | null } = {}) { 
    return this.http.get<PageResponse<Alert>>(`${this.base}/alerts`, { params: params({ page: query.page ?? 0,
       size: query.size ?? 20, read: query.read }) }); 
  }
  markAlertRead(id: number) { return this.http.put<Alert>(`${this.base}/alerts/${id}/read`, {}); }
  reports(query: { siteId?: number | null; from?: string; to?: string } = {}) { 
    return this.http.get<DailyEnergyReport[]>(`${this.base}/reports/energy`, { params: params(query) }); 
  }
}
