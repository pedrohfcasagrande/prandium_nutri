import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Sparkline SVG leve — se receber `data` (array de numeros) renderiza a trend
 * real; senao gera uma trend sintetica deterministica a partir de `seed`.
 * Uso como polish visual pra KPI cards enquanto nao ha serie historica real.
 */
@Component({
  selector: 'app-sparkline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg [attr.viewBox]="'0 0 100 30'" preserveAspectRatio="none" class="sparkline">
      <defs>
        <linearGradient [attr.id]="gradId()" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" [attr.stop-color]="color" stop-opacity="0.35"/>
          <stop offset="100%" [attr.stop-color]="color" stop-opacity="0"/>
        </linearGradient>
      </defs>
      <path [attr.d]="areaPath()" [attr.fill]="'url(#' + gradId() + ')'"/>
      <path [attr.d]="linePath()" fill="none" [attr.stroke]="color"
            stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  `,
  styles: [`
    .sparkline { display: block; width: 100%; height: 100%; }
  `]
})
export class SparklineComponent {
  private static nextId = 0;
  private id = ++SparklineComponent.nextId;

  @Input() color = 'currentColor';
  @Input() data?: number[];
  @Input() seed = 1;

  gradId = signal(`spark-${this.id}`);

  private points = computed(() => {
    const raw = this.data && this.data.length >= 2 ? this.data : this.syntheticSeries(this.seed);
    const min = Math.min(...raw);
    const max = Math.max(...raw);
    const range = max - min || 1;
    return raw.map((v, i) => {
      const x = (i / (raw.length - 1)) * 100;
      const y = 30 - ((v - min) / range) * 26 - 2; // 2px padding top/bottom
      return { x, y };
    });
  });

  linePath = computed(() => {
    const pts = this.points();
    if (pts.length < 2) return '';
    return pts.reduce((acc, p, i) =>
      acc + (i === 0 ? `M${p.x},${p.y}` : ` L${p.x},${p.y}`), '');
  });

  areaPath = computed(() => {
    const pts = this.points();
    if (pts.length < 2) return '';
    return pts.reduce((acc, p, i) =>
      acc + (i === 0 ? `M${p.x},30 L${p.x},${p.y}` : ` L${p.x},${p.y}`),
      '') + ` L${pts[pts.length - 1].x},30 Z`;
  });

  /** Serie sintetica deterministica (12 pontos) baseada em seed — LCG simples. */
  private syntheticSeries(seed: number): number[] {
    const out: number[] = [];
    let s = (seed * 9301 + 49297) % 233280;
    for (let i = 0; i < 12; i++) {
      s = (s * 9301 + 49297) % 233280;
      const noise = (s / 233280 - 0.5) * 0.6;
      const trend = i * 0.12 * (seed % 2 === 0 ? 1 : 0.7);
      out.push(2 + trend + noise);
    }
    return out;
  }
}
