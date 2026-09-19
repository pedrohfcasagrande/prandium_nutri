import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { CompanyClientsComponent } from './company-clients.component';
import { environment } from '../../../environments/environment';

describe('CompanyClientsComponent', () => {
  let component: CompanyClientsComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompanyClientsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(CompanyClientsComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    // Absorve os requests iniciais
    http.expectOne(`${environment.apiUrl}/company/clients`).flush([]);
    http.expectOne(`${environment.apiUrl}/company/nutritionists`).flush([]);
  });

  afterEach(() => http.verify());

  it('activeNutris exclui nao ativos', () => {
    component.nutris.set([
      { id: 1, nutritionistId: 10, nutritionistName: 'A', nutritionistEmail: 'a@x',
        status: 'ACTIVE', invitedAt: '', acceptedAt: '', clientsCount: 0 },
      { id: 2, nutritionistId: 11, nutritionistName: 'B', nutritionistEmail: 'b@x',
        status: 'REJECTED', invitedAt: '', acceptedAt: '', clientsCount: 0 }
    ] as any);
    expect(component.activeNutris().length).toBe(1);
    expect(component.activeNutris()[0].id).toBe(10);
  });

  it('filtered filtra por query', () => {
    const seed = [
      { linkId: 1, clientId: 1, clientName: 'Joao Silva', clientEmail: 'j@x',
        nutriId: 10, nutriName: 'Ana', status: 'ACTIVE', lastMealDate: null },
      { linkId: 2, clientId: 2, clientName: 'Maria', clientEmail: 'm@x',
        nutriId: 10, nutriName: 'Ana', status: 'ACTIVE', lastMealDate: null }
    ] as any;
    component.query = 'joao';
    component.clients.set(seed);
    expect(component.filtered().length).toBe(1);
    component.query = '';
    component.clients.set([...seed]);  // dispara recomputacao
    expect(component.filtered().length).toBe(2);
  });

  it('transfer chama endpoint com target correto', () => {
    component.clients.set([
      { linkId: 1, clientId: 42, clientName: 'X', clientEmail: 'x@x',
        nutriId: 10, nutriName: 'A', status: 'ACTIVE', lastMealDate: null }
    ] as any);
    component.transfer(component.clients()[0], 99);
    const req = http.expectOne(`${environment.apiUrl}/company/clients/42/nutri`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ targetNutriId: 99 });
    req.flush(null);
    http.expectOne(`${environment.apiUrl}/company/clients`).flush([]);
    http.expectOne(`${environment.apiUrl}/company/nutritionists`).flush([]);
  });

  it('transfer no-op quando target igual ao atual', () => {
    component.transfer({ clientId: 1, nutriId: 10 } as any, 10);
    http.expectNone(`${environment.apiUrl}/company/clients/1/nutri`);
  });
});
