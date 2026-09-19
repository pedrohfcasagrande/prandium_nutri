import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { AlertBannerComponent } from './alert-banner.component';
import { environment } from '../../environments/environment';

describe('AlertBannerComponent', () => {
  let component: AlertBannerComponent;
  let fixture: ComponentFixture<AlertBannerComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertBannerComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AlertBannerComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    component.scope = 'admin';
  });

  afterEach(() => http.verify());

  it('carrega alerts do endpoint correto na init', () => {
    fixture.detectChanges(); // triggers ngOnInit
    const req = http.expectOne(`${environment.apiUrl}/alerts/admin`);
    expect(req.request.method).toBe('GET');
    req.flush([
      { id: 1, scope: 'ADMIN', scopeId: null, severity: 'WARN', kind: 'X',
        title: 't1', message: 'm1', metadata: null, createdAt: '2026-01-01T00:00:00', dismissedAt: null }
    ]);
    expect(component.alerts().length).toBe(1);
  });

  it('dismiss remove da lista e envia POST', () => {
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/alerts/admin`).flush([
      { id: 42, scope: 'ADMIN', scopeId: null, severity: 'INFO', kind: 'X',
        title: 't', message: null, metadata: null, createdAt: '2026-01-01T00:00:00', dismissedAt: null }
    ]);

    component.dismiss(component.alerts()[0]);
    const req = http.expectOne(`${environment.apiUrl}/alerts/42/dismiss`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
    expect(component.alerts().length).toBe(0);
  });

  it('icon retorna icone por severidade', () => {
    expect(component.icon('CRITICAL')).toBe('error');
    expect(component.icon('WARN')).toBe('warning');
    expect(component.icon('INFO')).toBe('info');
    expect(component.icon('X')).toBe('info');
  });
});
