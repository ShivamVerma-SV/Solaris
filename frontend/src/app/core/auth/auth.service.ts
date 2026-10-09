import { HttpClient } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, RegisterRequest } from '../models/api.models';

const STORAGE_KEY = 'solaris.auth';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private refreshInFlight: Observable<string> | null = null;
  readonly session = signal<LoginResponse | null>(this.readSession());
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly user = computed(() => {
    const session = this.session();
    return session ? { id: session.userId, name: session.name, email: session.email, role: session.role } : null;
  });

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, request).pipe(tap((response) => this.save(response)));
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/auth/register`, request);
  }

  refreshAccessToken(): Observable<string> {
    // Share one rotation request between concurrent 401 responses. Sending the same single-use
    // refresh token twice would cause one request to revoke the session unexpectedly.
    if (this.refreshInFlight) return this.refreshInFlight;
    const refreshToken = this.session()?.refreshToken;
    if (!refreshToken) return throwError(() => new Error('No refresh token is available'));

    this.refreshInFlight = this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/refresh`, { refreshToken }).pipe(
      tap((response) => this.save(response)),
      map((response) => response.accessToken),
      catchError((error: unknown) => {
        this.clear();
        void this.router.navigate(['/login'], { queryParams: { session: 'expired' } });
        return throwError(() => error);
      }),
      finalize(() => { this.refreshInFlight = null; }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.refreshInFlight;
  }

  logout(): Observable<void> {
    const refreshToken = this.session()?.refreshToken;
    const request = refreshToken
      ? this.http.post<void>(`${environment.apiBaseUrl}/auth/logout`, { refreshToken }).pipe(catchError(() => of(undefined)))
      : of(undefined);
    return request.pipe(finalize(() => {
      // Local cleanup must still happen if Redis or the API is temporarily unavailable.
      this.clear();
      void this.router.navigate(['/login']);
    }));
  }

  redirectForRole(): string {
    return this.session()?.role === 'ADMIN' ? '/admin' : '/homeowner';
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }

  private save(response: LoginResponse): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    this.session.set(response);
  }

  private readSession(): LoginResponse | null {
    try {
      const value = localStorage.getItem(STORAGE_KEY);
      if (!value) return null;
      const parsed = JSON.parse(value) as Partial<LoginResponse>;
      // Treat malformed or incomplete browser storage as signed-out state instead of propagating it.
      if (!parsed.accessToken || !parsed.refreshToken || (parsed.role !== 'ADMIN' && parsed.role !== 'HOMEOWNER')) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return parsed as LoginResponse;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
