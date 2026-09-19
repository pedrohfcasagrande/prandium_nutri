import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

/**
 * Barra reutilizável de busca + ordenação (Fase 6B).
 * Emite (search) com debounce 300ms e (sortChange) imediato.
 */
@Component({
  selector: 'app-search-sort-bar',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatButtonToggleModule,
    MatTooltipModule, TranslateModule
  ],
  template: `
    <div class="sb">
      <mat-form-field appearance="outline" class="search" subscriptSizing="dynamic">
        <mat-icon matPrefix>search</mat-icon>
        <input matInput [ngModel]="query()" (ngModelChange)="onType($event)"
               [placeholder]="placeholder || (t.instant('search.placeholder'))">
        @if (query()) {
          <button mat-icon-button matSuffix (click)="clear()" tabindex="-1"
                  [attr.aria-label]="t.instant('search.clear')">
            <mat-icon>close</mat-icon>
          </button>
        }
      </mat-form-field>

      <mat-button-toggle-group [value]="sort" (change)="onSort($event.value)"
                                hideSingleSelectionIndicator
                                [matTooltip]="t.instant('search.sortTooltip')">
        <mat-button-toggle value="created">
          <mat-icon>history</mat-icon>
        </mat-button-toggle>
        <mat-button-toggle value="name">
          <mat-icon>sort_by_alpha</mat-icon>
        </mat-button-toggle>
      </mat-button-toggle-group>
    </div>
  `,
  styles: [`
    .sb { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .search { flex: 1 1 240px; max-width: 420px; }
  `]
})
export class SearchSortBarComponent {
  t = inject(TranslateService);

  @Input() placeholder?: string;
  @Input() sort: 'created' | 'name' = 'name';
  @Output() search = new EventEmitter<string>();
  @Output() sortChange = new EventEmitter<'created' | 'name'>();

  query = signal('');
  private timer: any = null;

  onType(v: string) {
    this.query.set(v);
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.search.emit((v ?? '').trim()), 300);
  }

  clear() {
    this.query.set('');
    if (this.timer) clearTimeout(this.timer);
    this.search.emit('');
  }

  onSort(v: 'created' | 'name') {
    this.sort = v;
    this.sortChange.emit(v);
  }
}
