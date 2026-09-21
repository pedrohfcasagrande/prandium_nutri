import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-theme-switcher',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatMenuModule, MatIconModule, MatTooltipModule, TranslateModule],
  template: `
    <button mat-icon-button [matMenuTriggerFor]="menu"
            [matTooltip]="'app.theme.tooltip' | translate:{ value: label() | translate }">
      <mat-icon>{{ theme.effectiveDark() ? 'dark_mode' : 'light_mode' }}</mat-icon>
    </button>
    <mat-menu #menu="matMenu">
      <button mat-menu-item (click)="theme.setMode('auto')">
        <mat-icon>{{ theme.mode() === 'auto' ? 'check' : 'brightness_auto' }}</mat-icon>
        <span>{{ 'app.theme.auto' | translate }}</span>
      </button>
      <button mat-menu-item (click)="theme.setMode('light')">
        <mat-icon>{{ theme.mode() === 'light' ? 'check' : 'light_mode' }}</mat-icon>
        <span>{{ 'app.theme.light' | translate }}</span>
      </button>
      <button mat-menu-item (click)="theme.setMode('dark')">
        <mat-icon>{{ theme.mode() === 'dark' ? 'check' : 'dark_mode' }}</mat-icon>
        <span>{{ 'app.theme.dark' | translate }}</span>
      </button>
    </mat-menu>
  `
})
export class ThemeSwitcherComponent {
  theme = inject(ThemeService);
  label = () => {
    const m = this.theme.mode();
    return m === 'auto' ? 'app.theme.auto' : m === 'dark' ? 'app.theme.dark' : 'app.theme.light';
  };
}
