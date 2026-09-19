import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { ImpersonationService } from '../../services/impersonation.service';
import { CompanyNutriLink } from '../../models/models';
import { RevokeNutriDialogComponent, RevokeDialogResult } from './revoke-nutri-dialog.component';

@Component({
  selector: 'app-company-nutritionists',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatChipsModule, MatTooltipModule,
    MatSnackBarModule, MatDialogModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'company.nutris.title' | translate }}</h1>
          <p>{{ 'company.nutris.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-card class="invite-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>person_add</mat-icon>
            {{ 'company.nutris.inviteTitle' | translate }}
          </mat-card-title>
          <mat-card-subtitle>{{ 'company.nutris.inviteSubtitle' | translate }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="invite-row">
            <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
              <mat-label>{{ 'company.nutris.emailLabel' | translate }}</mat-label>
              <input matInput type="email" [(ngModel)]="email" (keydown.enter)="invite()">
              <mat-icon matPrefix>mail</mat-icon>
            </mat-form-field>
            <button mat-flat-button color="primary" (click)="invite()" [disabled]="!email">
              <mat-icon>send</mat-icon> {{ 'company.nutris.inviteBtn' | translate }}
            </button>
          </div>
        </mat-card-content>
      </mat-card>

      <div class="page-header history-header">
        <h2>{{ 'company.nutris.listTitle' | translate }} ({{ nutris().length }})</h2>
      </div>

      @if (!nutris().length) {
        <div class="empty card-surface">
          <mat-icon>medical_services</mat-icon>
          <p>{{ 'company.nutris.empty' | translate }}</p>
        </div>
      } @else {
        <div class="list">
          @for (n of nutris(); track n.id) {
            <mat-card class="nutri-card" [class.pending]="n.status === 'PENDING'"
                                          [class.active]="n.status === 'ACTIVE'">
              <div class="avatar" [class.avatar-pending]="n.status === 'PENDING'">
                {{ (n.nutritionistName || '?')[0].toUpperCase() }}
              </div>
              <div class="nutri-info">
                <div class="nutri-name">{{ n.nutritionistName }}</div>
                <div class="nutri-email">{{ n.nutritionistEmail }}</div>
                <div class="nutri-meta">
                  <span class="status-chip" [class]="'chip-' + n.status.toLowerCase()">
                    {{ ('nutri.status.' + n.status) | translate }}
                  </span>
                  @if (n.status === 'ACTIVE') {
                    <span class="text-muted small">
                      {{ n.clientsCount }} {{ 'company.nutris.clientsWord' | translate }}
                    </span>
                  }
                </div>
              </div>
              <div class="nutri-actions">
                @if (n.status === 'ACTIVE' && n.nutritionistId) {
                  <button mat-flat-button color="primary" (click)="manage(n)"
                          [matTooltip]="'company.nutris.manageTooltip' | translate">
                    <mat-icon>manage_accounts</mat-icon>
                    {{ 'company.nutris.manage' | translate }}
                  </button>
                }
                @if (canRevoke(n.status)) {
                  <button mat-icon-button color="warn" (click)="openRevoke(n)"
                          [matTooltip]="'company.nutris.revokeTooltip' | translate">
                    <mat-icon>link_off</mat-icon>
                  </button>
                } @else {
                  <button mat-stroked-button color="primary" (click)="reinvite(n)"
                          [matTooltip]="'company.nutris.reinviteTooltip' | translate">
                    <mat-icon>send</mat-icon>
                    {{ 'company.nutris.reinvite' | translate }}
                  </button>
                }
              </div>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .invite-card mat-card-title { display: flex; align-items: center; gap: 8px; }
    .invite-card mat-icon { color: var(--nt-primary); }
    .invite-row { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .grow { flex: 1 1 260px; }

    .history-header h2 { font-size: 1.35rem; margin: 0; font-weight: 600; }

    .list { display: flex; flex-direction: column; gap: 12px; }
    .nutri-card {
      padding: 16px 20px !important;
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 16px;
    }
    .nutri-card.pending { border-left: 4px solid var(--nt-accent) !important; }
    .nutri-card.active { border-left: 4px solid var(--nt-primary) !important; }

    .avatar {
      width: 48px; height: 48px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff; display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 1.1rem; flex-shrink: 0;
    }
    .avatar.avatar-pending { background: linear-gradient(135deg, var(--nt-accent), #d97706); }
    .nutri-info { min-width: 0; }
    .nutri-name { font-weight: 600; font-size: 1.05rem; }
    .nutri-email { color: var(--nt-text-muted); font-size: 0.9rem; margin: 2px 0 6px;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .nutri-meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .small { font-size: 0.8rem; }

    .status-chip {
      display: inline-flex; align-items: center;
      padding: 4px 12px; border-radius: 999px;
      font-weight: 600; font-size: 0.72rem;
      text-transform: uppercase; letter-spacing: 0.03em;
    }
    .chip-pending { background: #fef3c7; color: #92400e; }
    .chip-active { background: #d1fae5; color: #065f46; }
    .chip-rejected, .chip-revoked_by_nutri, .chip-revoked_by_client, .chip-expired {
      background: var(--nt-surface-2); color: var(--nt-text-muted);
    }

    .nutri-actions { display: flex; gap: 8px; align-items: center; justify-self: end; }

    @media (max-width: 640px) {
      .nutri-card { grid-template-columns: auto 1fr; }
      .nutri-actions { grid-column: 1 / -1; justify-self: stretch; }
    }
  `]
})
export class CompanyNutritionistsComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private impersonation = inject(ImpersonationService);
  private router = inject(Router);

  nutris = signal<CompanyNutriLink[]>([]);
  email = '';

  ngOnInit(): void { this.load(); }

  load() { this.api.companyNutritionists().subscribe(n => this.nutris.set(n)); }

  invite() {
    if (!this.email) return;
    this.api.companyInviteNutri(this.email).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('company.nutris.inviteSent'), 'OK', { duration: 2500 });
        this.email = ''; this.load();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('company.nutris.inviteFail'),
                                    'OK', { duration: 3000 })
    });
  }

  manage(n: CompanyNutriLink) {
    if (!n.nutritionistId) return;
    this.impersonation.start({
      id: n.nutritionistId,
      name: n.nutritionistName,
      email: n.nutritionistEmail
    });
    this.router.navigate(['/nutritionist/clients']);
  }

  revoke(n: CompanyNutriLink) {
    if (!confirm(this.translate.instant('company.nutris.revokeConfirm', { name: n.nutritionistName }))) return;
    this.api.companyRevokeNutri(n.id).subscribe(() => this.load());
  }

  private dialog = inject(MatDialog);

  openRevoke(n: CompanyNutriLink) {
    const otherNutris = this.nutris()
      .filter(x => x.status === 'ACTIVE' && x.nutritionistId && x.nutritionistId !== n.nutritionistId)
      .map(x => ({ id: x.nutritionistId!, name: x.nutritionistName }));

    const ref = this.dialog.open(RevokeNutriDialogComponent, {
      data: {
        nutriName: n.nutritionistName,
        clientsCount: n.clientsCount ?? 0,
        otherNutris
      },
      autoFocus: false, restoreFocus: true
    });
    ref.afterClosed().subscribe((result: RevokeDialogResult | null | undefined) => {
      if (!result) return;
      this.api.companyRevokeNutriWithAction(n.id, result.action, result.targetNutriId).subscribe({
        next: () => {
          this.snack.open(this.translate.instant('company.nutris.revokedOk'), 'OK', { duration: 2500 });
          this.load();
        },
        error: err => this.snack.open(err?.error?.error ?? 'Erro', 'OK', { duration: 3000 })
      });
    });
  }

  reinvite(n: CompanyNutriLink) {
    this.api.companyInviteNutri(n.nutritionistEmail).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('company.nutris.reinviteSent'), 'OK', { duration: 2500 });
        this.load();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('company.nutris.inviteFail'),
                                    'OK', { duration: 3000 })
    });
  }

  canRevoke(status: string): boolean {
    return status === 'ACTIVE' || status === 'PENDING';
  }
}
