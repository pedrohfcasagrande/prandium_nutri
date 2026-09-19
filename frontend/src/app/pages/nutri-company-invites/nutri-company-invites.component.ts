import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { CompanyInvite } from '../../models/models';

@Component({
  selector: 'app-nutri-company-invites',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutriCompanyInvites.title' | translate }}</h1>
          <p>{{ 'nutriCompanyInvites.subtitle' | translate }}</p>
        </div>
      </div>

      @if (!invites().length) {
        <div class="empty card-surface">
          <mat-icon>mail_outline</mat-icon>
          <p>{{ 'nutriCompanyInvites.empty' | translate }}</p>
        </div>
      } @else {
        <div class="invite-list">
          @for (inv of invites(); track inv.linkId) {
            <mat-card class="invite-card">
              <div class="invite-body">
                <div class="avatar">
                  <mat-icon>corporate_fare</mat-icon>
                </div>
                <div>
                  <div class="invite-text">
                    <strong>{{ inv.companyName }}</strong>
                    {{ 'nutriCompanyInvites.wantsYou' | translate }}
                  </div>
                  <div class="text-muted small">
                    {{ 'nutriCompanyInvites.invitedOn' | translate }}
                    {{ inv.invitedAt | date:'short' }}
                  </div>
                  <div class="text-muted small">
                    {{ 'nutriCompanyInvites.expiresOn' | translate }} {{ inv.expiresAt | date:'short' }}
                  </div>
                </div>
              </div>
              <div class="invite-actions">
                <button mat-stroked-button color="warn" (click)="reject(inv)">
                  <mat-icon>close</mat-icon>
                  {{ 'nutriCompanyInvites.reject' | translate }}
                </button>
                <button mat-flat-button color="primary" (click)="accept(inv)">
                  <mat-icon>check</mat-icon>
                  {{ 'nutriCompanyInvites.accept' | translate }}
                </button>
              </div>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .invite-list { display: flex; flex-direction: column; gap: 12px; }
    .invite-card {
      padding: 16px 20px !important;
      display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap;
      align-items: center;
      border-left: 4px solid var(--nt-accent) !important;
    }
    .invite-body { display: flex; align-items: center; gap: 14px; }
    .avatar {
      width: 48px; height: 48px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-accent), #d97706);
      color: #fff; display: flex; align-items: center; justify-content: center;
    }
    .invite-text { font-size: 1.05rem; }
    .small { font-size: 0.85rem; }
    .invite-actions { display: flex; gap: 8px; }
  `]
})
export class NutriCompanyInvitesComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  invites = signal<CompanyInvite[]>([]);

  ngOnInit(): void { this.load(); }

  load() { this.api.nutriCompanyInvitesPending().subscribe(i => this.invites.set(i)); }

  accept(inv: CompanyInvite) {
    this.api.nutriAcceptCompany(inv.token).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriCompanyInvites.accepted', { name: inv.companyName }),
                        'OK', { duration: 2500 });
        this.load();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('nutriCompanyInvites.acceptFail'),
                                    'OK', { duration: 3000 })
    });
  }

  reject(inv: CompanyInvite) {
    this.api.nutriRejectCompany(inv.token).subscribe({
      next: () => { this.snack.open(this.translate.instant('nutriCompanyInvites.rejected'), 'OK', { duration: 2000 }); this.load(); },
      error: () => this.snack.open(this.translate.instant('nutriCompanyInvites.rejectFail'), 'OK', { duration: 3000 })
    });
  }
}
