import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { BaseChartDirective } from 'ng2-charts';
import { AdminMetricsComponent } from './admin-metrics.component';
import { environment } from '../../../environments/environment';
import { AdminMetricsOverview } from '../../models/models';

describe('AdminMetricsComponent', () => {
  let fixture: ComponentFixture<AdminMetricsComponent>;
  let component: AdminMetricsComponent;
  let http: HttpTestingController;
  const base = environment.apiUrl;

  const mockOverview = (overrides: Partial<AdminMetricsOverview> = {}): AdminMetricsOverview => ({
    totalUsers: 100, totalNutritionists: 12, totalCompanies: 4,
    activeSubscriptions: 33,
    dau: 10, wau: 25, mau: 60,
    newUsers30d: 18,
    mrrBrlCents: 250000,
    mrrByCurrency: { BRL: 250000, USD: 4900 },
    planDistribution: { NUTRI_PRO: 8, CLIENT_PLUS: 15 },
    suggestionsLast30d: [{ action: 'LOGIN', count: 120 }],
    topCompanies: [
      { companyId: 1, name: 'NutriPro Saude', amountCents: 49900, currency: 'BRL' }
    ],
    topNutris: [
      { nutriId: 1, name: 'Dra. Camila', activeClients: 8 }
    ],
    ...overrides
  } as AdminMetricsOverview);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminMetricsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    })
      // Remove ng2-charts directive — jsdom nao tem canvas context, entao chart.js
      // exploditaria. NO_ERRORS_SCHEMA deixa o <canvas baseChart> renderizar vazio.
      .overrideComponent(AdminMetricsComponent, {
        remove: { imports: [BaseChartDirective] },
        add: { schemas: [NO_ERRORS_SCHEMA] }
      })
      .compileComponents();

    fixture = TestBed.createComponent(AdminMetricsComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('mostra skeletons enquanto carrega', () => {
    fixture.detectChanges();

    // Alert banner ainda pendura sua request; responde vazio pra nao poluir
    http.expectOne(`${base}/alerts/admin`).flush([]);

    const skeletons = fixture.debugElement.queryAll(By.css('app-skeleton'));
    expect(skeletons.length).toBeGreaterThan(0);

    http.expectOne(`${base}/admin/metrics/overview`).flush(mockOverview());
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
  });

  it('renderiza cards KPI apos API retornar overview', () => {
    fixture.detectChanges();
    http.expectOne(`${base}/alerts/admin`).flush([]);
    http.expectOne(`${base}/admin/metrics/overview`).flush(mockOverview());
    fixture.detectChanges();

    const kpiCards = fixture.debugElement.queryAll(By.css('.kpi'));
    // Admin overview tem 6 KPIs (MRR, subs, MAU, novos users, nutris, empresas)
    expect(kpiCards.length).toBe(6);

    // MRR: 250000 cents = R$ 2500 ⇒ formato numero
    const values = fixture.debugElement.queryAll(By.css('.kpi-value'))
      .map(v => v.nativeElement.textContent.trim());
    // O locale padrao do teste (en-US) formata como "2,500.00" — aceitamos qualquer
    // separador de milhar (ponto/virgula/espaco) desde que os digitos apareçam
    expect(values.some(t => /2[.,\s]?500/.test(t))).toBe(true);
    expect(values.some(t => t === '33')).toBe(true); // activeSubscriptions
  });

  it('renderiza tabelas de top companies e top nutris', () => {
    fixture.detectChanges();
    http.expectOne(`${base}/alerts/admin`).flush([]);
    http.expectOne(`${base}/admin/metrics/overview`).flush(mockOverview());
    fixture.detectChanges();

    const bodyText = fixture.nativeElement.textContent;
    expect(bodyText).toContain('NutriPro Saude');
    expect(bodyText).toContain('Dra. Camila');
  });

  it('sai do estado de loading mesmo se a API falhar', () => {
    fixture.detectChanges();
    http.expectOne(`${base}/alerts/admin`).flush([]);
    http.expectOne(`${base}/admin/metrics/overview`).error(
      new ProgressEvent('error'), { status: 500, statusText: 'Server Error' }
    );
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.overview()).toBeNull();
  });
});
