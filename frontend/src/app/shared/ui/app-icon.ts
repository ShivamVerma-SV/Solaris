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
  templateUrl: './app-icon.component.html',
})
export class AppIcon {
  readonly name = input.required<AppIconName>();
}
