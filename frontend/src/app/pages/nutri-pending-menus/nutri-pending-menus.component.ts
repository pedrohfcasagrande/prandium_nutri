import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { NotificationPollService } from '../../services/notification-poll.service';
import { MenuPlan } from '../../models/models';

@Component({
  selector: 'app-nutri-pending-menus',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule, MatChipsModule, MatDividerModule,
    MatFormFieldModule, MatInputModule, MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'pendingMenus.title' | translate }}</h1>
          <p>{{ 'pendingMenus.subtitle' | translate }}</p>
        </div>
      </div>

      @if (pending().length === 0) {
        <div class="empty card-surface">
          <mat-icon>inbox</mat-icon>
          <p>{{ 'pendingMenus.empty' | translate }}</p>
        </div>
      } @else {
        <div class="list">
          @for (p of pending(); track p.id) {
            <mat-card class="pending-card">
              <div class="head">
                <mat-icon class="head-icon">assignment</mat-icon>
                <div class="head-info">
                  <div class="name">{{ p.name }}</div>
                  <div class="owner text-muted">
                    {{ 'pendingMenus.from' | translate }}: <strong>{{ p.ownerName }}</strong>
                    <span class="small">· {{ p.ownerEmail }}</span>
                  </div>
                  <div class="submitted text-muted small">
                    {{ 'pendingMenus.submittedAt' | translate }}: {{ p.submittedAt | date:'medium' }}
                  </div>
                </div>
                <mat-chip class="pending-chip">{{ 'pendingMenus.pending' | translate }}</mat-chip>
              </div>

              <mat-divider></mat-divider>

              <div class="content-wrap"><pre>{{ p.content }}</pre></div>

              <div class="actions">
                @if (rejectingId() === p.id) {
                  <mat-form-field appearance="outline" class="reason-field" subscriptSizing="dynamic">
                    <mat-label>{{ 'pendingMenus.reasonLabel' | translate }}</mat-label>
                    <textarea matInput rows="2" [(ngModel)]="rejectReason"
                              [placeholder]="'pendingMenus.reasonHint' | translate"></textarea>
                  </mat-form-field>
                  <button mat-flat-button color="warn" (click)="confirmReject(p)"
                          [disabled]="!rejectReason.trim()">
                    <mat-icon>send</mat-icon> {{ 'pendingMenus.confirmReject' | translate }}
                  </button>
                  <button mat-stroked-button (click)="cancelReject()">
                    {{ 'app.actions.cancel' | translate }}
                  </button>
                } @else {
                  <button mat-flat-button color="primary" (click)="approve(p)">
                    <mat-icon>check_circle</mat-icon> {{ 'pendingMenus.approve' | translate }}
                  </button>
                  <button mat-stroked-button color="warn" (click)="startReject(p)">
                    <mat-icon>cancel</mat-icon> {{ 'pendingMenus.reject' | translate }}
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
    .list { display: flex; flex-direction: column; gap: 16px; }
    .pending-card { padding: 20px !important; display: flex; flex-direction: column; gap: 14px;
      border-left: 4px solid #f59e0b !important; }
    .head { display: flex; gap: 14px; align-items: flex-start; }
    .head-icon { font-size: 32px; width: 32px; height: 32px; color: #f59e0b; }
    .head-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
    .name { font-size: 1.15rem; font-weight: 700; }
    .owner { font-size: 0.9rem; }
    .small { font-size: 0.8rem; }
    .pending-chip { background: #fef3c7 !important; color: #92400e !important; font-weight: 600; }
    .content-wrap {
      max-height: 300px; overflow-y: auto;
      padding: 12px 14px; background: var(--nt-surface-2); border-radius: var(--nt-radius);
      pre { margin: 0; white-space: pre-wrap; font-family: inherit; line-height: 1.6; font-size: 0.88rem; }
    }
    .actions { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
    .reason-field { flex: 1; min-width: 220px; }
  `]
})
export class NutriPendingMenusComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private polls = inject(NotificationPollService);

  pending = signal<MenuPlan[]>([]);
  rejectingId = signal<number | null>(null);
  rejectReason = '';

  ngOnInit() { this.load(); }

  load() {
    this.api.nutriPendingMenus().subscribe(list => this.pending.set(list));
  }

  approve(p: MenuPlan) {
    if (!p.id) return;
    this.api.approveMenu(p.id).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('pendingMenus.approved'), 'OK', { duration: 2500 });
        this.load();
        this.polls.refresh();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('pendingMenus.error'), 'OK', { duration: 3000 })
    });
  }

  startReject(p: MenuPlan) {
    this.rejectingId.set(p.id!);
    this.rejectReason = '';
  }

  cancelReject() {
    this.rejectingId.set(null);
    this.rejectReason = '';
  }

  confirmReject(p: MenuPlan) {
    if (!p.id || !this.rejectReason.trim()) return;
    this.api.rejectMenu(p.id, this.rejectReason.trim()).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('pendingMenus.rejected'), 'OK', { duration: 2500 });
        this.cancelReject();
        this.load();
        this.polls.refresh();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('pendingMenus.error'), 'OK', { duration: 3000 })
    });
  }
}
