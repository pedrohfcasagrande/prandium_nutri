import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { Appointment, AppointmentLocation, AppointmentRequest, ClientLink, NutriPublic } from '../../models/models';

export interface AppointmentDialogData {
  mode: 'CLIENT' | 'NUTRI' | 'COMPANY';
  kind: 'CREATE' | 'RESCHEDULE';
  appointment: Appointment | null;
  clients: ClientLink[];
  nutritionists: NutriPublic[];
}

@Component({
  selector: 'app-appointment-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatDialogModule, TranslateModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>event</mat-icon>
      {{ (data.kind === 'RESCHEDULE' ? 'appointments.rescheduleTitle'
                                     : (data.mode === 'CLIENT' ? 'appointments.requestTitle' : 'appointments.suggestTitle')) | translate }}
    </h2>
    <mat-dialog-content class="stack">
      @if (data.kind === 'CREATE') {
        @if (data.mode === 'CLIENT') {
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'appointments.pickNutri' | translate }}</mat-label>
            <mat-select [(ngModel)]="nutritionistId">
              @for (n of data.nutritionists; track n.id) {
                <mat-option [value]="n.userId">{{ n.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        } @else if (data.mode === 'NUTRI') {
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'appointments.pickClient' | translate }}</mat-label>
            <mat-select [(ngModel)]="clientId">
              @for (c of data.clients; track c.id) {
                <mat-option [value]="c.clientId">{{ c.clientName }} · {{ c.clientEmail }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        }
      }

      <div class="row">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="grow">
          <mat-label>{{ 'appointments.date' | translate }}</mat-label>
          <input matInput type="date" [(ngModel)]="date" required>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="grow">
          <mat-label>{{ 'appointments.time' | translate }}</mat-label>
          <input matInput type="time" [(ngModel)]="time" required>
        </mat-form-field>
      </div>

      @if (data.kind === 'CREATE') {
        <div class="row">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'appointments.duration' | translate }}</mat-label>
            <mat-select [(ngModel)]="durationMinutes">
              <mat-option [value]="30">30 min</mat-option>
              <mat-option [value]="45">45 min</mat-option>
              <mat-option [value]="60">60 min</mat-option>
              <mat-option [value]="90">90 min</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'appointments.location.label' | translate }}</mat-label>
            <mat-select [(ngModel)]="location">
              <mat-option value="ONLINE">{{ 'appointments.location.ONLINE' | translate }}</mat-option>
              <mat-option value="IN_PERSON">{{ 'appointments.location.IN_PERSON' | translate }}</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'appointments.locationDetails' | translate }}</mat-label>
          <input matInput [(ngModel)]="locationDetails"
                 [placeholder]="location === 'ONLINE' ? 'Google Meet, Zoom, ...' : 'Rua, cidade, sala...'">
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'appointments.notesLabel' | translate }}</mat-label>
          <textarea matInput [(ngModel)]="notes" rows="3"
                    [placeholder]="'appointments.notesHint' | translate"></textarea>
        </mat-form-field>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="cancel()">{{ 'app.actions.cancel' | translate }}</button>
      <button mat-flat-button color="primary" (click)="confirm()"
              [disabled]="!isValid()">
        <mat-icon>save</mat-icon>
        {{ (data.kind === 'RESCHEDULE' ? 'appointments.reschedule' : 'appointments.save') | translate }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); } }
    .stack { display: flex; flex-direction: column; gap: 14px; min-width: min(90vw, 560px); }
    .row { display: flex; flex-wrap: wrap; gap: 12px; }
    .grow { flex: 1 1 200px; }
  `]
})
export class AppointmentDialogComponent {
  private ref = inject(MatDialogRef<AppointmentDialogComponent, AppointmentRequest | null>);

  clientId: number | null = null;
  nutritionistId: number | null = null;
  date: string;
  time: string;
  durationMinutes = 60;
  location: AppointmentLocation = 'ONLINE';
  locationDetails = '';
  notes = '';

  constructor(@Inject(MAT_DIALOG_DATA) public data: AppointmentDialogData) {
    // Default: proxima data (amanha 10h)
    const d = new Date();
    d.setDate(d.getDate() + 1);
    this.date = d.toISOString().slice(0, 10);
    this.time = '10:00';
    if (data.kind === 'RESCHEDULE' && data.appointment) {
      const ap = new Date(data.appointment.scheduledAt);
      this.date = ap.toISOString().slice(0, 10);
      this.time = ap.toTimeString().slice(0, 5);
    }
  }

  isValid(): boolean {
    if (!this.date || !this.time) return false;
    if (this.data.kind === 'CREATE') {
      if (this.data.mode === 'CLIENT' && !this.nutritionistId) return false;
      if (this.data.mode === 'NUTRI' && !this.clientId) return false;
    }
    return true;
  }

  confirm() {
    const scheduledAt = `${this.date}T${this.time}:00`;
    const req: AppointmentRequest = {
      scheduledAt,
      clientId: this.clientId,
      nutritionistId: this.nutritionistId,
      durationMinutes: this.durationMinutes,
      location: this.location,
      locationDetails: this.locationDetails || null,
      notes: this.notes || null
    };
    this.ref.close(req);
  }

  cancel() { this.ref.close(null); }
}
