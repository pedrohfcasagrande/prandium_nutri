import { Injectable, inject } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslateService } from '@ngx-translate/core';

/**
 * MatPaginatorIntl reativo à mudança de idioma (Fase 7B fix).
 * Traduz labels via chaves `paginator.*` do ngx-translate.
 */
@Injectable({ providedIn: 'root' })
export class I18nPaginatorIntl extends MatPaginatorIntl {
  private translate = inject(TranslateService);

  constructor() {
    super();
    this.translate.onLangChange.subscribe(() => this.refresh());
    this.refresh();
  }

  private refresh() {
    this.itemsPerPageLabel = this.translate.instant('paginator.itemsPerPage');
    this.nextPageLabel = this.translate.instant('paginator.nextPage');
    this.previousPageLabel = this.translate.instant('paginator.previousPage');
    this.firstPageLabel = this.translate.instant('paginator.firstPage');
    this.lastPageLabel = this.translate.instant('paginator.lastPage');
    this.changes.next();
  }

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0 || pageSize === 0) {
      return this.translate.instant('paginator.rangeEmpty', { total: length });
    }
    const start = page * pageSize;
    const end = Math.min(start + pageSize, length);
    return this.translate.instant('paginator.range', { start: start + 1, end, total: length });
  };
}
