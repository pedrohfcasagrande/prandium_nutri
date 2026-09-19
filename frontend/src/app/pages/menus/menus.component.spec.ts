import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MenusComponent } from './menus.component';
import { MenuPlan } from '../../models/models';

describe('MenusComponent', () => {
  let component: MenusComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MenusComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync(),
        provideTranslateService({ defaultLanguage: 'pt' })
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(MenusComponent);
    component = fixture.componentInstance;
  });

  it('activePlan computed retorna o plan com active=true', () => {
    component.plans.set([
      { id: 1, name: 'A', source: 'MANUAL', content: 'x', active: false },
      { id: 2, name: 'B', source: 'AI', content: 'y', active: true }
    ]);
    expect(component.activePlan()?.id).toBe(2);
    expect(component.otherPlans().length).toBe(1);
    expect(component.otherPlans()[0].id).toBe(1);
  });

  it('activePlan null quando nenhum ativo', () => {
    component.plans.set([{ id: 1, name: 'A', source: 'MANUAL', content: 'x', active: false }]);
    expect(component.activePlan()).toBeNull();
    expect(component.otherPlans().length).toBe(1);
  });

  it('heroPreview retorna as primeiras 6 linhas nao-vazias', () => {
    const content = ['## Titulo', '- item 1', '- item 2', '', '- item 3', '', '- item 4', '- item 5', '- item 6', '- item 7'].join('\n');
    const preview = component.heroPreview(content);
    expect(preview.split('\n').length).toBe(6);
    expect(preview).not.toContain('item 7');
  });

  it('isTruncated true quando > 6 linhas', () => {
    const long = Array.from({ length: 10 }, (_, i) => `- item ${i}`).join('\n');
    const short = '- a\n- b\n- c';
    expect(component.isTruncated(long)).toBe(true);
    expect(component.isTruncated(short)).toBe(false);
  });
});
