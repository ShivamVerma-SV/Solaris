import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { UserRole } from '../models/api.models';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? true : inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const publicOnlyGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree([auth.redirectForRole()]) : true;
};

function roleGuard(role: UserRole): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (!auth.isAuthenticated()) return inject(Router).createUrlTree(['/login']);
    // Guards improve navigation UX; the backend remains the authority for every role-protected API call.
    return auth.session()?.role === role ? true : inject(Router).createUrlTree([auth.redirectForRole()]);
  };
}

export const adminGuard = roleGuard('ADMIN');
export const homeownerGuard = roleGuard('HOMEOWNER');
