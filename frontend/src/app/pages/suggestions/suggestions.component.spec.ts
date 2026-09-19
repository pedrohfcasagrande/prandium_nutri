import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { SuggestionsComponent } from './suggestions.component';

describe('SuggestionsComponent', () => {
  let component: SuggestionsComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuggestionsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(SuggestionsComponent);
    component = fixture.componentInstance;
  });

  it('scope inicia em DAY', () => {
    expect(component.scope).toBe('DAY');
  });

  it('parser separa dias e refeicoes de um menu semanal', () => {
    const md = `## Dia 1 - Segunda
### Cafe da manha
- 2 ovos
- 1 fatia de pao
### Almoco
- 150g de frango

## Dia 2 - Terca
### Cafe da manha
- iogurte`;
    const sections = (component as any).parseMenu(md);
    expect(sections.length).toBeGreaterThanOrEqual(4);
    const titles = sections.map((s: any) => s.title);
    expect(titles).toContain('Dia 1 - Segunda');
    expect(titles).toContain('Dia 2 - Terca');
    const cafe = sections.find((s: any) => s.title === 'Cafe da manha');
    expect(cafe.lines).toContain('2 ovos');
  });

  it('parser tolera menu sem headers e agrega em uma unica secao', () => {
    const md = `- 1 banana\n- 2 castanhas`;
    const sections = (component as any).parseMenu(md);
    expect(sections.length).toBe(1);
    expect(sections[0].lines).toEqual(['1 banana', '2 castanhas']);
  });

  it('groupByDay agrupa refeicoes sob cada dia (H2)', () => {
    const md = `## Dia 1 - Segunda
### Cafe da manha
- 2 ovos
### Almoco
- 150g frango

## Dia 2 - Terca
### Cafe da manha
- iogurte`;
    const flat = (component as any).parseMenu(md);
    const days = (component as any).groupByDay(flat);
    expect(days.length).toBe(2);
    expect(days[0].title).toBe('Dia 1 - Segunda');
    expect(days[0].meals.length).toBe(2);
    expect(days[0].meals[0].title).toBe('Cafe da manha');
    expect(days[1].title).toBe('Dia 2 - Terca');
    expect(days[1].meals[0].lines).toContain('iogurte');
  });

  it('groupByDay detecta dia mesmo quando header vem em bold markdown', () => {
    const md = `## **Dia 1 - Segunda**
### Cafe da manha
- 2 ovos

## **Dia 2 - Terca**
### Cafe da manha
- iogurte`;
    const flat = (component as any).parseMenu(md);
    const days = (component as any).groupByDay(flat);
    expect(days.length).toBe(2);
    expect(days[0].title).toBe('Dia 1 - Segunda');
    expect(days[1].title).toBe('Dia 2 - Terca');
  });

  it('groupByDay aceita "Dia N" mesmo em H3 (permissivo)', () => {
    const md = `### Dia 1\n- 2 ovos\n### Dia 2\n- iogurte`;
    const flat = (component as any).parseMenu(md);
    const days = (component as any).groupByDay(flat);
    expect(days.length).toBe(2);
  });

  it('groupByDay cai em unico dia sem titulo quando nao ha marcador de dia', () => {
    const md = `## Almoco\n- 150g frango\n## Lanche\n- 1 iogurte`;
    const flat = (component as any).parseMenu(md);
    const days = (component as any).groupByDay(flat);
    expect(days.length).toBe(1);
    expect(days[0].title).toBe('');
    expect(days[0].meals.length).toBe(2);
  });

  it('sectionIcon retorna icone conforme titulo', () => {
    expect(component.sectionIcon('Almoco', 0)).toBe('lunch_dining');
    expect(component.sectionIcon('Cafe da manha', 0)).toBe('free_breakfast');
    expect(component.sectionIcon('Jantar', 0)).toBe('dinner_dining');
    expect(component.sectionIcon('Titulo qualquer', 2)).toMatch(/dining|restaurant/);
  });
});
