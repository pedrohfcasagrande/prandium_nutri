import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { PricingComponent } from './pricing.component';
import { environment } from '../../../environments/environment';

describe('PricingComponent', () => {
  let component: PricingComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PricingComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(PricingComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('onTargetChange dispara nova requisicao pricing', () => {
    component.target = 'USER';
    component.currency = 'BRL';
    component.onTargetChange('NUTRITIONIST');

    const req = http.expectOne(r =>
      r.url === `${environment.apiUrl}/pricing`
      && r.params.get('target') === 'NUTRITIONIST');
    req.flush({ currency: 'BRL', plans: [] });
    expect(component.target).toBe('NUTRITIONIST');
  });

  it('onTargetChange no-op quando valor igual ao atual', () => {
    component.target = 'NUTRITIONIST';
    component.onTargetChange('NUTRITIONIST');
    http.expectNone(`${environment.apiUrl}/pricing`);
  });

  it('reload descarta respostas obsoletas (guard reqSeq)', () => {
    component.target = 'USER';
    component.currency = 'BRL';
    component.reload();
    const req1 = http.expectOne(`${environment.apiUrl}/pricing?currency=BRL&target=USER`);

    component.reload();
    const req2 = http.expectOne(`${environment.apiUrl}/pricing?currency=BRL&target=USER`);

    req1.flush({ currency: 'BRL', plans: [{ code: 'OLD', target: 'USER', tier: 'FREE',
                    name: 'Old', features: {}, currency: 'BRL', amountCents: 0 }] });
    expect(component.pricing()).toBeNull();

    req2.flush({ currency: 'BRL', plans: [{ code: 'NEW', target: 'USER', tier: 'FREE',
                    name: 'New', features: {}, currency: 'BRL', amountCents: 0 }] });
    expect(component.pricing()?.plans[0].code).toBe('NEW');
  });
});
