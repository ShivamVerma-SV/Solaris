import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

const AUTH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const isApiRequest = request.url.startsWith(environment.apiBaseUrl);
  const isAuthRequest = AUTH_ENDPOINTS.some((path) => request.url.endsWith(path));
  const token = auth.session()?.accessToken;
  const authenticatedRequest = isApiRequest && !isAuthRequest && token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !isApiRequest || isAuthRequest || !token) {
        return throwError(() => error);
      }
      return auth.refreshAccessToken().pipe(
        switchMap((newToken) => next(request.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } }))),
      );
    }),
  );
};
