import { Component, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { ThemeService } from '../core/services/theme.service';
import { Brand } from '../shared/ui/brand';
import { AppIcon, AppIconName } from '../shared/ui/app-icon';

interface NavItem { label: string; path: string; icon: AppIconName; emphasizedIcon?: boolean }

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Brand, AppIcon],
  templateUrl: './app-shell.component.html',
})
export class AppShell {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  readonly menuOpen = signal(false);
  readonly userMenuOpen = signal(false);
  private readonly userMenuAction = viewChild<ElementRef<HTMLButtonElement>>('userMenuAction');
  private readonly userMenuTrigger = viewChild<ElementRef<HTMLButtonElement>>('userMenuTrigger');
  readonly basePath = computed(() => this.auth.session()?.role === 'ADMIN' ? '/admin' : '/homeowner');
  readonly areaLabel = computed(() => this.auth.session()?.role === 'ADMIN' ? 'Administration' : 'My energy');
  readonly initials = computed(() => this.auth.user()?.name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() ?? 'S');
  readonly navItems = computed<NavItem[]>(() => this.auth.session()?.role === 'ADMIN' ? [
    { label: 'Overview', path: '/admin', icon: 'overview' }, { label: 'Users', path: '/admin/users', icon: 'users' },
    { label: 'Sites', path: '/admin/sites', icon: 'sites' }, { label: 'Devices', path: '/admin/devices', icon: 'devices' },
    { label: 'Batteries', path: '/admin/batteries', icon: 'battery' }, { label: 'Alerts', path: '/admin/alerts', icon: 'alerts' },
    { label: 'System settings', path: '/admin/settings', icon: 'settings', emphasizedIcon: true },
  ] : [
    { label: 'Overview', path: '/homeowner', icon: 'overview' }, { label: 'Energy', path: '/homeowner/energy', icon: 'energy' },
    { label: 'Storage', path: '/homeowner/storage', icon: 'battery' }, { label: 'Alerts', path: '/homeowner/alerts', icon: 'alerts' },
    { label: 'Reports', path: '/homeowner/reports', icon: 'reports' }, { label: 'Profile', path: '/homeowner/profile', icon: 'profile' },
  ]);

  constructor() {
    effect(() => {
      // Move focus into the opened menu so keyboard users do not have to tab through the page again.
      if (this.userMenuOpen()) queueMicrotask(() => this.userMenuAction()?.nativeElement.focus());
    });
  }

  toggleUserMenu(): void { this.userMenuOpen.update((open) => !open); }
  closeUserMenu(restoreFocus = false): void { this.userMenuOpen.set(false); if (restoreFocus) queueMicrotask(() => this.userMenuTrigger()?.nativeElement.focus()); }
  logout(): void { this.userMenuOpen.set(false); this.auth.logout().subscribe(); }
}
