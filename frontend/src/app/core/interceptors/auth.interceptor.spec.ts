import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { LoginResponse } from '../models/api.models';
import { authInterceptor } from './auth.interceptor';

const session: LoginResponse = {
  accessToken: 'old-access',
  refreshToken: 'old-refresh',
  tokenType: 'Bearer',
  accessTokenExpiresInSeconds: 900,
  refreshTokenExpiresInSeconds: 604800,
  userId: 12,
  name: 'Solar Owner',
  email: 'owner@example.com',
  role: 'HOMEOWNER',
};

describe('authInterceptor', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.setItem('solaris.auth', JSON.stringify(session));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('attaches the current access token to protected API requests', () => {
    TestBed.inject(HttpClient).get('/api/homeowner/profile').subscribe();

    const request = httpTesting.expectOne('/api/homeowner/profile');
    expect(request.request.headers.get('Authorization')).toBe('Bearer old-access');
    request.flush({});
  });

  it('uses one rotating refresh request for simultaneous authentication failures', () => {
    const http = TestBed.inject(HttpClient);
    const results: unknown[] = [];
    http.get('/api/homeowner/profile').subscribe((value) => results.push(value));
    http.get('/api/homeowner/sites').subscribe((value) => results.push(value));

    httpTesting.expectOne('/api/homeowner/profile').flush({}, { status: 401, statusText: 'Unauthorized' });
    httpTesting.expectOne('/api/homeowner/sites').flush({}, { status: 401, statusText: 'Unauthorized' });

    const refresh = httpTesting.expectOne('/api/auth/refresh');
    expect(refresh.request.body).toEqual({ refreshToken: 'old-refresh' });
    refresh.flush({ ...session, accessToken: 'new-access', refreshToken: 'new-refresh' });

    const retriedProfile = httpTesting.expectOne('/api/homeowner/profile');
    const retriedSites = httpTesting.expectOne('/api/homeowner/sites');
    expect(retriedProfile.request.headers.get('Authorization')).toBe('Bearer new-access');
    expect(retriedSites.request.headers.get('Authorization')).toBe('Bearer new-access');
    retriedProfile.flush({ id: 12 });
    retriedSites.flush([]);

    expect(results).toHaveLength(2);
    expect(TestBed.inject(AuthService).session()?.refreshToken).toBe('new-refresh');
  });
});
