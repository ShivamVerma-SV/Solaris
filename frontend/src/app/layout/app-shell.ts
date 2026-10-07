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
  template: `
    <div class="app-shell" [class.app-shell--open]="menuOpen()">
      <button class="sidebar-scrim" type="button" aria-label="Close navigation" (click)="menuOpen.set(false)"></button>
      <aside class="sidebar" aria-label="Primary navigation">
        <div class="sidebar__brand"><app-brand /><button class="icon-button sidebar__close" type="button" aria-label="Close navigation" (click)="menuOpen.set(false)"><app-icon name="close" /></button></div>
        <nav class="nav-list">
          <span class="nav-label">Workspace</span>
          @for (item of navItems(); track item.path) {
            <a [routerLink]="item.path" routerLinkActive="nav-link--active" [routerLinkActiveOptions]="{ exact: item.path === basePath() }" class="nav-link" (click)="menuOpen.set(false)">
              <span class="nav-link__icon" [class.nav-link__icon--emphasized]="item.emphasizedIcon"><app-icon [name]="item.icon" /></span><span>{{ item.label }}</span>
            </a>
          }
        </nav>
        <div class="sidebar__status"><span class="status-dot status-dot--online"></span><div><strong>API connected</strong><small>Solaris operations</small></div></div>
      </aside>
      <div class="shell-main">
        <header class="topbar">
          <button class="icon-button menu-button" type="button" aria-label="Open navigation" (click)="menuOpen.set(true)"><app-icon name="menu" /></button>
          <div class="topbar__title"><strong>{{ areaLabel() }}</strong><span>Operational workspace</span></div>
          <div class="topbar__actions">
            <button class="theme-button" type="button" (click)="theme.toggle()" [attr.aria-label]="theme.current() === 'dark' ? 'Use light theme' : 'Use dark theme'"><app-icon [name]="theme.current() === 'dark' ? 'sun' : 'moon'" /></button>
            @if (userMenuOpen()) { <button class="user-menu-scrim" type="button" aria-label="Close user menu" (click)="closeUserMenu()"></button> }
            <div class="user-menu-wrap" (keydown.escape)="closeUserMenu(true)">
              <button #userMenuTrigger class="user-chip" type="button" [attr.aria-expanded]="userMenuOpen()" aria-controls="user-menu" [attr.aria-label]="'Open user menu for ' + auth.user()?.name" (click)="toggleUserMenu()">
                <span class="avatar" aria-hidden="true">{{ initials() }}</span><span><strong>{{ auth.user()?.name }}</strong><small>{{ auth.user()?.role === 'ADMIN' ? 'Administrator' : 'Homeowner' }}</small></span><app-icon name="chevron-down" />
              </button>
              @if (userMenuOpen()) {
                <section id="user-menu" class="user-menu" role="menu" aria-label="User account">
                  <div class="user-menu__identity"><strong>{{ auth.user()?.name }}</strong><span>{{ auth.user()?.email }}</span><small>{{ auth.user()?.role === 'ADMIN' ? 'Administrator' : 'Homeowner' }}</small></div>
                  <button #userMenuAction class="user-menu__action" type="button" role="menuitem" (click)="logout()"><app-icon name="logout" /><span>Sign out</span></button>
                </section>
              }
            </div>
          </div>
        </header>
        <main class="page-content" id="main-content"><router-outlet /></main>
      </div>
    </div>
  `,
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
      if (this.userMenuOpen()) queueMicrotask(() => this.userMenuAction()?.nativeElement.focus());
    });
  }

  toggleUserMenu(): void { this.userMenuOpen.update((open) => !open); }
  closeUserMenu(restoreFocus = false): void { this.userMenuOpen.set(false); if (restoreFocus) queueMicrotask(() => this.userMenuTrigger()?.nativeElement.focus()); }
  logout(): void { this.userMenuOpen.set(false); this.auth.logout().subscribe(); }
}
