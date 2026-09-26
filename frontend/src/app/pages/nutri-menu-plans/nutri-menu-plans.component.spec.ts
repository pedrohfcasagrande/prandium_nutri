import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { NutriMenuPlansComponent } from './nutri-menu-plans.component';
import { environment } from '../../../environments/environment';
import { MenuPlan } from '../../models/models';

describe('NutriMenuPlansComponent', () => {
  let fixture: ComponentFixture<NutriMenuPlansComponent>;
  let component: NutriMenuPlansComponent;
  let http: HttpTestingController;
  const base = environment.apiUrl;

  const mockPending = (id = 1, name = 'Cardapio Ana'): MenuPlan => ({
    id, name, source: 'MANUAL', content: '', active: false,
    status: 'PENDING', submittedAt: '2026-09-20T12:00:00Z',
    ownerName: 'Ana Silva', ownerEmail: 'ana@example.com',
    ownerUserId: 42, reviewedAt: null, reviewNote: null
  } as MenuPlan);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NutriMenuPlansComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NutriMenuPlansComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // Consome as duas requests que ngOnInit dispara (pending + clients).
  function flushInit(pending: MenuPlan[] = []) {
    http.expectOne(`${base}/nutritionist/menu-plans/pending`).flush(pending);
    http.expectOne(r => r.url === `${base}/nutritionist/clients`)
        .flush({ items: [], total: 0, page: 0, size: 200 });
  }

  it('dispara pending + clients GET no ngOnInit', () => {
    fixture.detectChanges();
    flushInit([]);
    expect(component.pending().length).toBe(0);
    expect(component.clients().length).toBe(0);
  });

  it('mostra estado vazio quando nao ha menus pendentes', () => {
    fixture.detectChanges();
    flushInit([]);
    fixture.detectChanges();

    const empty = fixture.debugElement.query(By.css('.empty'));
    expect(empty).toBeTruthy();
    const inbox = fixture.debugElement.query(By.css('mat-icon'));
    expect(inbox.nativeElement.textContent.trim()).toBe('inbox');
  });

  it('renderiza cards com nome + owner para cada menu pendente', () => {
    fixture.detectChanges();
    flushInit([mockPending(10, 'Plano A'), mockPending(11, 'Plano B')]);
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('.row-card'));
    expect(rows.length).toBe(2);

    const text = rows[0].nativeElement.textContent;
    expect(text).toContain('Plano A');
    expect(text).toContain('Ana Silva');
  });

  it('approve(p) faz POST /approve e recarrega pending', () => {
    fixture.detectChanges();
    flushInit([mockPending(50)]);
    fixture.detectChanges();

    component.approve(mockPending(50));

    const approveReq = http.expectOne(`${base}/nutritionist/menu-plans/50/approve`);
    expect(approveReq.request.method).toBe('POST');
    approveReq.flush({ ...mockPending(50), status: 'APPROVED' });

    // load() eh chamado apos approve: pending recarrega
    http.expectOne(`${base}/nutritionist/menu-plans/pending`).flush([]);
  });
});
