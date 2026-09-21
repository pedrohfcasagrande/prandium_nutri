import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PendingInvite } from '../../models/models';

@Component({
  selector: 'app-invitations',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'invitations.title' | translate }}</h1>
          <p>{{ 'invitations.subtitle' | translate }}</p>
        </div>
      </div>

      @if (!invites().length) {
        <div class="empty card-surface">
          <mat-icon>mail_outline</mat-icon>
          <p>{{ 'invitations.empty' | translate }}</p>
        </div>
      } @else {
        <div class="invite-list">
          @for (inv of invites(); track inv.linkId; let i = $index) {
            <mat-card class="invite-card stagger-item" [style.--i]="i">
              <div class="avatar">
                <mat-icon>medical_services</mat-icon>
              </div>
              <div class="info">
                <div class="invite-text">
                  <strong>{{ inv.nutritionistName }}</strong>
                  {{ 'invitations.wantsToFollow' | translate }}
                </div>
                <div class="text-muted small">
                  {{ inv.nutritionistEmail }} · {{ 'invitations.invitedOn' | translate }}
                  {{ inv.invitedAt | date:'short' }}
                </div>
                <div class="text-muted small">
                  {{ 'invitations.expiresOn' | translate }} {{ inv.expiresAt | date:'short' }}
                </div>
              </div>
              <div class="invite-actions">
                <button mat-stroked-button color="warn" (click)="reject(inv)">
                  <mat-icon>close</mat-icon>
                  {{ 'invitations.reject' | translate }}
                </button>
                <button mat-flat-button color="primary" (click)="accept(inv)">
                  <mat-icon>check</mat-icon>
                  {{ 'invitations.accept' | translate }}
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
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 16px;
      border-left: 4px solid var(--nt-accent) !important;
    }
    .avatar {
      width: 48px; height: 48px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-accent), #d97706);
      color: #fff; display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .info { min-width: 0; }
    .invite-text { font-size: 1.05rem; margin-bottom: 4px; }
    .small { font-size: 0.85rem; }
    .invite-actions { display: flex; gap: 8px; justify-self: end; flex-wrap: wrap; }

    @media (max-width: 720px) {
      .invite-card { grid-template-columns: auto 1fr; }
      .invite-actions { grid-column: 1 / -1; justify-self: stretch; justify-content: flex-end; }
    }
  `]
})
export class InvitationsComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  invites = signal<PendingInvite[]>([]);

  ngOnInit(): void { this.load(); }

  load() { this.api.pendingInvites().subscribe(i => this.invites.set(i)); }

  accept(inv: PendingInvite) {
    this.api.acceptInvite(inv.token).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('invitations.accepted', { name: inv.nutritionistName }),
                        'OK', { duration: 2500 });
        this.load();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('invitations.acceptFail'),
                                    'OK', { duration: 3000 })
    });
  }

  reject(inv: PendingInvite) {
    this.api.rejectInvite(inv.token).subscribe({
      next: () => { this.snack.open(this.translate.instant('invitations.rejected'), 'OK', { duration: 2000 }); this.load(); },
      error: () => this.snack.open(this.translate.instant('invitations.rejectFail'), 'OK', { duration: 3000 })
    });
  }
}
