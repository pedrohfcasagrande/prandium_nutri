import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { MenuPlan } from '../../models/models';
import { MenuFormDialogComponent } from './menu-form-dialog.component';

@Component({
  selector: 'app-menus',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatDividerModule,
    MatChipsModule, MatTooltipModule, MatSnackBarModule, MatDialogModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'menus.title' | translate }}</h1>
          <p>{{ 'menus.subtitle' | translate }}</p>
        </div>
        <button mat-flat-button color="primary" class="new-btn" (click)="openDialog(null)">
          <mat-icon>add</mat-icon>
          {{ 'menus.newTitle' | translate }}
        </button>
      </div>

      @if (activePlan(); as ap) {
        <div class="page-header history-header">
          <div>
            <h2>
              <mat-icon class="header-icon">check_circle</mat-icon>
              {{ 'menus.activePlanTitle' | translate }}
            </h2>
          </div>
        </div>
        <mat-card class="hero-card">
          <div class="hero-badge">
            <mat-icon>bolt</mat-icon>
            <span>{{ 'menus.activeNow' | translate }}</span>
          </div>
          <div class="hero-body">
            <div class="hero-title">
              <div class="hero-name">{{ ap.name }}</div>
              <div class="plan-meta">
                <mat-chip class="source-chip" [class.ai-chip]="ap.source === 'AI'">
                  {{ ap.source === 'AI' ? ('menus.source.ai' | translate) : ('menus.source.manual' | translate) }}
                </mat-chip>
                @if (ap.startDate || ap.endDate) {
                  <span class="text-muted">{{ ap.startDate || '?' }} — {{ ap.endDate || '?' }}</span>
                }
              </div>
            </div>
            <div class="hero-preview">
              <pre>{{ heroPreview(ap.content) }}</pre>
              @if (isTruncated(ap.content)) {
                <div class="fade-out"></div>
              }
            </div>
            <div class="hero-actions">
              <button mat-flat-button color="primary" (click)="toggleExpand(ap.id!)">
                <mat-icon>{{ expanded() === ap.id ? 'expand_less' : 'expand_more' }}</mat-icon>
                {{ (expanded() === ap.id ? 'menus.hideFull' : 'menus.viewFull') | translate }}
              </button>
              <button mat-stroked-button (click)="openDialog(ap)">
                <mat-icon>edit</mat-icon>
                {{ 'menus.edit' | translate }}
              </button>
              <button mat-stroked-button (click)="toggle(ap, false)"
                      [matTooltip]="'menus.deactivate' | translate">
                <mat-icon>toggle_off</mat-icon>
                {{ 'menus.deactivate' | translate }}
              </button>
            </div>
            @if (expanded() === ap.id) {
              <mat-divider></mat-divider>
              <div class="plan-content"><pre>{{ ap.content }}</pre></div>
            }
          </div>
        </mat-card>
      }

      @if (pendingPlans().length) {
        <div class="page-header history-header">
          <div><h2>
            <mat-icon class="header-icon" style="color:#f59e0b">assignment</mat-icon>
            {{ 'menus.pendingTitle' | translate }}
          </h2></div>
        </div>
        <div class="plan-list">
          @for (p of pendingPlans(); track p.id) {
            <mat-card class="plan-card pending-card">
              <div class="plan-head">
                <div class="plan-title">
                  <mat-icon style="color:#f59e0b">schedule</mat-icon>
                  <div>
                    <div class="plan-name">{{ p.name }}</div>
                    <div class="plan-meta">
                      <mat-chip class="pending-chip">{{ 'menus.pendingChip' | translate }}</mat-chip>
                      @if (p.submittedAt) {
                        <span class="text-muted small">{{ 'menus.submittedAt' | translate }}: {{ p.submittedAt | date:'short' }}</span>
                      }
                    </div>
                  </div>
                </div>
                <button mat-icon-button (click)="toggleExpand(p.id!)">
                  <mat-icon>{{ expanded() === p.id ? 'expand_less' : 'expand_more' }}</mat-icon>
                </button>
              </div>
              @if (expanded() === p.id) {
                <mat-divider></mat-divider>
                <div class="plan-content"><pre>{{ p.content }}</pre></div>
              }
            </mat-card>
          }
        </div>
      }

      @if (rejectedPlans().length) {
        <div class="page-header history-header">
          <div><h2>
            <mat-icon class="header-icon" style="color:#ef4444">cancel</mat-icon>
            {{ 'menus.rejectedTitle' | translate }}
          </h2></div>
        </div>
        <div class="plan-list">
          @for (p of rejectedPlans(); track p.id) {
            <mat-card class="plan-card rejected-card">
              <div class="plan-head">
                <div class="plan-title">
                  <mat-icon style="color:#ef4444">cancel</mat-icon>
                  <div>
                    <div class="plan-name">{{ p.name }}</div>
                    @if (p.reviewNote) {
                      <div class="reason-box">
                        <mat-icon>info</mat-icon>
                        <span>{{ p.reviewNote }}</span>
                      </div>
                    }
                  </div>
                </div>
                <div class="plan-actions">
                  @if (hasActiveNutri()) {
                    <button mat-stroked-button color="primary" (click)="submitForApproval(p)">
                      <mat-icon>send</mat-icon> {{ 'menus.resubmit' | translate }}
                    </button>
                  }
                  <button mat-icon-button (click)="openDialog(p)"
                          [matTooltip]="'menus.edit' | translate">
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button mat-icon-button (click)="toggleExpand(p.id!)">
                    <mat-icon>{{ expanded() === p.id ? 'expand_less' : 'expand_more' }}</mat-icon>
                  </button>
                  <button mat-icon-button color="warn" (click)="remove(p)">
                    <mat-icon>delete_outline</mat-icon>
                  </button>
                </div>
              </div>
              @if (expanded() === p.id) {
                <mat-divider></mat-divider>
                <div class="plan-content"><pre>{{ p.content }}</pre></div>
              }
            </mat-card>
          }
        </div>
      }

      <div class="page-header history-header">
        <div><h2>{{ 'menus.otherPlansTitle' | translate }}</h2></div>
      </div>

      @if (!otherPlans().length) {
        <div class="empty card-surface">
          <mat-icon>restaurant_menu</mat-icon>
          <p>{{ activePlan() ? ('menus.emptyOther' | translate) : ('menus.empty' | translate) }}</p>
        </div>
      } @else {
        <div class="plan-list">
          @for (p of otherPlans(); track p.id) {
            <mat-card class="plan-card">
              <div class="plan-head">
                <div class="plan-title">
                  <mat-icon>restaurant_menu</mat-icon>
                  <div>
                    <div class="plan-name">{{ p.name }}</div>
                    <div class="plan-meta">
                      <mat-chip class="draft-chip">{{ 'menus.draft' | translate }}</mat-chip>
                      <mat-chip class="source-chip" [class.ai-chip]="p.source === 'AI'">
                        {{ p.source === 'AI' ? ('menus.source.ai' | translate) : ('menus.source.manual' | translate) }}
                      </mat-chip>
                      @if (p.startDate || p.endDate) {
                        <span class="text-muted">
                          {{ p.startDate || '?' }} — {{ p.endDate || '?' }}
                        </span>
                      }
                      @if (p.createdAt) {
                        <span class="text-muted">·  {{ p.createdAt | date:'shortDate' }}</span>
                      }
                    </div>
                  </div>
                </div>
                <div class="plan-actions">
                  @if (hasActiveNutri()) {
                    <button mat-stroked-button color="primary" (click)="submitForApproval(p)"
                            [matTooltip]="'menus.submitTooltip' | translate">
                      <mat-icon>send</mat-icon>
                      {{ 'menus.submit' | translate }}
                    </button>
                  } @else {
                    <button mat-stroked-button color="primary" (click)="toggle(p, true)"
                            [matTooltip]="'menus.activate' | translate">
                      <mat-icon>check_circle</mat-icon>
                      {{ 'menus.activate' | translate }}
                    </button>
                  }
                  <button mat-icon-button (click)="openDialog(p)"
                          [matTooltip]="'menus.edit' | translate">
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button mat-icon-button (click)="toggleExpand(p.id!)">
                    <mat-icon>{{ expanded() === p.id ? 'expand_less' : 'expand_more' }}</mat-icon>
                  </button>
                  <button mat-icon-button color="warn" (click)="remove(p)">
                    <mat-icon>delete_outline</mat-icon>
                  </button>
                </div>
              </div>
              @if (expanded() === p.id) {
                <mat-divider></mat-divider>
                <div class="plan-content"><pre>{{ p.content }}</pre></div>
              }
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .new-btn { height: 44px; font-weight: 600; }
    mat-card-title {
      display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
    }

    .history-header h2 {
      font-size: 1.35rem; margin: 0; font-weight: 600;
      display: flex; align-items: center; gap: 8px;
      .header-icon { color: var(--nt-primary); }
    }

    .hero-card {
      padding: 0 !important; overflow: hidden;
      border: 2px solid var(--nt-primary);
      box-shadow: 0 8px 24px rgba(13, 148, 136, 0.25);
      position: relative;
    }
    .hero-badge {
      display: inline-flex; align-items: center; gap: 6px;
      position: absolute; top: 14px; right: 14px;
      background: linear-gradient(135deg, #10b981, var(--nt-primary));
      color: #fff; padding: 6px 12px; border-radius: 999px;
      font-size: 0.75rem; font-weight: 700; text-transform: uppercase;
      letter-spacing: 0.05em;
      box-shadow: 0 2px 6px rgba(13, 148, 136, 0.4);
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }
    .hero-body { padding: 24px 28px; display: flex; flex-direction: column; gap: 16px; }
    .hero-name {
      font-size: 1.5rem; font-weight: 700; letter-spacing: -0.02em;
      color: var(--nt-primary-dark);
    }
    .hero-preview {
      position: relative; max-height: 180px; overflow: hidden;
      padding: 12px 16px; background: var(--nt-surface-2); border-radius: var(--nt-radius);
      pre { margin: 0; white-space: pre-wrap; font-family: inherit; line-height: 1.6; font-size: 0.9rem; }
    }
    .fade-out {
      position: absolute; bottom: 0; left: 0; right: 0; height: 60px;
      background: linear-gradient(transparent, var(--nt-surface-2));
      pointer-events: none;
    }
    .hero-actions { display: flex; gap: 10px; flex-wrap: wrap; }

    .plan-list { display: flex; flex-direction: column; gap: 12px; }
    .plan-card { padding: 16px 20px !important; display: flex; flex-direction: column; gap: 12px; }
    .plan-active { border-left: 4px solid var(--nt-primary) !important; }

    .plan-head {
      display: flex; justify-content: space-between; align-items: center; gap: 12px;
      flex-wrap: wrap;
    }
    .plan-title { display: flex; gap: 14px; align-items: center; }
    .plan-title mat-icon { font-size: 28px; width: 28px; height: 28px; color: var(--nt-text-muted); }
    .plan-title mat-icon.icon-active { color: var(--nt-primary); }
    .plan-name { font-weight: 600; font-size: 1.05rem; }
    .plan-meta {
      display: flex; align-items: center; gap: 8px; margin-top: 4px;
      font-size: 0.85rem;
    }
    .source-chip { font-weight: 600; font-size: 0.75rem !important; }
    .ai-chip {
      background: linear-gradient(135deg, #8b5cf6, #6366f1) !important;
      color: #fff !important;
    }
    .draft-chip {
      background: var(--nt-surface-2) !important;
      color: var(--nt-text-muted) !important;
      font-weight: 600; font-size: 0.75rem !important;
    }
    .pending-chip {
      background: #fef3c7 !important; color: #92400e !important;
      font-weight: 600; font-size: 0.75rem !important;
    }
    .pending-card { border-left: 4px solid #f59e0b !important; }
    .rejected-card { border-left: 4px solid #ef4444 !important; }
    .reason-box {
      display: flex; align-items: flex-start; gap: 6px;
      padding: 8px 12px; margin-top: 6px;
      background: #fee2e2; color: #991b1b; border-radius: var(--nt-radius);
      font-size: 0.85rem;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .small { font-size: 0.85rem; }
    .plan-actions { display: flex; align-items: center; gap: 4px; }

    .plan-content pre {
      white-space: pre-wrap; font-family: inherit; line-height: 1.6;
      margin: 0; padding-top: 4px;
    }
  `]
})
export class MenusComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);

  plans = signal<MenuPlan[]>([]);
  expanded = signal<number | null>(null);

  activePlan = computed(() => this.plans().find(p => p.active) ?? null);
  otherPlans = computed(() => this.plans().filter(p => !p.active && p.status !== 'PENDING'));
  pendingPlans = computed(() => this.plans().filter(p => p.status === 'PENDING'));
  rejectedPlans = computed(() => this.plans().filter(p => p.status === 'REJECTED'));
  hasActiveNutri = signal(false);

  heroPreview(content: string): string {
    if (!content) return '';
    const lines = content.split('\n').filter(l => l.trim());
    return lines.slice(0, 6).join('\n');
  }

  isTruncated(content: string): boolean {
    if (!content) return false;
    return content.split('\n').filter(l => l.trim()).length > 6;
  }

  ngOnInit(): void {
    this.load();
    this.api.myNutritionists().subscribe({
      next: list => this.hasActiveNutri.set(list.length > 0),
      error: () => this.hasActiveNutri.set(false)
    });
    const pending = sessionStorage.getItem('prandiumnutri.pendingMenu');
    if (pending) {
      sessionStorage.removeItem('prandiumnutri.pendingMenu');
      try {
        const parsed: MenuPlan = JSON.parse(pending);
        this.openDialog(parsed);
      } catch { /* ignore */ }
    }
  }

  submitForApproval(p: MenuPlan) {
    if (!p.id) return;
    this.api.submitMenuPlan(p.id).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('menus.submitted'), 'OK', { duration: 3000 });
        this.load();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('menus.submitFail'), 'OK', { duration: 4000 })
    });
  }

  load() { this.api.listMenuPlans().subscribe(p => this.plans.set(p)); }

  openDialog(prefill: MenuPlan | null) {
    const ref = this.dialog.open(MenuFormDialogComponent, {
      data: prefill,
      autoFocus: false,
      restoreFocus: true,
      maxWidth: '800px',
      width: '100%'
    });
    ref.afterClosed().subscribe((result: MenuPlan | null | undefined) => {
      if (!result) return;
      this.api.saveMenuPlan(result).subscribe({
        next: () => {
          this.snack.open(this.translate.instant('menus.saved'), 'OK', { duration: 2000 });
          this.load();
        },
        error: () => this.snack.open(this.translate.instant('menus.saveFail'), 'OK', { duration: 3000 })
      });
    });
  }

  toggle(p: MenuPlan, active: boolean) {
    if (!p.id) return;
    this.api.setMenuPlanActive(p.id, active).subscribe(() => this.load());
  }

  remove(p: MenuPlan) {
    if (!p.id) return;
    if (!confirm(this.translate.instant('menus.removeConfirm', { name: p.name }))) return;
    this.api.deleteMenuPlan(p.id).subscribe(() => this.load());
  }

  toggleExpand(id: number) {
    this.expanded.set(this.expanded() === id ? null : id);
  }
}
