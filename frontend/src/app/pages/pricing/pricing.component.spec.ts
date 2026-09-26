import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { PricingComponent } from './pricing.component';
import { environment } from '../../../environments/environment';

describe('PricingComponent', () => {
  let fixture: ComponentFixture<PricingComponent>;
  let component: PricingComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PricingComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PricingComponent);
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

  // Helper: consome as chamadas que ngOnInit dispara (detectCurrency + pricing inicial),
  // depois seta os planos manualmente para o teste de renderizacao.
  function initWithPlans(plans: any[]) {
    fixture.detectChanges(); // dispara ngOnInit
    http.expectOne(`${environment.apiUrl}/pricing/detect-currency`).flush({ currency: 'BRL' });
    http.expectOne(r => r.url === `${environment.apiUrl}/pricing`)
        .flush({ currency: 'BRL', plans });
    fixture.detectChanges();
  }

  it('renderiza um .plan-card por plano recebido', () => {
    initWithPlans([
      { code: 'CLIENT_FREE', target: 'USER', tier: 'FREE',
        name: 'Free', features: {}, currency: 'BRL', amountCents: 0 },
      { code: 'CLIENT_PLUS', target: 'USER', tier: 'PLUS',
        name: 'Plus', features: {}, currency: 'BRL', amountCents: 1900 }
    ]);

    const cards = fixture.debugElement.queryAll(By.css('.plan-card'));
    expect(cards.length).toBe(2);

    const names = cards.map(c => c.query(By.css('.plan-name')).nativeElement.textContent.trim());
    expect(names).toEqual(['Free', 'Plus']);
  });

  it('plano free mostra label "billing.free" e plano pago mostra amount', () => {
    initWithPlans([
      { code: 'CLIENT_FREE', target: 'USER', tier: 'FREE',
        name: 'Free', features: {}, currency: 'BRL', amountCents: 0 },
      { code: 'CLIENT_PLUS', target: 'USER', tier: 'PLUS',
        name: 'Plus', features: {}, currency: 'BRL', amountCents: 1900 }
    ]);

    const cards = fixture.debugElement.queryAll(By.css('.plan-card'));
    const freeCard = cards[0].nativeElement.textContent;
    expect(freeCard).toContain('billing.free'); // key literal (sem translation loader real)

    const paidCard = cards[1].nativeElement.textContent;
    expect(paidCard).toMatch(/19[.,]00/); // formato pt-BR: R$ 19,00
  });
});
