import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { TenantHomePageComponent } from './tenant-home-page.component';

describe('TenantHomePageComponent language', () => {
  const status = signal<'ready' | 'loading' | 'idle'>('ready');
  const user = signal<{ firstName: string; email: string; tenantSlug: string } | null>(
    { firstName: 'Ada', email: 'ada@example.test', tenantSlug: 'tenant-a' }
  );
  let state: TenantLanguageStateService;

  beforeEach(() => {
    status.set('ready');
    user.set({ firstName: 'Ada', email: 'ada@example.test', tenantSlug: 'tenant-a' });
    TestBed.configureTestingModule({ providers: [
      provideRouter([]),
      { provide: AuthFacade, useValue: { status, currentUser: user } },
      { provide: PermissionService, useValue: { isCurrentOwner: () => true, hasPermission: () => true } },
      { provide: AuthStorageService, useValue: { getSession: () => ({ scope: 'TENANT', loginId: 'login-a', tenantSlug: 'tenant-a' }) } },
    ] });
    state = TestBed.inject(TenantLanguageStateService);
    state.enterLayout();
    state.activate('login-a:tenant-a');
  });

  it('renders Spanish and reacts to confirmed English without changing data or links', () => {
    state.confirmFetched('login-a:tenant-a', state.getRevision(), 'es');
    const fixture = TestBed.createComponent(TenantHomePageComponent);
    fixture.detectChanges();
    const beforeLinks = Array.from(fixture.nativeElement.querySelectorAll('a'), (link: Element) => link.getAttribute('href'));
    expect(fixture.nativeElement.textContent).toContain('Hola, Ada');
    expect(fixture.nativeElement.textContent).toContain('Organización: tenant-a');
    expect(fixture.nativeElement.textContent).toContain('Abrir sección');
    expect(state.confirmSaved('login-a:tenant-a', 'en')).toBeTrue();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Hello, Ada');
    expect(fixture.nativeElement.textContent).toContain('Organization: tenant-a');
    expect(fixture.nativeElement.textContent).toContain('Open section');
    expect(fixture.nativeElement.textContent).toContain('ada@example.test');
    expect(Array.from(fixture.nativeElement.querySelectorAll('a'), (link: Element) => link.getAttribute('href'))).toEqual(beforeLinks);
  });

  it('translates loading and unverifiable session states without changing access logic', () => {
    const fixture = TestBed.createComponent(TenantHomePageComponent);
    status.set('loading');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Verificando tu sesión');
    state.confirmFetched('login-a:tenant-a', state.getRevision(), 'en');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Checking your session');
    status.set('ready');
    user.set(null);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('We could not verify your session');
    expect(fixture.nativeElement.querySelectorAll('a').length).toBe(0);
  });
});
