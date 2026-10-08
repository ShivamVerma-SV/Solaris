export type UserRole = 'ADMIN' | 'HOMEOWNER';
export type DeviceStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE' | 'RETIRED';
export type DeviceType = 'SOLAR_INVERTER' | 'SMART_METER' | 'BATTERY_CONTROLLER' | 'WEATHER_SENSOR';
export type BatteryStatus = DeviceStatus;
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type AlertType = 'LOW_BATTERY' | 'HIGH_CONSUMPTION' | 'LOW_PRODUCTION' | 'DEVICE_OFFLINE';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string[]> | null;
}

export interface LoginRequest { email: string; password: string }
export interface RegisterRequest { name: string; email: string; password: string }
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  accessTokenExpiresInSeconds: number;
  refreshTokenExpiresInSeconds: number;
  userId: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface AdminUser {
  id: number; name: string; email: string; role: UserRole; enabled: boolean;
  phone: string | null; createdAt: string; updatedAt: string;
}
export interface CreateUserRequest {
  name: string; email: string; password: string; role: UserRole; phone: string | null; enabled: boolean;
}
export interface UpdateUserRequest extends Omit<CreateUserRequest, 'password'> { password: string | null }

export interface Site {
  id: number; ownerId: number; ownerName: string; code: string; name: string;
  address: string | null; capacityKw: number; active: boolean; createdAt: string; updatedAt: string;
}
export interface SiteRequest {
  ownerId: number; code: string; name: string; address: string | null; capacityKw: number; active: boolean;
}

export interface Device {
  id: number; identifier: string; name: string; type: DeviceType; status: DeviceStatus;
  siteId: number; siteName: string; ownerId: number; manufacturer: string | null; model: string | null;
  firmwareVersion: string | null; lastSeenAt: string | null; createdAt: string; updatedAt: string;
}
export interface DeviceRequest {
  identifier: string; name: string; type: DeviceType; status: DeviceStatus; siteId: number;
  manufacturer: string | null; model: string | null; firmwareVersion: string | null; lastSeenAt: string | null;
}

export interface Battery {
  id: number; identifier: string; name: string; siteId: number; siteName: string; ownerId: number;
  capacityKwh: number; status: BatteryStatus; currentChargePercent: number;
  currentStoredEnergyKwh: number; lastUpdatedAt: string;
}
export interface BatteryRequest {
  identifier: string; name: string; siteId: number; capacityKwh: number; status: BatteryStatus;
  currentChargePercent: number; currentStoredEnergyKwh: number; lastUpdatedAt: string;
}

export interface SystemSettings {
  lowBatteryThreshold: number; highConsumptionThresholdKwh: number; lowProductionThresholdKwh: number;
  productionAlertsEnabled: boolean; consumptionAlertsEnabled: boolean; batteryAlertsEnabled: boolean;
  deviceOfflineAlertEnabled: boolean; deviceOfflineThresholdMinutes: number;
  emailNotificationsEnabled: boolean; updatedAt?: string;
}

export interface Alert {
  id: number; type: AlertType; severity: AlertSeverity; message: string; read: boolean; createdAt: string;
  userId: number; siteId: number | null; deviceId: number | null; batteryId: number | null;
}

export interface Profile { id: number; name: string; email: string; phone: string | null; createdAt: string; updatedAt: string }
export interface UpdateProfileRequest { name: string; email: string; phone: string | null }
export interface ChangePasswordRequest { currentPassword: string; newPassword: string }

export interface EnergySummary {
  siteId: number | null; from: string; to: string; productionKwh: number; consumptionKwh: number;
  gridImportKwh: number; gridExportKwh: number; readingCount: number;
}
export interface EnergyReading {
  id: number; siteId: number; siteName: string; deviceId: number; deviceIdentifier: string;
  productionKwh: number; consumptionKwh: number; gridImportKwh: number; gridExportKwh: number; timestamp: string;
}
export interface StorageReading {
  id: number; siteId: number; siteName: string; batteryId: number; batteryIdentifier: string;
  chargePercent: number; storedEnergyKwh: number; timestamp: string;
}
export interface DailyEnergyReport {
  date: string; siteId: number; siteName: string; productionKwh: number; consumptionKwh: number;
  gridImportKwh: number; gridExportKwh: number; readingCount: number;
}

export interface PageQuery { page?: number; size?: number }
export interface TimeRangeQuery extends PageQuery { siteId?: number | null; from?: string; to?: string }
