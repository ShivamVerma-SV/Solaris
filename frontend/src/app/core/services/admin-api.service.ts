import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AdminUser, Alert, AlertSeverity, AlertType, Battery, BatteryRequest, CreateUserRequest, Device, DeviceRequest, DeviceStatus, DeviceType, PageResponse, Site, SiteRequest, SystemSettings, UpdateUserRequest } from '../models/api.models';

function params(values: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(values)) if (value !== null && value !== undefined && value !== '') result = result.set(key, value);
  return result;
}

@Service()
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/admin`;

  users(page = 0, size = 20) { return this.http.get<PageResponse<AdminUser>>(`${this.base}/users`, { params: params({ page, size }) }); }
  user(id: number) { return this.http.get<AdminUser>(`${this.base}/users/${id}`); }
  createUser(request: CreateUserRequest) { return this.http.post<AdminUser>(`${this.base}/users`, request); }
  updateUser(id: number, request: UpdateUserRequest) { return this.http.put<AdminUser>(`${this.base}/users/${id}`, request); }
  deleteUser(id: number) { return this.http.delete<void>(`${this.base}/users/${id}`); }

  devices(query: { page?: number; size?: number; status?: DeviceStatus | ''; type?: DeviceType | ''; siteId?: number | null } = {}) {
    return this.http.get<PageResponse<Device>>(`${this.base}/devices`, { params: params({ page: query.page ?? 0, size: query.size ?? 20,
       status: query.status, type: query.type, siteId: query.siteId 
      }) });
  }
  createDevice(request: DeviceRequest) { return this.http.post<Device>(`${this.base}/devices`, request); }
  updateDevice(id: number, request: DeviceRequest) { return this.http.put<Device>(`${this.base}/devices/${id}`, request); }
  deleteDevice(id: number) { return this.http.delete<void>(`${this.base}/devices/${id}`); }

  sites(page = 0, size = 100) { return this.http.get<PageResponse<Site>>(`${this.base}/sites`, { params: params({ page, size }) }); }
  createSite(request: SiteRequest) { return this.http.post<Site>(`${this.base}/sites`, request); }
  updateSite(id: number, request: SiteRequest) { return this.http.put<Site>(`${this.base}/sites/${id}`, request); }
  deleteSite(id: number) { return this.http.delete<void>(`${this.base}/sites/${id}`); }

  batteries(page = 0, size = 100) { return this.http.get<PageResponse<Battery>>(`${this.base}/batteries`, { params: params({ page, size }) }); }
  createBattery(request: BatteryRequest) { return this.http.post<Battery>(`${this.base}/batteries`, request); }
  updateBattery(id: number, request: BatteryRequest) { return this.http.put<Battery>(`${this.base}/batteries/${id}`, request); }
  deleteBattery(id: number) { return this.http.delete<void>(`${this.base}/batteries/${id}`); }

  settings() { return this.http.get<SystemSettings>(`${this.base}/settings`); }
  updateSettings(request: SystemSettings) { return this.http.put<SystemSettings>(`${this.base}/settings`, request); }
  alerts(query: { page?: number; size?: number; type?: AlertType | ''; severity?: AlertSeverity | ''; read?: boolean | null; userId?: number | null } = {}) {
    return this.http.get<PageResponse<Alert>>(`${this.base}/alerts`, { params: params({ page: query.page ?? 0, size: query.size ?? 20,
       type: query.type, severity: query.severity, read: query.read, userId: query.userId 
      }) });
  }
}
