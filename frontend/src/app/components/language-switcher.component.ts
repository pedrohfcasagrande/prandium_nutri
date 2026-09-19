import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '../services/language.service';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [
    CommonModule, MatButtonModule, MatIconModule,
    MatMenuModule, MatDividerModule, MatTooltipModule, TranslateModule
  ],
  template: `
    <button mat-icon-button [matMenuTriggerFor]="langMenu"
            [matTooltip]="'app.language.tooltip' | translate">
      <mat-icon>language</mat-icon>
    </button>
    <mat-menu #langMenu="matMenu">
      <button mat-menu-item (click)="lang.setMode('auto')">
        <mat-icon>{{ lang.mode() === 'auto' ? 'check' : 'translate' }}</mat-icon>
        <span>{{ 'app.theme.auto' | translate }}</span>
      </button>
      <mat-divider></mat-divider>
      <button mat-menu-item (click)="lang.setMode('pt')">
        <mat-icon>{{ lang.mode() === 'pt' ? 'check' : 'flag' }}</mat-icon>
        <span>{{ 'app.language.pt' | translate }}</span>
      </button>
      <button mat-menu-item (click)="lang.setMode('en')">
        <mat-icon>{{ lang.mode() === 'en' ? 'check' : 'flag' }}</mat-icon>
        <span>{{ 'app.language.en' | translate }}</span>
      </button>
      <button mat-menu-item (click)="lang.setMode('fr')">
        <mat-icon>{{ lang.mode() === 'fr' ? 'check' : 'flag' }}</mat-icon>
        <span>{{ 'app.language.fr' | translate }}</span>
      </button>
    </mat-menu>
  `
})
export class LanguageSwitcherComponent {
  lang = inject(LanguageService);
}
