import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { TenantAccessDeniedPageComponent } from './tenant-access-denied-page.component';

describe('TenantAccessDeniedPageComponent language', () => {
  let state: TenantLanguageStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideRouter([]),
      { provide: AuthStorageService, useValue: { getSession: () => ({ scope: 'TENANT', loginId: 'login-a', tenantSlug: 'tenant-a' }) } },
    ] });
    state = TestBed.inject(TenantLanguageStateService);
    state.enterLayout();
    state.activate('login-a:tenant-a');
  });

  it('renders Spanish, then reacts to English while preserving destination links', () => {
    state.confirmFetched('login-a:tenant-a', state.getRevision(), 'es');
    const fixture = TestBed.createComponent(TenantAccessDeniedPageComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Acceso no disponible');
    expect(fixture.nativeElement.textContent).toContain('No tienes permisos');
    const beforeLinks = Array.from(fixture.nativeElement.querySelectorAll('a'), (link: Element) => link.getAttribute('href'));
    expect(beforeLinks).toEqual(['/dashboard/home', '/dashboard/profile']);
    expect(state.confirmSaved('login-a:tenant-a', 'en')).toBeTrue();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Access unavailable');
    expect(fixture.nativeElement.textContent).toContain('You do not have permission');
    expect(fixture.nativeElement.textContent).toContain('Go to home');
    expect(fixture.nativeElement.textContent).toContain('View my profile');
    expect(Array.from(fixture.nativeElement.querySelectorAll('a'), (link: Element) => link.getAttribute('href'))).toEqual(beforeLinks);
  });
});
