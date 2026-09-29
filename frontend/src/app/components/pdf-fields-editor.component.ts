import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { TranslateModule } from '@ngx-translate/core';
import { PdfFieldType, PdfTemplateField } from '../models/models';

/**
 * Fase 9H-2: editor da lista modular de campos do rodape do PDF.
 * Recebe fields (2-way via [fields]/(fieldsChange)) e disabled (pra nutri vinculado).
 * Emite `save` quando qualquer mudanca deve ser persistida — o pai decide quando salva.
 */
@Component({
  selector: 'app-pdf-fields-editor',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatTooltipModule, MatMenuModule, TranslateModule
  ],
  template: `
    <div class="fields">
      @for (f of fields; track $index) {
        <div class="field-row">
          <div class="field-head">
            <mat-icon>{{ iconOf(f.type) }}</mat-icon>
            <input type="text" class="label-input" [(ngModel)]="f.label"
                   [disabled]="disabled" maxlength="100">
            <span class="type-pill" [attr.data-type]="f.type"
                  [matTooltip]="'pdfFields.type.' + f.type | translate">
              {{ 'pdfFields.type.' + f.type | translate }}
            </span>
            <span class="spacer"></span>
            @if (!disabled) {
              <button mat-icon-button (click)="move($index, -1)" [disabled]="$index === 0"
                      [matTooltip]="'pdfFields.moveUp' | translate">
                <mat-icon>arrow_upward</mat-icon>
              </button>
              <button mat-icon-button (click)="move($index, 1)" [disabled]="$index === fields.length - 1"
                      [matTooltip]="'pdfFields.moveDown' | translate">
                <mat-icon>arrow_downward</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="remove($index)"
                      [matTooltip]="'pdfFields.remove' | translate">
                <mat-icon>delete</mat-icon>
              </button>
            }
          </div>
          @if (f.type === 'TEXTAREA') {
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <textarea matInput rows="2" [(ngModel)]="f.value" [disabled]="disabled"
                        maxlength="1000"></textarea>
            </mat-form-field>
          } @else {
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <input matInput [type]="inputTypeOf(f.type)" [(ngModel)]="f.value"
                     [disabled]="disabled" maxlength="500"
                     [placeholder]="placeholderOf(f.type)">
            </mat-form-field>
          }
        </div>
      }
      @if (!fields.length) {
        <div class="empty text-muted small">{{ 'pdfFields.empty' | translate }}</div>
      }
      @if (!disabled) {
        <button mat-stroked-button [matMenuTriggerFor]="typeMenu" class="add-btn">
          <mat-icon>add</mat-icon>
          {{ 'pdfFields.add' | translate }}
        </button>
        <mat-menu #typeMenu="matMenu">
          @for (t of allTypes; track t) {
            <button mat-menu-item (click)="add(t)">
              <mat-icon>{{ iconOf(t) }}</mat-icon>
              <span>{{ 'pdfFields.type.' + t | translate }}</span>
            </button>
          }
        </mat-menu>
      }
    </div>
  `,
  styles: [`
    .fields { display: flex; flex-direction: column; gap: 12px; }
    .field-row {
      display: flex; flex-direction: column; gap: 6px;
      padding: 12px 14px;
      background: var(--nt-surface-2);
      border-radius: 10px;
      border-left: 3px solid var(--nt-primary);
    }
    .field-head { display: flex; align-items: center; gap: 8px; }
    .field-head mat-icon { color: var(--nt-primary-dark); font-size: 20px; width: 20px; height: 20px; }
    .label-input {
      background: transparent; border: none; outline: none;
      font-weight: 600; color: var(--nt-text); font-size: 0.95rem;
      padding: 2px 4px; border-radius: 4px;
      min-width: 80px; max-width: 240px;
      &:focus { background: var(--nt-surface); }
      &:disabled { color: var(--nt-text-muted); }
    }
    .type-pill {
      font-size: 0.7rem; font-weight: 700; letter-spacing: 0.04em;
      padding: 2px 8px; border-radius: 999px;
      background: var(--nt-primary-soft); color: var(--nt-primary-dark);
      text-transform: uppercase;
    }
    .type-pill[data-type=TEXTAREA] { background: var(--nt-accent-soft); color: var(--nt-accent); }
    .spacer { flex: 1; }
    .empty { padding: 12px; text-align: center; }
    .add-btn { align-self: flex-start; margin-top: 4px; }
    mat-form-field { display: block; }
  `]
})
export class PdfFieldsEditorComponent {
  @Input() fields: PdfTemplateField[] = [];
  @Input() disabled = false;
  @Output() fieldsChange = new EventEmitter<PdfTemplateField[]>();
  @Output() save = new EventEmitter<void>();

  allTypes: PdfFieldType[] = ['TEXT', 'TEXTAREA', 'URL', 'EMAIL', 'TEL', 'INSTAGRAM'];

  iconOf(t: PdfFieldType): string {
    switch (t) {
      case 'TEL':       return 'call';
      case 'EMAIL':     return 'mail';
      case 'URL':       return 'language';
      case 'INSTAGRAM': return 'photo_camera';
      case 'TEXTAREA':  return 'notes';
      default:          return 'short_text';
    }
  }

  inputTypeOf(t: PdfFieldType): string {
    switch (t) {
      case 'URL':   return 'url';
      case 'EMAIL': return 'email';
      case 'TEL':   return 'tel';
      default:      return 'text';
    }
  }

  placeholderOf(t: PdfFieldType): string {
    switch (t) {
      case 'INSTAGRAM': return '@usuario';
      case 'URL':       return 'https://...';
      default:          return '';
    }
  }

  add(type: PdfFieldType) {
    const defaults: Record<PdfFieldType, string> = {
      TEXT: 'Texto', TEXTAREA: 'Observações',
      URL: 'Site', EMAIL: 'E-mail', TEL: 'Telefone', INSTAGRAM: 'Instagram'
    };
    this.fields = [...this.fields, { label: defaults[type], type, value: '' }];
    this.fieldsChange.emit(this.fields);
    // Nao dispara save automatico — user preenche o valor e clica Salvar
  }

  remove(i: number) {
    this.fields = this.fields.filter((_, idx) => idx !== i);
    this.fieldsChange.emit(this.fields);
    this.save.emit();
  }

  move(i: number, delta: number) {
    const j = i + delta;
    if (j < 0 || j >= this.fields.length) return;
    const copy = [...this.fields];
    [copy[i], copy[j]] = [copy[j], copy[i]];
    this.fields = copy;
    this.fieldsChange.emit(this.fields);
    this.save.emit();
  }
}
