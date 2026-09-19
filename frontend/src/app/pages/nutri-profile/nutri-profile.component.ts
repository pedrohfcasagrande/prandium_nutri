import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { NutriProfileMe } from '../../models/models';

@Component({
  selector: 'app-nutri-profile',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule, MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutriProfile.title' | translate }}</h1>
          <p>{{ 'nutriProfile.subtitle' | translate }}</p>
        </div>
      </div>

      @if (profile()) {
        <mat-card class="card">
          <div class="opt-in">
            <mat-slide-toggle color="primary" [(ngModel)]="profile()!.acceptsNewClients">
              {{ 'nutriProfile.acceptsNewClients' | translate }}
            </mat-slide-toggle>
            <p class="text-muted small">{{ 'nutriProfile.acceptsHint' | translate }}</p>
          </div>

          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'nutriProfile.specialties' | translate }}</mat-label>
            <input matInput [(ngModel)]="profile()!.specialties"
                   [placeholder]="'nutriProfile.specialtiesHint' | translate">
          </mat-form-field>

          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'nutriProfile.bio' | translate }}</mat-label>
            <textarea matInput rows="6" [(ngModel)]="profile()!.bio"
                      [placeholder]="'nutriProfile.bioHint' | translate"></textarea>
          </mat-form-field>

          <div class="actions">
            <button mat-flat-button color="primary" (click)="save()">
              <mat-icon>save</mat-icon> {{ 'app.actions.save' | translate }}
            </button>
          </div>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .card { padding: 24px !important; display: flex; flex-direction: column; gap: 18px; }
    .opt-in { display: flex; flex-direction: column; gap: 4px; }
    .small { font-size: 0.85rem; margin: 0; }
    .actions { display: flex; justify-content: flex-end; }
    mat-form-field { width: 100%; }
  `]
})
export class NutriProfileComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  profile = signal<NutriProfileMe | null>(null);

  ngOnInit() {
    this.api.nutriProfileMe().subscribe(p => this.profile.set(p));
  }

  save() {
    const p = this.profile();
    if (!p) return;
    this.api.updateNutriProfile(p).subscribe({
      next: () => this.snack.open(this.translate.instant('nutriProfile.saved'), 'OK', { duration: 2500 }),
      error: () => this.snack.open(this.translate.instant('nutriProfile.saveFail'), 'OK', { duration: 3000 })
    });
  }
}
