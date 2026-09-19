import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiService } from './api.service';
import { environment } from '../../environments/environment';

describe('ApiService', () => {
  let service: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('suggest() envia scope=DAY como default', () => {
    service.suggest('notinha').subscribe();
    const req = http.expectOne(`${environment.apiUrl}/suggestions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ extraNotes: 'notinha', scope: 'DAY' });
    req.flush({ menu: '## Almoco' });
  });

  it('suggest() envia scope=WEEK quando explicito', () => {
    service.suggest('sem notas', 'WEEK').subscribe();
    const req = http.expectOne(`${environment.apiUrl}/suggestions`);
    expect(req.request.body).toEqual({ extraNotes: 'sem notas', scope: 'WEEK' });
    req.flush({ menu: '## Dia 1' });
  });

  it('adminMetricsOverview() bate no endpoint correto', () => {
    service.adminMetricsOverview().subscribe();
    const req = http.expectOne(`${environment.apiUrl}/admin/metrics/overview`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('pricing() serializa currency+target como query params', () => {
    service.pricing('BRL', 'NUTRITIONIST').subscribe();
    const req = http.expectOne(r => r.url === `${environment.apiUrl}/pricing`);
    expect(req.request.params.get('currency')).toBe('BRL');
    expect(req.request.params.get('target')).toBe('NUTRITIONIST');
    req.flush({ currency: 'BRL', plans: [] });
  });
});
