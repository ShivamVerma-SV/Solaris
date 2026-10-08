import { Component, input } from '@angular/core';

export type AppIconName =
  | 'alerts'
  | 'battery'
  | 'chevron-down'
  | 'close'
  | 'devices'
  | 'energy'
  | 'logout'
  | 'menu'
  | 'moon'
  | 'overview'
  | 'profile'
  | 'reports'
  | 'settings'
  | 'sites'
  | 'sun'
  | 'users';

@Component({
  selector: 'app-icon',
  host: { 'aria-hidden': 'true' },
  template: `
    <svg class="app-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" focusable="false">
      @switch (name()) {
        @case ('overview') { <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/> }
        @case ('users') { <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/> }
        @case ('sites') { <path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/> }
        @case ('devices') { <rect x="5" y="5" width="14" height="14" rx="2"/><rect x="9" y="9" width="6" height="6"/><path d="M9 2v3M15 2v3M9 19v3M15 19v3M19 9h3M19 14h3M2 9h3M2 14h3"/> }
        @case ('energy') { <path d="m13 2-9 12h8l-1 8 9-12h-8l1-8Z"/> }
        @case ('battery') { <rect x="2" y="6" width="18" height="12" rx="2"/><path d="M22 10v4M6 10v4M10 10v4M14 10v4"/> }
        @case ('alerts') { <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/> }
        @case ('reports') { <path d="M4 19V9M10 19V5M16 19v-7M22 19V2M2 22h21"/> }
        @case ('settings') { <circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.93 4.93l2.12 2.12M16.95 16.95l2.12 2.12M19.07 4.93l-2.12 2.12M7.05 16.95l-2.12 2.12"/> }
        @case ('profile') { <circle cx="12" cy="8" r="4"/><path d="M4 22a8 8 0 0 1 16 0"/> }
        @case ('logout') { <path d="M10 17l5-5-5-5M15 12H3M21 19V5a2 2 0 0 0-2-2h-6"/> }
        @case ('menu') { <path d="M4 7h16M4 12h16M4 17h16"/> }
        @case ('close') { <path d="m6 6 12 12M18 6 6 18"/> }
        @case ('sun') { <circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.66 6.34l1.41-1.41"/> }
        @case ('moon') { <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z"/> }
        @case ('chevron-down') { <path d="m7 10 5 5 5-5"/> }
      }
    </svg>
  `,
})
export class AppIcon {
  readonly name = input.required<AppIconName>();
}
