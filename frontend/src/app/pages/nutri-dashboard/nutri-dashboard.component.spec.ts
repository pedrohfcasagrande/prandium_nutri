import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { NutriDashboardComponent } from './nutri-dashboard.component';
import { environment } from '../../../environments/environment';

describe('NutriDashboardComponent', () => {
  let component: NutriDashboardComponent;
  let http: HttpTestingController;

  const base = environment.apiUrl;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NutriDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' }),
        { provide: AuthService, useValue: {
          currentUser: () => ({ id: 1, name: 'Dr. Bruno Almeida', role: 'NUTRITIONIST' })
        } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(NutriDashboardComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('carrega metrics + pending menus + client requests + subscription em paralelo', () => {
    http.expectOne(r => r.url === `${base}/nutritionist/metrics/overview`).flush({
      activeClients: 5, aiSuggestionsMonth: 3, aiSuggestionsLimit: 10,
      menusActive: 4, avgAdherence7d: 0.65, clientsNeedingAttention: 1, clients: []
    });
    http.expectOne(`${base}/nutritionist/menu-plans/pending`).flush([
      { id: 1, name: 'M1', status: 'PENDING' } as any
    ]);
    http.expectOne(`${base}/nutritionist/client-requests`).flush([]);
    http.expectOne(`${base}/subscription/me`).flush({
      id: 1, planCode: 'NUTRI_FREE', planName: 'Grátis', status: 'ACTIVE',
      currentPeriodEnd: '2026-10-19T00:00:00'
    } as any);

    expect(component.loading()).toBe(false);
    expect(component.metrics()?.activeClients).toBe(5);
    expect(component.pendingMenus().length).toBe(1);
    expect(component.subscription()?.status).toBe('ACTIVE');
    expect(component.needsAttention()).toBe(1);
  });

  it('firstName preserva honorifico + primeiro nome', () => {
    http.expectOne(r => r.url === `${base}/nutritionist/metrics/overview`).flush({} as any);
    http.expectOne(`${base}/nutritionist/menu-plans/pending`).flush([]);
    http.expectOne(`${base}/nutritionist/client-requests`).flush([]);
    http.expectOne(`${base}/subscription/me`).flush(null);
    // Mock injetado retorna "Dr. Bruno Almeida"
    expect(component.firstName()).toBe('Dr. Bruno');
  });

  it('marca loading=false mesmo quando algum endpoint falha', () => {
    http.expectOne(r => r.url === `${base}/nutritionist/metrics/overview`).error(new ProgressEvent('err'));
    // As demais requests podem ou nao ser feitas dependendo do forkJoin — deixamos o verify pegar
    try { http.expectOne(`${base}/nutritionist/menu-plans/pending`).flush([]); } catch {}
    try { http.expectOne(`${base}/nutritionist/client-requests`).flush([]); } catch {}
    try { http.expectOne(`${base}/subscription/me`).flush(null); } catch {}
    expect(component.loading()).toBe(false);
  });
});
