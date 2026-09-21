import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { PracticeService } from '../../services/practice.service';
import { NotificationPollService } from '../../services/notification-poll.service';
import { Appointment, AppointmentLocation, AppointmentRequest, ClientLink, NutriPublic } from '../../models/models';
import { AppointmentDialogComponent, AppointmentDialogData } from './appointment-dialog.component';

type Mode = 'CLIENT' | 'NUTRI' | 'COMPANY';

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule, MatDialogModule, MatTooltipModule,
    TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'appointments.title' | translate }}</h1>
          <p>{{ ('appointments.subtitle.' + mode) | translate }}</p>
        </div>
        @if (canRequest()) {
          <button mat-flat-button color="primary" (click)="openDialog()">
            <mat-icon>event</mat-icon>
            {{ (mode === 'CLIENT' ? 'appointments.request' : 'appointments.suggest') | translate }}
          </button>
        }
      </div>

      @if (appointments().length === 0) {
        <div class="empty card-surface">
          <mat-icon>event_available</mat-icon>
          <p>{{ 'appointments.empty' | translate }}</p>
        </div>
      } @else {
        <div class="list">
          @for (a of appointments(); track a.id) {
            <mat-card class="ap-card" [class]="'status-' + a.status.toLowerCase()">
              <div class="date-block">
                <div class="day">{{ a.scheduledAt | date:'dd' }}</div>
                <div class="month">{{ a.scheduledAt | date:'MMM' }}</div>
                <div class="time">{{ a.scheduledAt | date:'HH:mm' }}</div>
              </div>
              <div class="info">
                <div class="name">
                  @if (mode === 'CLIENT') {
                    <mat-icon class="small-icon">medical_services</mat-icon>{{ a.nutritionistName }}
                  } @else {
                    <mat-icon class="small-icon">person</mat-icon>{{ a.clientName }}
                    <span class="text-muted small">· {{ a.clientEmail }}</span>
                  }
                </div>
                <div class="meta">
                  <span class="chip" [class]="'chip-' + a.status.toLowerCase()">
                    {{ ('appointments.status.' + a.status) | translate }}
                  </span>
                  <span class="text-muted small">
                    <mat-icon class="small-icon">{{ a.location === 'ONLINE' ? 'videocam' : 'place' }}</mat-icon>
                    {{ ('appointments.location.' + a.location) | translate }}
                    @if (a.locationDetails) { · {{ a.locationDetails }} }
                  </span>
                  <span class="text-muted small">
                    <mat-icon class="small-icon">schedule</mat-icon>
                    {{ a.durationMinutes }}min
                  </span>
                  @if (a.companyName && mode !== 'CLIENT') {
                    <span class="text-muted small">
                      <mat-icon class="small-icon">corporate_fare</mat-icon>{{ a.companyName }}
                    </span>
                  }
                </div>
                @if (a.status === 'RESCHEDULE_REQUESTED' && a.proposedScheduledAt) {
                  <div class="proposed">
                    <mat-icon class="small-icon">update</mat-icon>
                    {{ 'appointments.proposedTo' | translate:{ date: (a.proposedScheduledAt | date:'short') } }}
                    · {{ 'appointments.by' | translate }}: {{ ('appointments.actor.' + a.proposedBy) | translate }}
                  </div>
                }
                @if (a.notes) {
                  <div class="notes text-muted small">
                    <mat-icon class="small-icon">notes</mat-icon>{{ a.notes }}
                  </div>
                }
                @if (a.cancelReason) {
                  <div class="notes text-muted small">
                    <mat-icon class="small-icon">cancel</mat-icon>{{ a.cancelReason }}
                  </div>
                }
              </div>
              @if (mode !== 'COMPANY') {
                <div class="actions">
                  @if (canConfirm(a)) {
                    <button mat-flat-button color="primary" (click)="confirm(a)">
                      <mat-icon>check</mat-icon>{{ 'appointments.confirm' | translate }}
                    </button>
                  }
                  @if (canReschedule(a)) {
                    <button mat-stroked-button (click)="openDialog(a, 'RESCHEDULE')">
                      <mat-icon>update</mat-icon>{{ 'appointments.reschedule' | translate }}
                    </button>
                  }
                  @if (canCancel(a)) {
                    <button mat-icon-button color="warn" (click)="cancel(a)"
                            [matTooltip]="'appointments.cancel' | translate">
                      <mat-icon>cancel</mat-icon>
                    </button>
                  }
                  @if (mode === 'NUTRI' && a.status === 'CONFIRMED') {
                    <button mat-icon-button (click)="complete(a)"
                            [matTooltip]="'appointments.complete' | translate">
                      <mat-icon>done_all</mat-icon>
                    </button>
                    <button mat-icon-button (click)="noShow(a)"
                            [matTooltip]="'appointments.noShow' | translate">
                      <mat-icon>person_off</mat-icon>
                    </button>
                  }
                </div>
              }
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .list { display: flex; flex-direction: column; gap: 12px; }
    .ap-card {
      padding: 16px 20px !important;
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 20px;
    }
    .ap-card.status-canceled, .ap-card.status-no_show { opacity: 0.75; }
    .ap-card.status-confirmed { border-left: 4px solid var(--nt-primary) !important; }
    .ap-card.status-requested { border-left: 4px solid var(--nt-accent) !important; }
    .ap-card.status-reschedule_requested { border-left: 4px solid #8b5cf6 !important; }
    .ap-card.status-canceled { border-left: 4px solid var(--nt-danger) !important; }
    .ap-card.status-completed { border-left: 4px solid #64748b !important; }
    .ap-card.status-no_show { border-left: 4px solid #94a3b8 !important; }

    .date-block {
      text-align: center;
      min-width: 68px;
      padding: 8px;
      background: var(--nt-surface-2);
      border-radius: var(--nt-radius);
    }
    .day { font-size: 1.6rem; font-weight: 700; line-height: 1; color: var(--nt-text); }
    .month { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--nt-text-muted); margin-top: 2px; }
    .time { font-size: 0.85rem; font-weight: 600; color: var(--nt-primary-dark); margin-top: 6px; font-variant-numeric: tabular-nums; }

    .info { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
    .name { font-weight: 600; font-size: 1.05rem; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
    .small { font-size: 0.82rem; }
    .small-icon { font-size: 16px !important; width: 16px !important; height: 16px !important; vertical-align: middle; }
    .meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .proposed { background: #ede9fe; color: #5b21b6; padding: 6px 10px; border-radius: 8px; font-size: 0.85rem; display: inline-flex; align-items: center; gap: 6px; width: fit-content; }
    .notes { display: inline-flex; align-items: center; gap: 6px; }

    .chip {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 3px 12px; border-radius: 999px;
      font-weight: 700; font-size: 0.7rem;
      text-transform: uppercase; letter-spacing: 0.03em;
    }
    .chip-requested { background: #fef3c7; color: #92400e; }
    .chip-confirmed { background: #d1fae5; color: #065f46; }
    .chip-reschedule_requested { background: #ede9fe; color: #5b21b6; }
    .chip-canceled { background: #fee2e2; color: #991b1b; }
    .chip-completed { background: #dbeafe; color: #1e40af; }
    .chip-no_show { background: #f1f5f9; color: #475569; }

    .actions { display: flex; gap: 4px; align-items: center; flex-wrap: wrap; }

    @media (max-width: 720px) {
      .ap-card { grid-template-columns: auto 1fr; }
      .actions { grid-column: 1 / -1; justify-self: end; }
    }
  `]
})
export class AppointmentsComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);
  private practiceCtx = inject(PracticeService);
  private polls = inject(NotificationPollService);
  private route = inject(ActivatedRoute);

  appointments = signal<Appointment[]>([]);
  clients = signal<ClientLink[]>([]);
  nutritionists = signal<NutriPublic[]>([]);

  mode: Mode = 'CLIENT';

  canRequest = computed(() => this.mode !== 'COMPANY');

  private firstLoad = true;
  constructor() {
    effect(() => {
      this.practiceCtx.activePracticeId();
      if (this.firstLoad) { this.firstLoad = false; return; }
      if (this.mode === 'NUTRI') this.load();
    }, { allowSignalWrites: true });
  }

  ngOnInit() {
    this.mode = (this.route.snapshot.data['mode'] as Mode) ?? this.detectMode();
    this.load();
    if (this.mode === 'NUTRI') this.loadClients();
    if (this.mode === 'CLIENT') this.loadNutritionists();
  }

  private detectMode(): Mode {
    const role = this.auth.currentUser()?.role;
    if (role === 'NUTRITIONIST') return 'NUTRI';
    if (role === 'COMPANY') return 'COMPANY';
    return 'CLIENT';
  }

  load() {
    const obs = this.mode === 'CLIENT' ? this.api.clientListAppointments()
              : this.mode === 'NUTRI' ? this.api.nutriListAppointments(this.practiceCtx.activePracticeId())
              : this.api.companyListAppointments();
    obs.subscribe(list => this.appointments.set(list));
  }

  loadClients() {
    this.api.nutriListClients(this.practiceCtx.activePracticeId(), '', 'name', 0, 500)
      .subscribe(p => this.clients.set(p.items.filter(c => c.status === 'ACTIVE' && c.clientId)));
  }

  loadNutritionists() {
    this.api.availableNutris().subscribe(list => this.nutritionists.set(list));
  }

  openDialog(ap?: Appointment, kind: 'CREATE' | 'RESCHEDULE' = 'CREATE') {
    const data: AppointmentDialogData = {
      mode: this.mode,
      kind,
      appointment: ap ?? null,
      clients: this.clients(),
      nutritionists: this.nutritionists()
    };
    const ref = this.dialog.open(AppointmentDialogComponent, {
      data,
      maxWidth: '720px',
      width: '90vw'
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      if (kind === 'RESCHEDULE' && ap) {
        this.reschedule(ap, result.scheduledAt);
      } else {
        this.create(result);
      }
    });
  }

  private create(req: AppointmentRequest) {
    const pid = this.practiceCtx.activePracticeId();
    if (this.mode === 'NUTRI') req.practiceId = pid ?? undefined;
    const obs = this.mode === 'CLIENT'
      ? this.api.clientRequestAppointment(req)
      : this.api.nutriSuggestAppointment(req);
    obs.subscribe({
      next: () => {
        this.snack.open(this.translate.instant('appointments.saved'), 'OK', { duration: 2500 });
        this.load();
        this.polls.refresh();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('appointments.saveErr'),
                                    'OK', { duration: 3000 })
    });
  }

  canConfirm(a: Appointment): boolean {
    const my = this.mode === 'CLIENT' ? 'CLIENT' : 'NUTRITIONIST';
    if (a.status === 'REQUESTED') return a.requestedBy !== my;
    if (a.status === 'RESCHEDULE_REQUESTED') return a.proposedBy !== my;
    return false;
  }

  canReschedule(a: Appointment): boolean {
    return ['REQUESTED', 'CONFIRMED', 'RESCHEDULE_REQUESTED'].includes(a.status);
  }

  canCancel(a: Appointment): boolean {
    return !['CANCELED', 'COMPLETED', 'NO_SHOW'].includes(a.status);
  }

  confirm(a: Appointment) {
    const obs = this.mode === 'CLIENT'
      ? this.api.clientConfirmAppointment(a.id)
      : this.api.nutriConfirmAppointment(a.id);
    obs.subscribe({
      next: () => { this.snack.open(this.translate.instant('appointments.confirmed'), 'OK', { duration: 2500 }); this.load(); this.polls.refresh(); },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('appointments.saveErr'), 'OK', { duration: 3000 })
    });
  }

  cancel(a: Appointment) {
    const reason = prompt(this.translate.instant('appointments.cancelReasonPrompt')) || undefined;
    const obs = this.mode === 'CLIENT'
      ? this.api.clientCancelAppointment(a.id, reason)
      : this.api.nutriCancelAppointment(a.id, reason);
    obs.subscribe({
      next: () => { this.snack.open(this.translate.instant('appointments.canceled'), 'OK', { duration: 2500 }); this.load(); this.polls.refresh(); },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('appointments.saveErr'), 'OK', { duration: 3000 })
    });
  }

  private reschedule(a: Appointment, scheduledAt: string) {
    const obs = this.mode === 'CLIENT'
      ? this.api.clientRescheduleAppointment(a.id, scheduledAt)
      : this.api.nutriRescheduleAppointment(a.id, scheduledAt);
    obs.subscribe({
      next: () => { this.snack.open(this.translate.instant('appointments.rescheduled'), 'OK', { duration: 2500 }); this.load(); this.polls.refresh(); },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('appointments.saveErr'), 'OK', { duration: 3000 })
    });
  }

  complete(a: Appointment) {
    this.api.nutriCompleteAppointment(a.id).subscribe({
      next: () => { this.snack.open(this.translate.instant('appointments.completed'), 'OK', { duration: 2500 }); this.load(); },
      error: () => this.snack.open(this.translate.instant('appointments.saveErr'), 'OK', { duration: 3000 })
    });
  }

  noShow(a: Appointment) {
    this.api.nutriNoShowAppointment(a.id).subscribe({
      next: () => { this.snack.open(this.translate.instant('appointments.markedNoShow'), 'OK', { duration: 2500 }); this.load(); },
      error: () => this.snack.open(this.translate.instant('appointments.saveErr'), 'OK', { duration: 3000 })
    });
  }
}
