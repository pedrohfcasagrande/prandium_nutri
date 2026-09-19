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

  const emptyPage = { items: [], total: 0, page: 0, size: 10 };
  const clientsUrl = `${environment.apiUrl}/company/clients`;
  const nutrisUrl = `${environment.apiUrl}/company/nutritionists`;

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

    http.expectOne(r => r.url === clientsUrl).flush(emptyPage);
    http.expectOne(r => r.url === nutrisUrl).flush(emptyPage);
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

  it('onSearch dispara reload com q e reset page', () => {
    component.pageIndex = 5;
    component.onSearch('joao');
    expect(component.pageIndex).toBe(0);
    const req = http.expectOne(r => r.url === clientsUrl && r.params.get('q') === 'joao');
    req.flush(emptyPage);
    http.expectOne(r => r.url === nutrisUrl).flush(emptyPage);
  });

  it('onSort dispara reload com sort', () => {
    component.onSort('name');
    const req = http.expectOne(r => r.url === clientsUrl && r.params.get('sort') === 'name');
    req.flush(emptyPage);
    http.expectOne(r => r.url === nutrisUrl).flush(emptyPage);
  });

  it('onPage dispara reload mantendo pageIndex', () => {
    component.onPage({ pageIndex: 2, pageSize: 25, length: 100 } as any);
    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(25);
    const req = http.expectOne(r => r.url === clientsUrl
                                     && r.params.get('page') === '2'
                                     && r.params.get('size') === '25');
    req.flush(emptyPage);
    http.expectOne(r => r.url === nutrisUrl).flush(emptyPage);
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
    http.expectOne(r => r.url === clientsUrl).flush(emptyPage);
    http.expectOne(r => r.url === nutrisUrl).flush(emptyPage);
  });

  it('transfer no-op quando target igual ao atual', () => {
    component.transfer({ clientId: 1, nutriId: 10 } as any, 10);
    http.expectNone(`${environment.apiUrl}/company/clients/1/nutri`);
  });
});
