import { Component, Inject, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { NutriPublic } from '../../models/models';

@Component({
  selector: 'app-request-nutri-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, TranslateModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>send</mat-icon>
      {{ 'findNutri.dialogTitle' | translate:{ name: data.name } }}
    </h2>
    <mat-dialog-content>
      <p class="text-muted">{{ 'findNutri.dialogHint' | translate }}</p>
      <mat-form-field appearance="outline" class="full" subscriptSizing="dynamic">
        <mat-label>{{ 'findNutri.messageLabel' | translate }}</mat-label>
        <textarea matInput rows="4" [(ngModel)]="message"
                  [placeholder]="'findNutri.messagePlaceholder' | translate"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="ref.close(undefined)">{{ 'app.actions.cancel' | translate }}</button>
      <button mat-flat-button color="primary" (click)="ref.close(message.trim() || null)">
        <mat-icon>send</mat-icon> {{ 'findNutri.sendRequest' | translate }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] { display: flex; align-items: center; gap: 8px; mat-icon { color: var(--nt-primary); } }
    .full { width: 100%; min-width: min(90vw, 480px); }
    mat-dialog-content p { margin: 0 0 12px; }
  `]
})
export class RequestNutriDialogComponent {
  message = '';
  constructor(
    public ref: MatDialogRef<RequestNutriDialogComponent, string | null | undefined>,
    @Inject(MAT_DIALOG_DATA) public data: NutriPublic
  ) {}
}

@Component({
  selector: 'app-find-nutritionist',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule, MatChipsModule,
    MatFormFieldModule, MatInputModule, MatDialogModule, MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>
            {{ 'findNutri.title' | translate }}
            @if (!canFind()) { <mat-chip class="plus-badge">PLUS</mat-chip> }
          </h1>
          <p>{{ 'findNutri.subtitle' | translate }}</p>
        </div>
      </div>

      @if (!canFind()) {
        <mat-card class="paywall">
          <mat-icon>lock</mat-icon>
          <div class="paywall-body">
            <strong>{{ 'findNutri.paywallTitle' | translate }}</strong>
            <div class="small">{{ 'findNutri.paywallSub' | translate }}</div>
          </div>
          <button mat-flat-button color="primary" (click)="goPricing()">
            <mat-icon>workspace_premium</mat-icon>
            {{ 'findNutri.upgradeCta' | translate }}
          </button>
        </mat-card>
      }

      <mat-form-field appearance="outline" class="search" subscriptSizing="dynamic">
        <mat-label>{{ 'findNutri.searchLabel' | translate }}</mat-label>
        <mat-icon matPrefix>search</mat-icon>
        <input matInput [(ngModel)]="query" [placeholder]="'findNutri.searchHint' | translate">
      </mat-form-field>

      @if (filtered().length === 0) {
        <div class="empty card-surface">
          <mat-icon>search_off</mat-icon>
          <p>{{ 'findNutri.empty' | translate }}</p>
        </div>
      } @else {
        <div class="grid">
          @for (n of filtered(); track n.id) {
            <mat-card class="nutri-card">
              <div class="head">
                <div class="avatar">{{ n.name?.[0]?.toUpperCase() }}</div>
                <div class="info">
                  <div class="name">{{ n.name }}</div>
                  <div class="link-badge" [class.private]="!n.companyName">
                    <mat-icon>{{ n.companyName ? 'corporate_fare' : 'person' }}</mat-icon>
                    {{ n.companyName ?? ('findNutri.private' | translate) }}
                  </div>
                  @if (n.specialties) {
                    <div class="specs">
                      @for (s of splitSpecialties(n.specialties); track s) {
                        <mat-chip>{{ s }}</mat-chip>
                      }
                    </div>
                  }
                </div>
              </div>
              @if (n.bio) { <p class="bio">{{ n.bio }}</p> }
              <div class="actions">
                @if (canFind()) {
                  <button mat-flat-button color="primary" (click)="open(n)">
                    <mat-icon>send</mat-icon> {{ 'findNutri.requestBtn' | translate }}
                  </button>
                } @else {
                  <button mat-flat-button color="primary" (click)="goPricing()">
                    <mat-icon>workspace_premium</mat-icon> {{ 'findNutri.upgradeCta' | translate }}
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
    .search { width: 100%; max-width: 480px; margin-bottom: 20px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; }
    .nutri-card {
      padding: 20px !important; display: flex; flex-direction: column; gap: 12px;
      border-left: 4px solid var(--nt-primary) !important;
    }
    .head { display: flex; gap: 14px; align-items: center; }
    .avatar {
      width: 54px; height: 54px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff; display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 1.3rem;
    }
    .name { font-weight: 700; font-size: 1.1rem; }
    .link-badge {
      display: inline-flex; align-items: center; gap: 4px;
      font-size: 0.75rem; font-weight: 600; margin-top: 2px;
      color: #1e40af;
      mat-icon { font-size: 14px; width: 14px; height: 14px; }
    }
    .link-badge.private { color: var(--nt-text-muted); }
    .specs { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 4px;
      mat-chip { font-size: 0.7rem !important; height: 22px !important; }
    }
    .bio { margin: 0; color: var(--nt-text); font-size: 0.9rem; line-height: 1.5; }
    .actions { display: flex; justify-content: flex-end; }

    .plus-badge {
      background: linear-gradient(135deg, #8b5cf6, #6366f1) !important;
      color: #fff !important; font-weight: 700 !important; font-size: 0.7rem !important;
      height: 22px !important; margin-left: 8px; vertical-align: middle;
    }
    .paywall {
      display: flex; align-items: center; gap: 16px; padding: 16px 20px !important;
      margin-bottom: 20px;
      background: linear-gradient(135deg, #fef3c7, transparent);
      border-left: 4px solid #f59e0b !important;
      color: #78350f;
      mat-icon { color: #b45309; font-size: 28px; width: 28px; height: 28px; }
      .paywall-body { flex: 1; }
      .small { font-size: 0.85rem; color: #78350f; }
    }
  `]
})
export class FindNutritionistComponent implements OnInit {
  private api = inject(ApiService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private router = inject(Router);

  all = signal<NutriPublic[]>([]);
  canFind = signal(true);
  query = '';

  filtered = computed(() => {
    const q = this.query.trim().toLowerCase();
    if (!q) return this.all();
    return this.all().filter(n =>
      n.name.toLowerCase().includes(q)
      || (n.bio ?? '').toLowerCase().includes(q)
      || (n.specialties ?? '').toLowerCase().includes(q)
      || (n.companyName ?? '').toLowerCase().includes(q));
  });

  ngOnInit() {
    this.api.availableNutris().subscribe(list => this.all.set(list));
    this.api.canFindNutri().subscribe({
      next: (r: { allowed: boolean }) => this.canFind.set(r.allowed),
      error: () => this.canFind.set(false)
    });
  }

  goPricing() {
    this.router.navigate(['/pricing'], { queryParams: { feature: 'findNutritionist' } });
  }

  splitSpecialties(s: string): string[] {
    return s.split(/[,;]/).map(x => x.trim()).filter(Boolean);
  }

  open(n: NutriPublic) {
    const ref = this.dialog.open(RequestNutriDialogComponent, {
      data: n, autoFocus: false, restoreFocus: true
    });
    ref.afterClosed().subscribe(message => {
      if (message === undefined) return;
      this.api.requestNutriLink(n.id, message ?? undefined).subscribe({
        next: () => this.snack.open(this.translate.instant('findNutri.sentOk'), 'OK', { duration: 3000 }),
        error: err => this.snack.open(err?.error?.error ?? this.translate.instant('findNutri.sentErr'), 'OK', { duration: 4000 })
      });
    });
  }
}
