import { Routes } from '@angular/router';
import { adminGuard, authGuard, homeownerGuard, publicOnlyGuard } from './core/guards/auth.guards';
import { AppShell } from './layout/app-shell';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', canActivate: [publicOnlyGuard], loadComponent: () => import('./features/auth/login').then((module) => module.Login), title: 'Sign in · Solaris' },
  { path: 'register', canActivate: [publicOnlyGuard], loadComponent: () => import('./features/auth/register').then((module) => module.Register), title: 'Create account · Solaris' },
  {
    path: 'admin', component: AppShell, canActivate: [authGuard, adminGuard], children: [
      { path: '', loadComponent: () => import('./features/admin/overview/admin-overview').then((module) => module.AdminOverview), title: 'System overview · Solaris' },
      { path: 'users', loadComponent: () => import('./features/admin/users/admin-users').then((module) => module.AdminUsers), title: 'Users · Solaris' },
      { path: 'sites', loadComponent: () => import('./features/admin/sites/admin-sites').then((module) => module.AdminSites), title: 'Solar sites · Solaris' },
      { path: 'devices', loadComponent: () => import('./features/admin/devices/admin-devices').then((module) => module.AdminDevices), title: 'Devices · Solaris' },
      { path: 'batteries', loadComponent: () => import('./features/admin/batteries/admin-batteries').then((module) => module.AdminBatteries), title: 'Batteries · Solaris' },
      { path: 'alerts', loadComponent: () => import('./features/admin/alerts/admin-alerts').then((module) => module.AdminAlerts), title: 'Alerts · Solaris' },
      { path: 'settings', loadComponent: () => import('./features/admin/settings/admin-settings').then((module) => module.AdminSettings), title: 'Settings · Solaris' },
    ],
  },
  {
    path: 'homeowner', component: AppShell, canActivate: [authGuard, homeownerGuard], children: [
      { path: '', loadComponent: () => import('./features/homeowner/dashboard/homeowner-dashboard').then((module) => module.HomeownerDashboard), title: 'Energy overview · Solaris' },
      { path: 'energy', loadComponent: () => import('./features/homeowner/energy/homeowner-energy').then((module) => module.HomeownerEnergy), title: 'Energy · Solaris' },
      { path: 'storage', loadComponent: () => import('./features/homeowner/storage/homeowner-storage').then((module) => module.HomeownerStorage), title: 'Storage · Solaris' },
      { path: 'alerts', loadComponent: () => import('./features/homeowner/alerts/homeowner-alerts').then((module) => module.HomeownerAlerts), title: 'Alerts · Solaris' },
      { path: 'reports', loadComponent: () => import('./features/homeowner/reports/homeowner-reports').then((module) => module.HomeownerReports), title: 'Reports · Solaris' },
      { path: 'profile', loadComponent: () => import('./features/homeowner/profile/homeowner-profile').then((module) => module.HomeownerProfile), title: 'Profile · Solaris' },
    ],
  },
  { path: '**', loadComponent: () => import('./features/not-found').then((module) => module.NotFound), title: 'Page not found · Solaris' },
];
