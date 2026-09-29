import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PracticeService } from '../../services/practice.service';
import { PdfCustomization, Practice } from '../../models/models';

@Component({
  selector: 'app-nutri-practices',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatSlideToggleModule,
    MatSnackBarModule, MatTooltipModule, MatExpansionModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutriPractices.title' | translate }}</h1>
          <p>{{ 'nutriPractices.subtitle' | translate }}</p>
        </div>
      </div>

      <!-- Fase 9D: assinatura digital para PDF de prescricao -->
      <mat-card class="signature-card">
        <div class="sig-head">
          <mat-icon>draw</mat-icon>
          <div>
            <h3>{{ 'nutriPractices.signature.title' | translate }}</h3>
            <p class="text-muted small">{{ 'nutriPractices.signature.hint' | translate }}</p>
          </div>
        </div>
        @if (signaturePreview()) {
          <img [src]="signaturePreview()!" alt="Assinatura" class="sig-preview"/>
        } @else {
          <div class="sig-empty">
            <mat-icon>gesture</mat-icon>
            <span>{{ 'nutriPractices.signature.empty' | translate }}</span>
          </div>
        }
        <div class="sig-actions">
          <button mat-stroked-button (click)="fileInput.click()">
            <mat-icon>upload</mat-icon>
            {{ 'nutriPractices.signature.upload' | translate }}
          </button>
          <input #fileInput type="file" accept="image/png,image/jpeg" hidden (change)="onSignaturePicked($event)">
          @if (signaturePreview()) {
            <button mat-stroked-button color="warn" (click)="clearSignature()">
              <mat-icon>delete</mat-icon>
              {{ 'nutriPractices.signature.remove' | translate }}
            </button>
          }
        </div>
      </mat-card>

      @if (!visiblePractices().length) {
        <div class="empty card-surface">
          <mat-icon>badge</mat-icon>
          <p>{{ 'nutriPractices.empty' | translate }}</p>
        </div>
      } @else {
        <div class="list">
          @for (p of visiblePractices(); track p.id) {
            <mat-card class="practice-card"
                      [class.suspended]="p.status === 'SUSPENDED'"
                      [class.particular]="!p.companyName">
              <div class="head">
                <mat-icon>{{ p.companyName ? 'corporate_fare' : 'person' }}</mat-icon>
                <div>
                  <h3>{{ p.companyName ?? ('nutriPractices.private' | translate) }}</h3>
                  <span class="status-pill" [class]="'pill-' + p.status.toLowerCase()">
                    {{ ('nutriPractices.status.' + p.status) | translate }}
                  </span>
                </div>
              </div>

              @if (p.status === 'SUSPENDED') {
                <p class="text-muted small">{{ 'nutriPractices.suspendedHint' | translate }}</p>
              }

              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'nutriPractices.bio' | translate }}</mat-label>
                <textarea matInput rows="3" [(ngModel)]="p.bio"
                          [placeholder]="'nutriPractices.bioPlaceholder' | translate"></textarea>
              </mat-form-field>

              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'nutriPractices.specialties' | translate }}</mat-label>
                <input matInput [(ngModel)]="p.specialties"
                       [placeholder]="'nutriPractices.specialtiesHint' | translate">
              </mat-form-field>

              <mat-slide-toggle [(ngModel)]="p.acceptsNewClients"
                                [disabled]="p.status === 'SUSPENDED'">
                {{ 'nutriPractices.acceptsNewClients' | translate }}
              </mat-slide-toggle>

              <!-- Fase 9D-2-fix: personalizacao do PDF por practice, colapsavel -->
              <mat-expansion-panel class="pdf-panel" (opened)="loadPdfFor(p.id)">
                <mat-expansion-panel-header>
                  <mat-panel-title>
                    <mat-icon>palette</mat-icon>
                    {{ 'nutriPractices.pdf.title' | translate }}
                  </mat-panel-title>
                  <mat-panel-description>
                    {{ 'nutriPractices.pdf.hint' | translate }}
                  </mat-panel-description>
                </mat-expansion-panel-header>

                @if (pdfDrafts()[p.id]; as draft) {
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
                        <button mat-stroked-button (click)="pickLogo(p.id)">
                          <mat-icon>upload</mat-icon>
                          {{ 'nutriPractices.pdf.uploadLogo' | translate }}
                        </button>
                        @if (draft.pdfLogoBase64) {
                          <button mat-icon-button color="warn"
                                  (click)="draft.pdfLogoBase64 = null; savePdf(p.id)">
                            <mat-icon>delete</mat-icon>
                          </button>
                        }
                      </div>
                    </div>
                    <div class="pdf-col">
                      <label class="pdf-label">{{ 'nutriPractices.pdf.color' | translate }}</label>
                      <div class="color-row">
                        <input type="color" [value]="draft.pdfPrimaryColor || '#0e8f83'"
                               (change)="onColorChange(p.id, $event)" class="color-input">
                        <span class="color-swatch" [style.background]="draft.pdfPrimaryColor || '#0e8f83'"></span>
                        <span class="color-code text-muted small">{{ draft.pdfPrimaryColor || '#0e8f83' }}</span>
                        @if (draft.pdfPrimaryColor) {
                          <button mat-icon-button (click)="draft.pdfPrimaryColor = null; savePdf(p.id)"
                                  [matTooltip]="'nutriPractices.pdf.resetColor' | translate">
                            <mat-icon>restart_alt</mat-icon>
                          </button>
                        }
                      </div>
                    </div>
                  </div>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic">
                    <mat-label>{{ 'nutriPractices.pdf.headerText' | translate }}</mat-label>
                    <textarea matInput rows="2" [(ngModel)]="draft.pdfHeaderText"
                              [placeholder]="'nutriPractices.pdf.headerPlaceholder' | translate"></textarea>
                    <mat-hint>{{ 'nutriPractices.pdf.htmlAllowed' | translate }}</mat-hint>
                  </mat-form-field>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic">
                    <mat-label>{{ 'nutriPractices.pdf.footerText' | translate }}</mat-label>
                    <textarea matInput rows="3" [(ngModel)]="draft.pdfFooterText"
                              [placeholder]="'nutriPractices.pdf.footerPlaceholder' | translate"></textarea>
                    <mat-hint>{{ 'nutriPractices.pdf.htmlAllowed' | translate }}</mat-hint>
                  </mat-form-field>
                  <div class="pdf-actions">
                    <button mat-stroked-button (click)="previewPdf(p.id)"
                            [disabled]="loadingPreview() === p.id">
                      <mat-icon>visibility</mat-icon>
                      {{ (loadingPreview() === p.id ? 'nutriPractices.pdf.previewLoading' : 'nutriPractices.pdf.preview') | translate }}
                    </button>
                    <button mat-flat-button color="primary" (click)="savePdf(p.id)"
                            [disabled]="savingPdf() === p.id">
                      <mat-icon>save</mat-icon>
                      {{ (savingPdf() === p.id ? 'app.actions.saving' : 'app.actions.save') | translate }}
                    </button>
                  </div>
                }
              </mat-expansion-panel>

              <div class="actions">
                <button mat-flat-button color="primary" (click)="save(p)"
                        [disabled]="p.status === 'SUSPENDED' || saving().has(p.id)">
                  <mat-icon>save</mat-icon>
                  {{ 'app.actions.save' | translate }}
                </button>
              </div>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .list { display: flex; flex-direction: column; gap: 16px; }
    .practice-card {
      padding: 20px !important;
      display: flex; flex-direction: column; gap: 12px;
      border-left: 4px solid var(--nt-primary) !important;
    }
    .practice-card.particular { border-left-color: var(--nt-accent) !important; }
    .practice-card.suspended { opacity: 0.7; border-left-color: var(--nt-text-muted) !important; }

    .head { display: flex; align-items: center; gap: 12px;
      mat-icon { color: var(--nt-primary); font-size: 28px; width: 28px; height: 28px; }
      h3 { margin: 0; font-size: 1.1rem; font-weight: 600; }
    }

    .status-pill {
      display: inline-flex; padding: 2px 10px; border-radius: 12px;
      font-size: 0.72rem; font-weight: 700; letter-spacing: 0.02em; margin-top: 4px;
    }
    .pill-active { background: #d1fae5; color: #065f46; }
    .pill-suspended { background: var(--nt-surface-2); color: var(--nt-text-muted); }

    .actions { display: flex; justify-content: flex-end; }
    .small { font-size: 0.85rem; margin: 0; }

    .signature-card {
      padding: 20px !important; margin-bottom: 20px;
      display: flex; flex-direction: column; gap: 12px;
      border-left: 4px solid var(--nt-primary-dark) !important;
    }
    .sig-head { display: flex; align-items: center; gap: 12px;
      mat-icon { color: var(--nt-primary-dark); font-size: 28px; width: 28px; height: 28px; }
      h3 { margin: 0; font-size: 1.1rem; font-weight: 600; }
    }
    .sig-preview {
      max-width: 260px; max-height: 90px;
      border: 1px dashed var(--nt-border); border-radius: 8px;
      padding: 6px; background: #fff;
      align-self: flex-start;
    }
    .sig-empty {
      display: flex; align-items: center; gap: 8px;
      padding: 20px; background: var(--nt-surface-2); border-radius: 8px;
      color: var(--nt-text-muted); font-size: 0.9rem;
      mat-icon { font-size: 24px; width: 24px; height: 24px; }
    }
    .sig-actions { display: flex; gap: 8px; }

    .pdf-card {
      padding: 20px !important; margin-bottom: 20px;
      display: flex; flex-direction: column; gap: 14px;
      border-left: 4px solid var(--nt-purple) !important;
    }
    .pdf-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 20px;
    }
    .pdf-col { display: flex; flex-direction: column; gap: 8px; }
    .pdf-label {
      font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted); font-weight: 600;
    }
    .logo-preview {
      max-width: 200px; max-height: 60px;
      border: 1px dashed var(--nt-border); border-radius: 8px;
      padding: 6px; background: #fff; align-self: flex-start;
    }
    .logo-empty {
      display: flex; align-items: center; gap: 8px;
      padding: 14px; background: var(--nt-surface-2); border-radius: 8px;
      color: var(--nt-text-muted); font-size: 0.9rem;
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }
    .row-actions { display: flex; align-items: center; gap: 4px; }
    .color-row { display: flex; align-items: center; gap: 8px; }
    .color-input {
      width: 44px; height: 44px; border: 1px solid var(--nt-border);
      border-radius: 8px; cursor: pointer; padding: 0; background: transparent;
    }
    .color-swatch {
      width: 30px; height: 30px; border-radius: 8px;
      border: 1px solid var(--nt-border);
    }
    .color-code { font-family: 'JetBrains Mono', monospace; }
    .pdf-actions { display: flex; justify-content: flex-end; gap: 8px; }
  `]
})
export class NutriPracticesComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  private practiceCtx = inject(PracticeService);

  practices = signal<Practice[]>([]);
  /**
   * Fase 9D-2-fix seguimento: a página respeita a practice ativa do PracticeSwitcher.
   * Se nenhuma estiver selecionada (activePracticeId = null), mostra todas as practices
   * do nutri (comportamento "gerenciar todos os perfis"). Se houver uma ativa,
   * filtra pra exibir só ela — replicando a semântica de tenant do resto do app.
   */
  visiblePractices = computed(() => {
    const active = this.practiceCtx.activePracticeId();
    const all = this.practices();
    if (active == null) return all;
    const only = all.filter(p => p.id === active);
    // Se a activePracticeId aponta pra uma practice que nao esta na lista (edge case),
    // cai pra mostrar todas — melhor que mostrar tela vazia.
    return only.length > 0 ? only : all;
  });
  saving = signal(new Set<number>());
  signaturePreview = signal<string | null>(null);

  // Fase 9D-2-fix: customizacao do PDF por practice.
  // Signal com map practiceId -> customizacao carregada lazy no expansion open.
  pdfDrafts = signal<Record<number, PdfCustomization>>({});
  // Signals de status "por practice" — armazenam o id da practice em curso, ou null.
  savingPdf = signal<number | null>(null);
  loadingPreview = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
    this.api.nutriGetSignature().subscribe(r => {
      if (r?.signatureBase64) this.signaturePreview.set(r.signatureBase64);
    });
  }

  /** Carrega a customizacao da practice. Se falhar (ex.: backend antigo), inicializa vazio. */
  loadPdfFor(practiceId: number) {
    if (this.pdfDrafts()[practiceId]) return;
    this.api.nutriGetPdfTemplate(practiceId).subscribe({
      next: t => this.pdfDrafts.update(m => ({ ...m, [practiceId]: { ...t } })),
      error: () => this.pdfDrafts.update(m => ({ ...m, [practiceId]:
          { pdfLogoBase64: null, pdfPrimaryColor: null, pdfHeaderText: null, pdfFooterText: null } }))
    });
  }

  pickLogo(practiceId: number) {
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
        const draft = this.pdfDrafts()[practiceId] ?? {};
        draft.pdfLogoBase64 = reader.result as string;
        this.pdfDrafts.update(m => ({ ...m, [practiceId]: { ...draft } }));
        this.savePdf(practiceId);
      };
      reader.readAsDataURL(file);
    };
    input.click();
  }

  onColorChange(practiceId: number, ev: Event) {
    const v = (ev.target as HTMLInputElement).value;
    const draft = this.pdfDrafts()[practiceId] ?? {};
    draft.pdfPrimaryColor = v;
    this.pdfDrafts.update(m => ({ ...m, [practiceId]: { ...draft } }));
    this.savePdf(practiceId);
  }

  savePdf(practiceId: number) {
    const draft = this.pdfDrafts()[practiceId];
    if (!draft) return;
    this.savingPdf.set(practiceId);
    this.api.nutriPutPdfTemplate(practiceId, draft).subscribe({
      next: () => {
        this.savingPdf.set(null);
        this.snack.open(this.translate.instant('nutriPractices.pdf.saved'),
                        'OK', { duration: 2000 });
      },
      error: err => {
        this.savingPdf.set(null);
        this.snack.open(err?.error?.error ?? this.translate.instant('nutriPractices.pdf.err'),
                        'OK', { duration: 3000 });
      }
    });
  }

  previewPdf(practiceId: number) {
    this.loadingPreview.set(practiceId);
    this.api.nutriPdfPreview(practiceId).subscribe({
      next: blob => {
        this.loadingPreview.set(null);
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: () => {
        this.loadingPreview.set(null);
        this.snack.open(this.translate.instant('nutriPractices.pdf.err'),
                        'OK', { duration: 3000 });
      }
    });
  }

  load() {
    this.api.nutriMyPractices().subscribe(list => {
      this.practices.set(list);
      // Fase 9D-2-fix seguimento: pré-carrega customização de todas as practices.
      // Antes fazia lazy no evento (opened) do mat-expansion, mas o evento
      // podia não disparar em alguns cenários — resultado: painel abria vazio.
      for (const p of list) this.loadPdfFor(p.id);
    });
  }

  onSignaturePicked(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > 2_000_000) {
      this.snack.open(this.translate.instant('nutriPractices.signature.tooBig'), 'OK', { duration: 3000 });
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      this.signaturePreview.set(dataUrl);
      this.api.nutriPutSignature(dataUrl).subscribe({
        next: () => this.snack.open(this.translate.instant('nutriPractices.signature.saved'),
                                     'OK', { duration: 2500 }),
        error: () => this.snack.open(this.translate.instant('nutriPractices.signature.err'),
                                      'OK', { duration: 3000 })
      });
    };
    reader.readAsDataURL(file);
    input.value = ''; // limpa pra permitir re-upload do mesmo arquivo
  }

  clearSignature() {
    this.signaturePreview.set(null);
    this.api.nutriPutSignature(null).subscribe();
  }

  save(p: Practice) {
    const s = new Set(this.saving()); s.add(p.id); this.saving.set(s);
    this.api.nutriUpdatePractice(p.id, {
      bio: p.bio ?? undefined,
      specialties: p.specialties ?? undefined,
      acceptsNewClients: p.acceptsNewClients
    }).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriPractices.savedOk'), 'OK', { duration: 2500 });
        const s2 = new Set(this.saving()); s2.delete(p.id); this.saving.set(s2);
      },
      error: err => {
        this.snack.open(err?.error?.error ?? this.translate.instant('nutriPractices.saveErr'),
                        'OK', { duration: 3000 });
        const s2 = new Set(this.saving()); s2.delete(p.id); this.saving.set(s2);
      }
    });
  }
}
