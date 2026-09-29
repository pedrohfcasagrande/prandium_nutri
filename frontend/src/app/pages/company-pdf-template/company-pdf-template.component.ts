import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PdfCustomization } from '../../models/models';

/**
 * Fase 9H-1: admin da empresa personaliza o template PDF que sera usado
 * por todas as practices vinculadas a essa empresa.
 * Layout espelha o card do nutri-practices, mas mora numa tela propria.
 */
@Component({
  selector: 'app-company-pdf-template',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatFormFieldModule,
            MatInputModule, MatButtonModule, MatIconModule, MatSnackBarModule,
            MatTooltipModule, TranslateModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'companyPdfTemplate.title' | translate }}</h1>
          <p>{{ 'companyPdfTemplate.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-card class="pdf-card">
        <div class="pdf-grid">
          <div class="pdf-col">
            <label class="pdf-label">{{ 'nutriPractices.pdf.logo' | translate }}</label>
            @if (draft.pdfLogoBase64) {
              <img [src]="draft.pdfLogoBase64" alt="Logo" class="logo-preview"/>
            } @else {
              <div class="logo-empty">
                <mat-icon>image</mat-icon>
                <span>{{ 'nutriPractices.pdf.logoEmpty' | translate }}</span>
              </div>
            }
            <div class="row-actions">
              <button mat-stroked-button (click)="pickLogo()">
                <mat-icon>upload</mat-icon>
                {{ 'nutriPractices.pdf.uploadLogo' | translate }}
              </button>
              @if (draft.pdfLogoBase64) {
                <button mat-icon-button color="warn"
                        (click)="draft.pdfLogoBase64 = null; save()">
                  <mat-icon>delete</mat-icon>
                </button>
              }
            </div>
          </div>
          <div class="pdf-col">
            <label class="pdf-label">{{ 'nutriPractices.pdf.color' | translate }}</label>
            <div class="color-row">
              <input type="color" [value]="draft.pdfPrimaryColor || '#0e8f83'"
                     (change)="onColorChange($event)" class="color-input">
              <span class="color-swatch" [style.background]="draft.pdfPrimaryColor || '#0e8f83'"></span>
              <span class="color-code text-muted small">{{ draft.pdfPrimaryColor || '#0e8f83' }}</span>
              @if (draft.pdfPrimaryColor) {
                <button mat-icon-button (click)="draft.pdfPrimaryColor = null; save()">
                  <mat-icon>restart_alt</mat-icon>
                </button>
              }
            </div>
          </div>
        </div>

        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'nutriPractices.pdf.headerLine' | translate }}</mat-label>
          <input matInput [(ngModel)]="draft.pdfHeaderLine"
                 [placeholder]="'nutriPractices.pdf.headerPlaceholder' | translate"
                 maxlength="200">
          <mat-hint>{{ 'nutriPractices.pdf.headerHint' | translate }}</mat-hint>
        </mat-form-field>

        <div class="section-sep">{{ 'nutriPractices.pdf.footerSection' | translate }}</div>

        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'nutriPractices.pdf.address' | translate }}</mat-label>
          <mat-icon matPrefix>place</mat-icon>
          <input matInput [(ngModel)]="draft.pdfFooterAddress"
                 [placeholder]="'nutriPractices.pdf.addressPlaceholder' | translate" maxlength="300">
        </mat-form-field>
        <div class="pdf-row">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'nutriPractices.pdf.phone' | translate }}</mat-label>
            <mat-icon matPrefix>call</mat-icon>
            <input matInput [(ngModel)]="draft.pdfFooterPhone" maxlength="80">
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'nutriPractices.pdf.email' | translate }}</mat-label>
            <mat-icon matPrefix>mail</mat-icon>
            <input matInput type="email" [(ngModel)]="draft.pdfFooterEmail" maxlength="150">
          </mat-form-field>
        </div>
        <div class="pdf-row">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'nutriPractices.pdf.website' | translate }}</mat-label>
            <mat-icon matPrefix>language</mat-icon>
            <input matInput type="url" [(ngModel)]="draft.pdfFooterWebsite" maxlength="200"
                   placeholder="https://...">
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'nutriPractices.pdf.instagram' | translate }}</mat-label>
            <mat-icon matPrefix>photo_camera</mat-icon>
            <input matInput [(ngModel)]="draft.pdfFooterInstagram" maxlength="80"
                   placeholder="@usuario">
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'nutriPractices.pdf.note' | translate }}</mat-label>
          <textarea matInput rows="2" [(ngModel)]="draft.pdfFooterNote"
                    [placeholder]="'nutriPractices.pdf.notePlaceholder' | translate" maxlength="500"></textarea>
          <mat-hint>{{ 'nutriPractices.pdf.noteHint' | translate }}</mat-hint>
        </mat-form-field>

        <div class="pdf-actions">
          <button mat-stroked-button (click)="preview()" [disabled]="previewing()">
            <mat-icon>visibility</mat-icon>
            {{ (previewing() ? 'nutriPractices.pdf.previewLoading' : 'nutriPractices.pdf.preview') | translate }}
          </button>
          <button mat-flat-button color="primary" (click)="save()" [disabled]="saving()">
            <mat-icon>save</mat-icon>
            {{ (saving() ? 'app.actions.saving' : 'app.actions.save') | translate }}
          </button>
        </div>
      </mat-card>
    </div>
  `,
  styles: [`
    .pdf-card { padding: 20px !important; display: flex; flex-direction: column; gap: 14px;
      border-left: 4px solid var(--nt-purple) !important; }
    .pdf-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 20px; }
    .pdf-col { display: flex; flex-direction: column; gap: 8px; }
    .pdf-label { font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted); font-weight: 600; }
    .logo-preview { max-width: 200px; max-height: 60px;
      border: 1px dashed var(--nt-border); border-radius: 8px;
      padding: 6px; background: #fff; align-self: flex-start; }
    .logo-empty { display: flex; align-items: center; gap: 8px;
      padding: 14px; background: var(--nt-surface-2); border-radius: 8px;
      color: var(--nt-text-muted); font-size: 0.9rem;
      mat-icon { font-size: 20px; width: 20px; height: 20px; } }
    .row-actions { display: flex; align-items: center; gap: 4px; }
    .color-row { display: flex; align-items: center; gap: 8px; }
    .color-input { width: 44px; height: 44px; border: 1px solid var(--nt-border);
      border-radius: 8px; cursor: pointer; padding: 0; background: transparent; }
    .color-swatch { width: 30px; height: 30px; border-radius: 8px; border: 1px solid var(--nt-border); }
    .color-code { font-family: 'JetBrains Mono', monospace; }
    .section-sep { font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted); font-weight: 600;
      border-top: 1px solid var(--nt-border); padding-top: 18px; margin-top: 8px; }
    .pdf-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
      @media (max-width: 600px) { grid-template-columns: 1fr; } }
    .pdf-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
    mat-form-field { display: block; }
  `]
})
export class CompanyPdfTemplateComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  draft: PdfCustomization = {};
  saving = signal(false);
  previewing = signal(false);

  ngOnInit() {
    this.api.companyGetPdfTemplate().subscribe(view => this.draft = { ...view.template });
  }

  pickLogo() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/png,image/jpeg';
    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) return;
      if (file.size > 2_000_000) {
        this.snack.open(this.translate.instant('nutriPractices.signature.tooBig'), 'OK', { duration: 3000 });
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        this.draft.pdfLogoBase64 = reader.result as string;
        this.save();
      };
      reader.readAsDataURL(file);
    };
    input.click();
  }

  onColorChange(ev: Event) {
    this.draft.pdfPrimaryColor = (ev.target as HTMLInputElement).value;
    this.save();
  }

  save() {
    this.saving.set(true);
    this.api.companyPutPdfTemplate(this.draft).subscribe({
      next: () => {
        this.saving.set(false);
        this.snack.open(this.translate.instant('nutriPractices.pdf.saved'), 'OK', { duration: 2000 });
      },
      error: () => {
        this.saving.set(false);
        this.snack.open(this.translate.instant('nutriPractices.pdf.err'), 'OK', { duration: 3000 });
      }
    });
  }

  preview() {
    this.previewing.set(true);
    this.api.companyPdfPreview().subscribe({
      next: blob => {
        this.previewing.set(false);
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: () => {
        this.previewing.set(false);
        this.snack.open(this.translate.instant('nutriPractices.pdf.err'), 'OK', { duration: 3000 });
      }
    });
  }
}
