import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Skeleton loader com shimmer. Types: bar (default), circle, kpi, card. */
@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="sk" [class]="'sk-' + type"
         [style.width]="width" [style.height]="height"
         [style.border-radius]="radius"></div>
  `,
  styles: [`
    .sk {
      display: block;
      background: linear-gradient(90deg,
        var(--nt-surface-2) 0%,
        var(--nt-border) 50%,
        var(--nt-surface-2) 100%);
      background-size: 200% 100%;
      animation: nt-shimmer 1.4s ease-in-out infinite;
      border-radius: 8px;
    }
    .sk-bar { height: 14px; }
    .sk-circle { border-radius: 50%; }
    .sk-kpi { height: 96px; border-radius: var(--nt-radius); }
    .sk-card { height: 120px; border-radius: var(--nt-radius); }
  `]
})
export class SkeletonComponent {
  @Input() type: 'bar' | 'circle' | 'kpi' | 'card' = 'bar';
  @Input() width = '100%';
  @Input() height?: string;
  @Input() radius?: string;
}
