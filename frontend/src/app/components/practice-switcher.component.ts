import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { PracticeService } from '../services/practice.service';

@Component({
  selector: 'app-practice-switcher',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatMenuModule, MatIconModule, MatTooltipModule, TranslateModule],
  template: `
    @if (practices().length >= 2) {
      <button mat-button [matMenuTriggerFor]="menu" class="switcher"
              [matTooltip]="'practiceSwitcher.tooltip' | translate">
        <mat-icon>{{ activeIcon() }}</mat-icon>
        <span class="label">{{ activeLabel() }}</span>
        <mat-icon class="chevron">expand_more</mat-icon>
      </button>
      <mat-menu #menu="matMenu">
        @for (p of practices(); track p.id) {
          <button mat-menu-item (click)="select(p.id)" [disabled]="p.status === 'SUSPENDED'">
            <mat-icon>
              {{ p.id === active() ? 'check' : (p.companyName ? 'corporate_fare' : 'person') }}
            </mat-icon>
            <span>{{ p.companyName ?? ('practiceSwitcher.private' | translate) }}</span>
          </button>
        }
      </mat-menu>
    }
  `,
  styles: [`
    .switcher {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 0 10px !important;
      background: var(--nt-surface-2); border-radius: 999px;
      mat-icon:not(.chevron) { color: var(--nt-primary); }
      .chevron { font-size: 18px; width: 18px; height: 18px; opacity: 0.6; }
    }
    .label { font-size: 0.85rem; font-weight: 600; max-width: 160px; overflow: hidden;
             text-overflow: ellipsis; white-space: nowrap; }
  `]
})
export class PracticeSwitcherComponent {
  private svc = inject(PracticeService);

  practices = this.svc.practices;
  active = this.svc.activePracticeId;

  activeIcon = computed(() => {
    const p = this.practices().find(x => x.id === this.active());
    return p?.companyName ? 'corporate_fare' : 'person';
  });

  activeLabel = computed(() => {
    const p = this.practices().find(x => x.id === this.active());
    if (!p) return '';
    return p.companyName ?? 'Particular';
  });

  select(id: number) { this.svc.setActive(id); }
}
