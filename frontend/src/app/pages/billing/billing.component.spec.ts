import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { BillingComponent } from './billing.component';
import { environment } from '../../../environments/environment';
import { Subscription } from '../../models/models';

describe('BillingComponent', () => {
  let fixture: ComponentFixture<BillingComponent>;
  let component: BillingComponent;
  let http: HttpTestingController;
  const base = environment.apiUrl;

  const mockSub = (overrides: Partial<Subscription> = {}): Subscription => ({
    id: 1,
    payerType: 'NUTRITIONIST',
    payerId: 10,
    planCode: 'NUTRI_PRO',
    planName: 'Pro',
    planTier: 'PRO',
    features: { maxClients: 25, aiSuggestionsMonthly: 100, menuLimit: null },
    currency: 'BRL',
    amountCents: 7900,
    provider: 'STRIPE',
    status: 'ACTIVE',
    currentPeriodStart: '2026-09-01T00:00:00Z',
    currentPeriodEnd: '2026-10-01T00:00:00Z',
    cancelAtPeriodEnd: false,
    currentMonthUsage: { maxClients: 12, aiSuggestionsMonthly: 45 },
    ...overrides
  } as Subscription);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BillingComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('mostra estado vazio quando API retorna null', () => {
    fixture.detectChanges();
    http.expectOne(`${base}/subscription/me`).flush(null);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.empty'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.plan-card'))).toBeNull();
  });

  it('renderiza plan card com nome, preco e tier class quando API retorna assinatura', () => {
    fixture.detectChanges();
    http.expectOne(`${base}/subscription/me`).flush(mockSub());
    fixture.detectChanges();

    const planCard = fixture.debugElement.query(By.css('.plan-card'));
    expect(planCard).toBeTruthy();

    const hero = fixture.debugElement.query(By.css('.plan-hero'));
    expect(hero.nativeElement.classList.contains('tier-pro')).toBe(true);

    const name = fixture.debugElement.query(By.css('.plan-name'));
    expect(name.nativeElement.textContent.trim()).toBe('Pro');

    const amount = fixture.debugElement.query(By.css('.plan-price .amount'));
    expect(amount.nativeElement.textContent).toContain('79');
  });

  it('renderiza uma linha de feature por chave de features', () => {
    fixture.detectChanges();
    http.expectOne(`${base}/subscription/me`).flush(mockSub());
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('.limit-row'));
    expect(rows.length).toBe(3);
  });

  it('cancel() chama endpoint de cancelamento e recarrega assinatura', () => {
    jest.spyOn(window, 'confirm').mockReturnValue(true);
    jest.useFakeTimers();

    fixture.detectChanges();
    http.expectOne(`${base}/subscription/me`).flush(mockSub());
    fixture.detectChanges();

    component.cancel();

    const cancelReq = http.expectOne(r =>
      r.url === `${base}/checkout/cancel` && r.params.get('atPeriodEnd') === 'true');
    expect(cancelReq.request.method).toBe('POST');
    cancelReq.flush({ message: 'ok' });

    // Depois de 1500ms o reload() dispara mySubscription() novamente
    jest.advanceTimersByTime(1600);
    http.expectOne(`${base}/subscription/me`).flush(mockSub());

    jest.useRealTimers();
  });

  it('nao mostra botao cancelar quando provider e MANUAL (plano free)', () => {
    fixture.detectChanges();
    http.expectOne(`${base}/subscription/me`).flush(
      mockSub({ provider: 'MANUAL', amountCents: 0, planTier: 'FREE' })
    );
    fixture.detectChanges();

    const buttons = fixture.debugElement.queryAll(By.css('mat-card-actions button'));
    const cancelBtn = buttons.find(b => b.nativeElement.textContent.includes('cancel'));
    expect(cancelBtn).toBeUndefined();
  });
});
