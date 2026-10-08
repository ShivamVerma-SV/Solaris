import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { ThemeService } from '../core/services/theme.service';
import { AppShell } from './app-shell';

describe('AppShell user menu', () => {
  const logout = vi.fn(() => of(undefined));

  beforeEach(async () => {
    logout.mockClear();
    await TestBed.configureTestingModule({
      imports: [AppShell],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            session: () => ({ role: 'HOMEOWNER' }),
            user: () => ({ id: 4, name: 'Solar Owner', email: 'owner@example.com', role: 'HOMEOWNER' }),
            logout,
          },
        },
        { provide: ThemeService, useValue: { current: () => 'light', toggle: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('opens from the avatar trigger and exposes account details and logout', async () => {
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.user-chip')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(element.querySelector('.user-menu')?.textContent).toContain('Solar Owner');
    expect(element.querySelector('.user-menu')?.textContent).toContain('owner@example.com');
    expect(element.querySelector('.user-menu')?.textContent).toContain('Homeowner');

    element.querySelector<HTMLButtonElement>('.user-menu__action')?.click();
    expect(logout).toHaveBeenCalledOnce();
  });

  it('closes on Escape and restores focus to the trigger', async () => {
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const trigger = element.querySelector<HTMLButtonElement>('.user-chip');
    trigger?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    element.querySelector<HTMLButtonElement>('.user-menu__action')?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(element.querySelector('.user-menu')).toBeNull();
    expect(document.activeElement).toBe(trigger);
  });
});
