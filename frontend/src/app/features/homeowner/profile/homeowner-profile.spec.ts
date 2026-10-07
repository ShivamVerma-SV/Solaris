import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HomeownerApiService } from '../../../core/services/homeowner-api.service';
import { ToastService } from '../../../core/services/toast.service';
import { HomeownerProfile } from './homeowner-profile';

describe('HomeownerProfile password change', () => {
  const changePassword = vi.fn(() => of(undefined));
  const showToast = vi.fn();

  beforeEach(async () => {
    changePassword.mockClear();
    showToast.mockClear();
    await TestBed.configureTestingModule({
      imports: [HomeownerProfile],
      providers: [
        {
          provide: HomeownerApiService,
          useValue: {
            profile: () => of({ id: 1, name: 'Solar Owner', email: 'owner@example.com', phone: null, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' }),
            updateProfile: vi.fn(),
            changePassword,
          },
        },
        { provide: ToastService, useValue: { show: showToast } },
      ],
    }).compileComponents();
  });

  it('sends only the current and new passwords and clears the form on success', () => {
    const fixture = TestBed.createComponent(HomeownerProfile);
    const component = fixture.componentInstance;
    component.passwordForm.setValue({
      currentPassword: 'current-password',
      newPassword: 'updated-password',
      confirmPassword: 'updated-password',
    });

    component.changePassword();

    expect(changePassword).toHaveBeenCalledWith({
      currentPassword: 'current-password',
      newPassword: 'updated-password',
    });
    expect(component.passwordForm.getRawValue()).toEqual({
      currentPassword: '', newPassword: '', confirmPassword: '',
    });
    expect(showToast).toHaveBeenCalledWith('Password changed successfully.', 'success');
  });

  it('does not submit when the new password confirmation does not match', () => {
    const component = TestBed.createComponent(HomeownerProfile).componentInstance;
    component.passwordForm.setValue({
      currentPassword: 'current-password',
      newPassword: 'updated-password',
      confirmPassword: 'different-password',
    });

    component.changePassword();

    expect(changePassword).not.toHaveBeenCalled();
    expect(component.passwordForm.hasError('passwordMismatch')).toBe(true);
  });
});
