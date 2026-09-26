import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideTranslateService } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { MealsComponent } from './meals.component';
import { environment } from '../../../environments/environment';

describe('MealsComponent (AI food fallback)', () => {
  let fixture: ComponentFixture<MealsComponent>;
  let component: MealsComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MealsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        provideNativeDateAdapter(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MealsComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    // absorve o listMeals inicial
    http.expectOne(req => req.url === `${environment.apiUrl}/meals`).flush([]);
  });

  afterEach(() => http.verify());

  it('dispara lookup IA quando searchFoods retorna vazio', fakeAsync(() => {
    component.foodQuery = 'coca';
    component.searchFoods();
    http.expectOne(req => req.url === `${environment.apiUrl}/foods` && req.params.get('q') === 'coca')
        .flush([]);

    tick(500);

    http.expectOne(req => req.url === `${environment.apiUrl}/foods/ai-lookup` && req.params.get('q') === 'coca')
        .flush([{
          id: null, name: 'Coca-Cola', category: 'Bebidas',
          caloriesPer100g: 42, proteinPer100g: 0, carbsPer100g: 10.6, fatPer100g: 0,
          aiSuggested: true
        }]);

    expect(component.foodOptions().length).toBe(1);
    expect(component.foodOptions()[0].aiSuggested).toBe(true);
  }));

  it('nao dispara lookup IA quando busca local retorna resultados', fakeAsync(() => {
    component.foodQuery = 'arroz';
    component.searchFoods();
    http.expectOne(req => req.url === `${environment.apiUrl}/foods`)
        .flush([{ id: 1, name: 'Arroz branco', caloriesPer100g: 128,
                  proteinPer100g: 2.5, carbsPer100g: 28.1, fatPer100g: 0.2 }]);

    tick(1000);
    http.expectNone(`${environment.apiUrl}/foods/ai-lookup`);
    expect(component.foodOptions().length).toBe(1);
  }));

  it('addItem persiste alimento IA antes de adicionar ao meal', () => {
    component.pendingFood = {
      id: null, name: 'Suco Del Valle', caloriesPer100g: 45,
      proteinPer100g: 0.5, carbsPer100g: 11, fatPer100g: 0, aiSuggested: true
    } as any;
    component.pendingGrams = 200;
    component.addItem();

    http.expectOne(req => req.url === `${environment.apiUrl}/foods` && req.method === 'POST')
        .flush({ id: 99, name: 'Suco Del Valle', caloriesPer100g: 45,
                 proteinPer100g: 0.5, carbsPer100g: 11, fatPer100g: 0 });

    expect(component.draft.items).toHaveLength(1);
    expect(component.draft.items[0].foodId).toBe(99);
    expect(component.pendingFood).toBeNull();
  });

  it('mostra estado vazio quando nao ha meals para a data', () => {
    // beforeEach ja flushou [] para /meals — apenas confirma que renderizou empty state
    fixture.detectChanges();
    const empty = fixture.debugElement.query(By.css('.empty'));
    expect(empty).toBeTruthy();
    expect(fixture.debugElement.queryAll(By.css('.meal-card')).length).toBe(0);
  });

  it('renderiza um .meal-card por refeicao retornada', () => {
    component.meals.set([
      { id: 1, type: 'BREAKFAST', time: '08:00', date: '2026-09-25',
        items: [], totalCalories: 350, totalProtein: 15, totalCarbs: 45, totalFat: 8 },
      { id: 2, type: 'LUNCH', time: '12:30', date: '2026-09-25',
        items: [], totalCalories: 620, totalProtein: 35, totalCarbs: 70, totalFat: 20 }
    ] as any);
    fixture.detectChanges();

    const cards = fixture.debugElement.queryAll(By.css('.meal-card'));
    expect(cards.length).toBe(2);
    expect(cards[0].nativeElement.classList.contains('type-breakfast')).toBe(true);
    expect(cards[1].nativeElement.classList.contains('type-lunch')).toBe(true);

    // Totais aparecem
    const text = cards[0].nativeElement.textContent;
    expect(text).toContain('350');
  });
});
