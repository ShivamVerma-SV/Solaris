import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api.models';

export function apiErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiError> | string | null;
    if (typeof body === 'object' && body?.message) return body.message;
    if (typeof body === 'string' && body.trim()) return body;
    if (error.status === 0) return 'Unable to reach Solaris services. Check that the backend is running.';
  }
  return fallback;
}
