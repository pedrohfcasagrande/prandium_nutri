import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { NutriPracticesComponent } from './nutri-practices.component';
import { environment } from '../../../environments/environment';

describe('NutriPracticesComponent', () => {
  let component: NutriPracticesComponent;
  let http: HttpTestingController;
  const base = environment.apiUrl;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NutriPracticesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(NutriPracticesComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne(`${base}/nutritionist/practices`).flush([
      { id: 1, companyId: null, companyName: null, bio: 'p', specialties: 'a',
        acceptsNewClients: true, status: 'ACTIVE' },
      { id: 2, companyId: 10, companyName: 'NutriPro', bio: null, specialties: null,
        acceptsNewClients: false, status: 'ACTIVE' }
    ]);
    // Fase 9D: componente tambem le assinatura no init.
    http.expectOne(`${base}/nutritionist/me/signature`).flush({ signatureBase64: '' });
    // Fase 9D-2-fix seguimento: componente pré-carrega o template PDF de cada practice
    // no init pra evitar bug de "painel abre vazio" quando o evento (opened) nao dispara.
    http.expectOne(`${base}/nutritionist/practices/1/pdf-template`).flush({
      pdfLogoBase64: null, pdfPrimaryColor: null, pdfHeaderText: null, pdfFooterText: null
    });
    http.expectOne(`${base}/nutritionist/practices/2/pdf-template`).flush({
      pdfLogoBase64: null, pdfPrimaryColor: null, pdfHeaderText: null, pdfFooterText: null
    });
  });

  afterEach(() => http.verify());

  it('carrega practices do nutri', () => {
    expect(component.practices().length).toBe(2);
    expect(component.practices()[0].companyName).toBeNull();
    expect(component.practices()[1].companyName).toBe('NutriPro');
  });

  it('save envia PUT com os campos editaveis', () => {
    const p = component.practices()[0];
    p.bio = 'nova bio';
    p.acceptsNewClients = false;
    component.save(p);
    const req = http.expectOne(`${base}/nutritionist/practices/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      bio: 'nova bio', specialties: 'a', acceptsNewClients: false
    });
    req.flush(p);
  });
});
