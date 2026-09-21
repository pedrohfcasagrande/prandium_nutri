import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PracticeService } from '../../services/practice.service';
import { NotificationPollService } from '../../services/notification-poll.service';
import { ClientLink, MenuPlan } from '../../models/models';

@Component({
  selector: 'app-nutri-menu-plans',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule, MatTabsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule, MatDividerModule, MatChipsModule, MatTooltipModule,
    TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutriMenus.title' | translate }}</h1>
          <p>{{ 'nutriMenus.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-tab-group animationDuration="150ms">
        <!-- Tab 1: Pendentes de aprovacao -->
        <mat-tab [label]="'nutriMenus.tabPending' | translate">
          @if (pending().length === 0) {
            <div class="empty card-surface">
              <mat-icon>inbox</mat-icon>
              <p>{{ 'nutriMenus.pendingEmpty' | translate }}</p>
            </div>
          } @else {
            <div class="list">
              @for (p of pending(); track p.id) {
                <mat-card class="row-card">
                  <mat-icon class="head-icon">assignment</mat-icon>
                  <div class="info">
                    <div class="name">{{ p.name }}</div>
                    <div class="text-muted small">
                      {{ 'nutriMenus.from' | translate }}: <strong>{{ p.ownerName }}</strong>
                      · {{ p.ownerEmail }}
                    </div>
                    @if (p.submittedAt) {
                      <div class="text-muted small">
                        {{ 'nutriMenus.submittedAt' | translate }}: {{ p.submittedAt | date:'short' }}
                      </div>
                    }
                  </div>
                  <div class="actions">
                    <button mat-flat-button color="primary" (click)="approve(p)">
                      <mat-icon>check</mat-icon>{{ 'nutriMenus.approve' | translate }}
                    </button>
                    <button mat-stroked-button color="warn" (click)="startReject(p)">
                      <mat-icon>close</mat-icon>{{ 'nutriMenus.reject' | translate }}
                    </button>
                  </div>
                  @if (rejectingId() === p.id) {
                    <div class="reject-box">
                      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="grow">
                        <mat-label>{{ 'nutriMenus.rejectReason' | translate }}</mat-label>
                        <input matInput [(ngModel)]="rejectReason">
                      </mat-form-field>
                      <button mat-flat-button color="warn" (click)="confirmReject(p)">{{ 'nutriMenus.confirm' | translate }}</button>
                      <button mat-button (click)="cancelReject()">{{ 'nutriMenus.cancel' | translate }}</button>
                    </div>
                  }
                </mat-card>
              }
            </div>
          }
        </mat-tab>

        <!-- Tab 2: Cardapios dos clientes -->
        <mat-tab [label]="'nutriMenus.tabAuthor' | translate">
          <div class="author-tab">
            <div class="client-picker">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'nutriMenus.pickClient' | translate }}</mat-label>
                <mat-select [(ngModel)]="selectedClientId" (selectionChange)="onClientChange()">
                  @for (c of clients(); track c.id) {
                    <mat-option [value]="c.clientId">{{ c.clientName }} · {{ c.clientEmail }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <button mat-flat-button color="primary" [disabled]="!selectedClientId" (click)="newDraft()">
                <mat-icon>add</mat-icon>{{ 'nutriMenus.new' | translate }}
              </button>
            </div>

            @if (selectedClientId) {
              @if (clientMenus().length === 0 && !draft()) {
                <div class="empty card-surface">
                  <mat-icon>menu_book</mat-icon>
                  <p>{{ 'nutriMenus.clientEmpty' | translate }}</p>
                </div>
              } @else {
                <div class="list">
                  @for (m of clientMenus(); track m.id) {
                    <mat-card class="row-card">
                      <mat-icon class="head-icon" [class.active]="m.active">restaurant_menu</mat-icon>
                      <div class="info">
                        <div class="name">
                          {{ m.name }}
                          @if (m.active) {
                            <span class="chip chip-active">{{ 'nutriMenus.chipActive' | translate }}</span>
                          }
                          <span class="chip" [class]="'chip-' + (m.status ?? '').toLowerCase()">{{ ('nutriMenus.status.' + m.status) | translate }}</span>
                        </div>
                        <div class="text-muted small">
                          @if (m.startDate || m.endDate) {
                            {{ 'nutriMenus.period' | translate }}: {{ m.startDate | date:'shortDate' }} - {{ m.endDate | date:'shortDate' }} ·
                          }
                          {{ 'nutriMenus.createdAt' | translate }}: {{ m.createdAt | date:'short' }}
                        </div>
                      </div>
                      <div class="actions">
                        <button mat-stroked-button (click)="editDraft(m)">
                          <mat-icon>edit</mat-icon>{{ 'nutriMenus.edit' | translate }}
                        </button>
                        <button mat-icon-button color="warn" (click)="remove(m)"
                                [matTooltip]="'nutriMenus.delete' | translate">
                          <mat-icon>delete_outline</mat-icon>
                        </button>
                      </div>
                    </mat-card>
                  }
                </div>
              }

              @if (draft(); as d) {
                <mat-card class="editor">
                  <h3>{{ d.id ? ('nutriMenus.editing' | translate) : ('nutriMenus.newing' | translate) }}</h3>
                  <div class="editor-row">
                    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="grow">
                      <mat-label>{{ 'nutriMenus.name' | translate }}</mat-label>
                      <input matInput [(ngModel)]="d.name" required>
                    </mat-form-field>
                    <mat-form-field appearance="outline" subscriptSizing="dynamic">
                      <mat-label>{{ 'nutriMenus.startDate' | translate }}</mat-label>
                      <input matInput type="date" [(ngModel)]="d.startDate">
                    </mat-form-field>
                    <mat-form-field appearance="outline" subscriptSizing="dynamic">
                      <mat-label>{{ 'nutriMenus.endDate' | translate }}</mat-label>
                      <input matInput type="date" [(ngModel)]="d.endDate">
                    </mat-form-field>
                  </div>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic" class="content">
                    <mat-label>{{ 'nutriMenus.content' | translate }}</mat-label>
                    <textarea matInput [(ngModel)]="d.content" rows="10" required
                              [placeholder]="'nutriMenus.contentHint' | translate"></textarea>
                  </mat-form-field>
                  <div class="editor-actions">
                    <label class="active-toggle">
                      <input type="checkbox" [(ngModel)]="d.active">
                      {{ 'nutriMenus.setActive' | translate }}
                    </label>
                    <span class="spacer"></span>
                    <button mat-button (click)="cancelEdit()">{{ 'nutriMenus.cancel' | translate }}</button>
                    <button mat-flat-button color="primary" (click)="save()" [disabled]="!d.name || !d.content">
                      {{ 'nutriMenus.save' | translate }}
                    </button>
                  </div>
                </mat-card>
              }
            } @else {
              <div class="empty card-surface">
                <mat-icon>person_search</mat-icon>
                <p>{{ 'nutriMenus.pickClientHint' | translate }}</p>
              </div>
            }
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .list { display: flex; flex-direction: column; gap: 12px; margin-top: 16px; }
    .row-card {
      padding: 14px 20px !important;
      display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 16px;
    }
    .head-icon { color: var(--nt-text-muted); }
    .head-icon.active { color: var(--nt-primary); }
    .info { min-width: 0; }
    .name { font-weight: 600; font-size: 1.05rem; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .small { font-size: 0.82rem; }
    .actions { display: flex; gap: 6px; }
    .chip { padding: 2px 10px; border-radius: 999px; font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.03em; }
    .chip-active { background: #dcfce7; color: #166534; }
    .chip-approved { background: var(--nt-primary-soft); color: var(--nt-primary-dark); }
    .chip-draft { background: #f1f5f9; color: #475569; }
    .chip-pending { background: #fef3c7; color: #92400e; }
    .chip-rejected { background: #fee2e2; color: #991b1b; }
    .reject-box { grid-column: 1 / -1; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding-top: 8px; border-top: 1px solid var(--nt-border); }
    .grow { flex: 1; min-width: 220px; }
    .author-tab { margin-top: 16px; display: flex; flex-direction: column; gap: 16px; }
    .client-picker { display: flex; gap: 12px; align-items: center; }
    .client-picker mat-form-field { flex: 1; max-width: 480px; }
    .editor { padding: 20px !important; display: flex; flex-direction: column; gap: 14px; }
    .editor h3 { margin: 0; }
    .editor-row { display: flex; gap: 12px; flex-wrap: wrap; }
    .content textarea { font-family: 'SF Mono', 'Menlo', monospace; font-size: 0.9rem; }
    .editor-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .active-toggle { display: inline-flex; gap: 6px; align-items: center; font-size: 0.9rem; }
    .spacer { flex: 1; }
  `]
})
export class NutriMenuPlansComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private polls = inject(NotificationPollService);
  private practiceCtx = inject(PracticeService);

  // Pending tab
  pending = signal<MenuPlan[]>([]);
  rejectingId = signal<number | null>(null);
  rejectReason = '';

  // Author tab
  clients = signal<ClientLink[]>([]);
  selectedClientId: number | null = null;
  clientMenus = signal<MenuPlan[]>([]);
  draft = signal<MenuPlan | null>(null);

  private firstLoad = true;
  constructor() {
    effect(() => {
      this.practiceCtx.activePracticeId();
      if (this.firstLoad) { this.firstLoad = false; return; }
      this.load();
      if (this.selectedClientId) this.loadClientMenus();
      this.loadClients();
    }, { allowSignalWrites: true });
  }

  ngOnInit() {
    this.load();
    this.loadClients();
  }

  load() {
    this.api.nutriPendingMenus(this.practiceCtx.activePracticeId()).subscribe(list => this.pending.set(list));
  }

  loadClients() {
    this.api.nutriListClients(this.practiceCtx.activePracticeId(), '', 'name', 0, 200)
        .subscribe(p => {
          this.clients.set(p.items.filter(c => c.status === 'ACTIVE' && c.clientId));
        });
  }

  onClientChange() {
    this.draft.set(null);
    if (this.selectedClientId) this.loadClientMenus();
    else this.clientMenus.set([]);
  }

  loadClientMenus() {
    if (!this.selectedClientId) return;
    this.api.nutriListClientMenus(this.selectedClientId, this.practiceCtx.activePracticeId())
        .subscribe(list => this.clientMenus.set(list));
  }

  newDraft() {
    this.draft.set({
      name: '', source: 'MANUAL', content: '', active: false
    });
  }

  editDraft(m: MenuPlan) {
    this.draft.set({ ...m });
  }

  cancelEdit() { this.draft.set(null); }

  save() {
    const d = this.draft();
    if (!d || !this.selectedClientId) return;
    const pid = this.practiceCtx.activePracticeId();
    const req$ = d.id
      ? this.api.nutriUpdateMenu(d.id, d, pid)
      : this.api.nutriCreateMenuForClient(this.selectedClientId, d, pid);
    req$.subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriMenus.saved'), 'OK', { duration: 2500 });
        this.draft.set(null);
        this.loadClientMenus();
        this.polls.refresh();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('nutriMenus.saveErr'),
                                     'OK', { duration: 3000 })
    });
  }

  remove(m: MenuPlan) {
    if (!m.id) return;
    if (!confirm(this.translate.instant('nutriMenus.deleteConfirm', { name: m.name }))) return;
    this.api.nutriDeleteMenu(m.id, this.practiceCtx.activePracticeId()).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriMenus.deleted'), 'OK', { duration: 2500 });
        this.loadClientMenus();
      },
      error: () => this.snack.open(this.translate.instant('nutriMenus.saveErr'), 'OK', { duration: 3000 })
    });
  }

  approve(p: MenuPlan) {
    if (!p.id) return;
    this.api.approveMenu(p.id).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriMenus.approved'), 'OK', { duration: 2500 });
        this.load();
        this.polls.refresh();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('nutriMenus.error'), 'OK', { duration: 3000 })
    });
  }

  startReject(p: MenuPlan) { this.rejectingId.set(p.id!); this.rejectReason = ''; }
  cancelReject() { this.rejectingId.set(null); this.rejectReason = ''; }
  confirmReject(p: MenuPlan) {
    if (!p.id) return;
    this.api.rejectMenu(p.id, this.rejectReason).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriMenus.rejected'), 'OK', { duration: 2500 });
        this.cancelReject();
        this.load();
        this.polls.refresh();
      },
      error: () => this.snack.open(this.translate.instant('nutriMenus.error'), 'OK', { duration: 3000 })
    });
  }
}
