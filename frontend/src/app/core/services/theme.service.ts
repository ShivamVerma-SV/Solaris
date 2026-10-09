import { DOCUMENT } from '@angular/common';
import { Service, inject, signal } from '@angular/core';

export type Theme = 'light' | 'dark';
const THEME_KEY = 'solaris.theme';

@Service()
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  readonly current = signal<Theme>('light');

  initialize(): void {
    const saved = localStorage.getItem(THEME_KEY);
    const systemDark = typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches;
    // An explicit choice wins; otherwise the first visit follows the operating-system preference.
    this.apply(saved === 'light' || saved === 'dark' ? saved : systemDark ? 'dark' : 'light');
  }

  toggle(): void {
    const next = this.current() === 'light' ? 'dark' : 'light';
    localStorage.setItem(THEME_KEY, next);
    this.apply(next);
  }

  private apply(theme: Theme): void {
    this.current.set(theme);
    this.document.documentElement.dataset['theme'] = theme;
    // This also gives native controls and scrollbars a theme that matches the application shell.
    this.document.documentElement.style.colorScheme = theme;
  }
}
