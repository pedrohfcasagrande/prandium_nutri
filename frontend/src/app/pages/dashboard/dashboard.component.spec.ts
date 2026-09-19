import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideTranslateService } from '@ngx-translate/core';
import { DashboardComponent } from './dashboard.component';
import { MenuPlan } from '../../models/models';

describe('DashboardComponent.todaysMenuPreview', () => {
  let component: DashboardComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync(),
        provideNativeDateAdapter(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('returns null when no active plan', () => {
    component.activePlan.set(null);
    expect(component.todaysMenuPreview()).toBeNull();
  });

  it('returns null for daily menu without day headers', () => {
    const plan: MenuPlan = {
      name: 'Dia unico', source: 'AI', content: '## Almoco\n- 150g frango\n## Lanche\n- iogurte',
      active: true
    };
    component.activePlan.set(plan);
    expect(component.todaysMenuPreview()).toBeNull();
  });

  it('extracts today section from weekly menu when day name matches', () => {
    const jsDay = new Date().getDay();
    const dayNames = ['Domingo', 'Segunda', 'Terca', 'Quarta', 'Quinta', 'Sexta', 'Sabado'];
    const today = dayNames[jsDay];
    const plan: MenuPlan = {
      name: 'Cardapio semana',
      source: 'AI',
      content: `## Dia 1 - ${today}\n### Cafe da manha\n- 2 ovos\n\n## Dia 2 - Amanha\n### Cafe da manha\n- iogurte`,
      active: true
    };
    component.activePlan.set(plan);
    const preview = component.todaysMenuPreview();
    expect(preview).not.toBeNull();
    expect(preview!.dayLabel).toContain(today);
    expect(preview!.text).toContain('2 ovos');
    expect(preview!.text).not.toContain('iogurte');
  });

  it('falls back to first day when no match', () => {
    const plan: MenuPlan = {
      name: 'X',
      source: 'AI',
      content: '## Dia 1 - Segunda\n### Cafe da manha\n- 2 ovos\n\n## Dia 2 - Terca\n### Cafe da manha\n- iogurte',
      active: true
    };
    component.activePlan.set(plan);
    const preview = component.todaysMenuPreview();
    expect(preview).not.toBeNull();
    expect(preview!.dayLabel).toBeTruthy();
  });
});
