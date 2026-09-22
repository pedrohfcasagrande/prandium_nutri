import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Empty state polido com ilustracao SVG inline. Usa currentColor pra adaptar
 * ao tema (dark/light). Aceita title, description e projeta content pro slot
 * de acao (botao, link, etc).
 *
 * Uso:
 *  <app-empty-state type="clients" title="Nenhum cliente ainda"
 *                   description="Convide seu primeiro cliente pra comecar">
 *    <button mat-flat-button color="primary">Convidar</button>
 *  </app-empty-state>
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="empty-shell">
      <div class="empty-illust">
        @switch (type) {
          @case ('clients') {
            <svg viewBox="0 0 220 140" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="60" cy="60" r="24" stroke="currentColor" stroke-width="2.5" opacity="0.6"/>
              <circle cx="120" cy="52" r="30" stroke="currentColor" stroke-width="2.5"/>
              <circle cx="175" cy="65" r="22" stroke="currentColor" stroke-width="2.5" opacity="0.5"/>
              <path d="M25 122c8-18 24-28 45-28h60c21 0 37 10 45 28" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
              <path d="M108 42v10m-5-5h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          }
          @case ('appointments') {
            <svg viewBox="0 0 220 140" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="40" y="30" width="140" height="100" rx="10" stroke="currentColor" stroke-width="2.5"/>
              <path d="M40 55h140" stroke="currentColor" stroke-width="2.5"/>
              <path d="M70 20v20M150 20v20" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
              <circle cx="75" cy="80" r="4" fill="currentColor" opacity="0.6"/>
              <circle cx="110" cy="80" r="4" fill="currentColor" opacity="0.4"/>
              <circle cx="145" cy="80" r="4" fill="currentColor" opacity="0.2"/>
              <circle cx="75" cy="105" r="4" fill="currentColor" opacity="0.3"/>
              <circle cx="110" cy="105" r="4" fill="currentColor" opacity="0.2"/>
              <path d="M132 100l6 6 12-12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          }
          @case ('menus') {
            <svg viewBox="0 0 220 140" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="55" y="15" width="110" height="120" rx="8" stroke="currentColor" stroke-width="2.5"/>
              <path d="M75 40h70M75 55h50M75 70h60M75 85h40M75 100h55" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.7"/>
              <circle cx="170" cy="100" r="20" fill="currentColor" opacity="0.15"/>
              <path d="M162 100l6 6 12-12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          }
          @case ('mail') {
            <svg viewBox="0 0 220 140" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="40" y="35" width="140" height="80" rx="8" stroke="currentColor" stroke-width="2.5"/>
              <path d="M40 45l70 45 70-45" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="180" cy="45" r="10" fill="currentColor" opacity="0.15"/>
              <path d="M175 45l3 3 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          }
          @case ('search') {
            <svg viewBox="0 0 220 140" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="95" cy="65" r="35" stroke="currentColor" stroke-width="2.5"/>
              <path d="M120 90l25 25" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
              <path d="M78 65l17 17M95 48l17 34M78 82l34-17" stroke="currentColor" stroke-width="2" opacity="0.4" stroke-linecap="round"/>
            </svg>
          }
          @case ('assessment') {
            <svg viewBox="0 0 220 140" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="65" y="20" width="90" height="110" rx="8" stroke="currentColor" stroke-width="2.5"/>
              <path d="M85 45h50M85 60h40M85 75h50M85 90h30" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
              <circle cx="110" cy="112" r="10" fill="currentColor" opacity="0.15"/>
              <path d="M106 112l3 3 5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          }
          @default {
            <svg viewBox="0 0 220 140" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="60" y="30" width="100" height="80" rx="8" stroke="currentColor" stroke-width="2.5"/>
              <path d="M80 55h60M80 70h40M80 85h50" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
              <circle cx="180" cy="35" r="14" stroke="currentColor" stroke-width="2.5" opacity="0.6"/>
              <circle cx="40" cy="105" r="10" stroke="currentColor" stroke-width="2" opacity="0.4"/>
            </svg>
          }
        }
      </div>
      @if (title) { <h3 class="empty-title">{{ title }}</h3> }
      @if (description) { <p class="empty-desc">{{ description }}</p> }
      <div class="empty-action">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .empty-shell {
      display: flex; flex-direction: column; align-items: center;
      text-align: center; padding: 48px 24px;
      background: var(--nt-surface); border-radius: var(--nt-radius);
      border: 1px solid var(--nt-border);
    }
    .empty-illust {
      width: 180px; height: 120px;
      color: var(--nt-primary);
      opacity: 0.7;
      margin-bottom: 20px;
    }
    .empty-illust svg { width: 100%; height: 100%; }
    .empty-title {
      margin: 0 0 6px;
      font-size: 1.05rem;
      font-weight: 600;
      color: var(--nt-text-strong);
      letter-spacing: -0.01em;
    }
    .empty-desc {
      margin: 0 0 20px;
      font-size: 0.9rem;
      color: var(--nt-text-muted);
      max-width: 360px;
      line-height: 1.5;
    }
    .empty-action { display: flex; gap: 8px; }
    .empty-action:empty { display: none; }
  `]
})
export class EmptyStateComponent {
  @Input() type: 'default' | 'clients' | 'appointments' | 'menus' | 'mail' | 'search' | 'assessment' = 'default';
  @Input() title?: string;
  @Input() description?: string;
}
