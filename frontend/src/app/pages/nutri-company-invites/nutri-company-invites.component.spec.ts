import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { NutriCompanyInvitesComponent } from './nutri-company-invites.component';
import { environment } from '../../../environments/environment';

describe('NutriCompanyInvitesComponent', () => {
  let component: NutriCompanyInvitesComponent;
  let http: HttpTestingController;
  const url = `${environment.apiUrl}/nutritionist/company-invitations`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NutriCompanyInvitesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(NutriCompanyInvitesComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne(url).flush([]);
  });

  afterEach(() => http.verify());

  it('carrega convites pendentes', () => {
    component.load();
    http.expectOne(url).flush([
      { linkId: 1, token: 'tk1', companyId: 10, companyName: 'ACME',
        invitedAt: '2026-09-19T10:00:00', expiresAt: '2026-10-19T10:00:00' }
    ]);
    expect(component.invites().length).toBe(1);
    expect(component.invites()[0].companyName).toBe('ACME');
  });

  it('accept chama endpoint correto e recarrega lista', () => {
    const inv = { linkId: 1, token: 'tk1', companyId: 10, companyName: 'ACME',
                  invitedAt: '', expiresAt: '' };
    component.accept(inv);
    http.expectOne(`${url}/tk1/accept`).flush(null);
    http.expectOne(url).flush([]);
    expect(component.invites().length).toBe(0);
  });

  it('reject chama endpoint correto e recarrega lista', () => {
    const inv = { linkId: 2, token: 'tk2', companyId: 20, companyName: 'BETA',
                  invitedAt: '', expiresAt: '' };
    component.reject(inv);
    http.expectOne(`${url}/tk2/reject`).flush(null);
    http.expectOne(url).flush([]);
    expect(component.invites().length).toBe(0);
  });
});
