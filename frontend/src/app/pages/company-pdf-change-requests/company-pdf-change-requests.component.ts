import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PdfChangeRequest, PdfChangeRequestStatus } from '../../models/models';

/**
 * Fase 9H-3: tela do admin da empresa pra revisar sugestões de mudança no PDF.
 * Toggle Pendentes / Histórico. Cada item mostra quem propôs, note, e resumo
 * do estado proposto (header + lista de fields). Aprovar aplica direto; rejeitar
 * abre input inline pra motivo.
 */
@Component({
  selector: 'app-company-pdf-change-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatButtonModule, MatIconModule,
            MatSnackBarModule, MatFormFieldModule, MatInputModule, MatButtonToggleModule,
            TranslateModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'pdfChangeRequests.title' | translate }}</h1>
          <p>{{ 'pdfChangeRequests.subtitle' | translate }}</p>
        </div>
        <mat-button-toggle-group [(ngModel)]="filter" (change)="load()"
                                 hideSingleSelectionIndicator>
          <mat-button-toggle value="PENDING">{{ 'pdfChangeRequests.pending' | translate }}</mat-button-toggle>
          <mat-button-toggle value="">{{ 'pdfChangeRequests.all' | translate }}</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      @if (requests().length === 0) {
        <div class="empty card-surface">
          <mat-icon>drafts</mat-icon>
          <p>{{ 'pdfChangeRequests.empty' | translate }}</p>
        </div>
      } @else {
        <div class="req-list">
          @for (r of requests(); track r.id) {
            <mat-card class="req-card" [class]="'status-' + r.status.toLowerCase()">
              <div class="req-head">
                <div class="head-left">
                  <div class="who">
                    <mat-icon>person</mat-icon>
                    <strong>{{ r.requestedByName }}</strong>
                    <span class="text-muted small">{{ r.createdAt | date:'short' }}</span>
                  </div>
                  <span class="status-pill" [class]="'pill-' + r.status.toLowerCase()">
                    {{ 'pdfChangeRequests.status.' + r.status | translate }}
                  </span>
                </div>
              </div>

              @if (r.note) {
                <div class="req-note">
                  <mat-icon>chat</mat-icon>
                  <span>{{ r.note }}</span>
                </div>
              }

              <div class="proposal">
                <div class="prop-item" *ngIf="r.proposed.pdfHeaderLine">
                  <span class="p-label">{{ 'nutriPractices.pdf.headerLine' | translate }}</span>
                  <span class="p-value">{{ r.proposed.pdfHeaderLine }}</span>
                </div>
                <div class="prop-item" *ngIf="r.proposed.pdfPrimaryColor">
                  <span class="p-label">{{ 'nutriPractices.pdf.color' | translate }}</span>
                  <span class="color-chip" [style.background]="r.proposed.pdfPrimaryColor"></span>
                  <span class="p-value">{{ r.proposed.pdfPrimaryColor }}</span>
                </div>
                @if (r.proposed.fields?.length) {
                  <div class="fields-list">
                    <span class="p-label">{{ 'pdfChangeRequests.fieldsProposed' | translate }}</span>
                    <ul>
                      @for (f of r.proposed.fields; track f.id ?? $index) {
                        <li>
                          <span class="type-chip">{{ 'pdfFields.type.' + f.type | translate }}</span>
                          <strong>{{ f.label }}</strong>: <span>{{ f.value }}</span>
                        </li>
                      }
                    </ul>
                  </div>
                }
              </div>

              @if (r.status === 'PENDING') {
                @if (rejectingId() === r.id) {
                  <mat-form-field appearance="outline" subscriptSizing="dynamic" class="reject-note">
                    <mat-label>{{ 'pdfChangeRequests.rejectReason' | translate }}</mat-label>
                    <input matInput [(ngModel)]="rejectNote" maxlength="500">
                  </mat-form-field>
                  <div class="req-actions">
                    <button mat-stroked-button (click)="cancelReject()">
                      {{ 'app.actions.cancel' | translate }}
                    </button>
                    <button mat-flat-button color="warn" (click)="reject(r)">
                      <mat-icon>close</mat-icon>
                      {{ 'pdfChangeRequests.confirmReject' | translate }}
                    </button>
                  </div>
                } @else {
                  <div class="req-actions">
                    <button mat-stroked-button color="warn" (click)="startReject(r.id)">
                      <mat-icon>close</mat-icon>
                      {{ 'pdfChangeRequests.reject' | translate }}
                    </button>
                    <button mat-flat-button color="primary" (click)="approve(r)">
                      <mat-icon>check</mat-icon>
                      {{ 'pdfChangeRequests.approve' | translate }}
                    </button>
                  </div>
                }
              } @else {
                <div class="review-info text-muted small">
                  <mat-icon>{{ r.status === 'APPROVED' ? 'check_circle' : 'cancel' }}</mat-icon>
                  {{ 'pdfChangeRequests.reviewedBy' | translate:{ name: r.reviewedByName, date: (r.reviewedAt | date:'short') } }}
                  @if (r.reviewNote) { — <em>{{ r.reviewNote }}</em> }
                </div>
              }
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .req-list { display: flex; flex-direction: column; gap: 12px; }
    .req-card { padding: 16px 20px !important; display: flex; flex-direction: column; gap: 10px;
      border-left: 4px solid var(--nt-accent) !important; }
    .req-card.status-approved { border-left-color: var(--nt-success) !important; opacity: 0.9; }
    .req-card.status-rejected { border-left-color: var(--nt-danger) !important; opacity: 0.75; }
    .req-head { display: flex; justify-content: space-between; align-items: center; }
    .head-left { display: flex; align-items: center; gap: 12px; }
    .who { display: inline-flex; align-items: center; gap: 6px;
      mat-icon { color: var(--nt-primary-dark); font-size: 20px; width: 20px; height: 20px; } }
    .status-pill { padding: 2px 10px; border-radius: 12px; font-size: 0.7rem; font-weight: 700; letter-spacing: 0.03em; }
    .pill-pending { background: #fef3c7; color: #92400e; }
    .pill-approved { background: #d1fae5; color: #065f46; }
    .pill-rejected { background: #fee2e2; color: #991b1b; }
    .req-note {
      display: flex; gap: 8px; padding: 10px 12px; border-radius: 8px;
      background: var(--nt-surface-2); font-size: 0.9rem;
      mat-icon { color: var(--nt-primary-dark); font-size: 18px; width: 18px; height: 18px; margin-top: 2px; }
    }
    .proposal { display: flex; flex-direction: column; gap: 6px; padding: 8px 0; }
    .prop-item { display: flex; align-items: center; gap: 8px; font-size: 0.9rem; }
    .p-label { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--nt-text-muted); font-weight: 600; }
    .p-value { color: var(--nt-text); }
    .color-chip { width: 20px; height: 20px; border-radius: 4px; border: 1px solid var(--nt-border); }
    .fields-list { display: flex; flex-direction: column; gap: 2px; }
    .fields-list ul { list-style: none; margin: 4px 0 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .fields-list li { font-size: 0.88rem; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
    .type-chip { padding: 1px 8px; border-radius: 8px; background: var(--nt-primary-soft); color: var(--nt-primary-dark);
      font-size: 0.7rem; font-weight: 700; text-transform: uppercase; }
    .req-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
    .reject-note { display: block; }
    .review-info { display: inline-flex; align-items: center; gap: 6px;
      mat-icon { font-size: 16px; width: 16px; height: 16px; } }
    .empty { text-align: center; padding: 32px;
      mat-icon { font-size: 36px; width: 36px; height: 36px; color: var(--nt-text-muted); }
      p { color: var(--nt-text-muted); margin: 8px 0 0; }
    }
  `]
})
export class CompanyPdfChangeRequestsComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  requests = signal<PdfChangeRequest[]>([]);
  filter: PdfChangeRequestStatus | '' = 'PENDING';
  rejectingId = signal<number | null>(null);
  rejectNote = '';

  ngOnInit() { this.load(); }

  load() {
    const status = this.filter || undefined;
    this.api.companyListPdfChangeRequests(status).subscribe(list => this.requests.set(list));
  }

  approve(r: PdfChangeRequest) {
    this.api.companyApprovePdfChangeRequest(r.id, null).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('pdfChangeRequests.approvedOk'), 'OK', { duration: 2500 });
        this.load();
      },
      error: () => this.snack.open(this.translate.instant('pdfChangeRequests.err'), 'OK', { duration: 3000 })
    });
  }

  startReject(id: number) { this.rejectingId.set(id); this.rejectNote = ''; }
  cancelReject() { this.rejectingId.set(null); this.rejectNote = ''; }

  reject(r: PdfChangeRequest) {
    this.api.companyRejectPdfChangeRequest(r.id, this.rejectNote || null).subscribe({
      next: () => {
        this.rejectingId.set(null);
        this.rejectNote = '';
        this.snack.open(this.translate.instant('pdfChangeRequests.rejectedOk'), 'OK', { duration: 2500 });
        this.load();
      },
      error: () => this.snack.open(this.translate.instant('pdfChangeRequests.err'), 'OK', { duration: 3000 })
    });
  }
}
