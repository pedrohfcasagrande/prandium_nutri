import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { Practice } from '../../models/models';

@Component({
  selector: 'app-nutri-practices',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatSlideToggleModule,
    MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutriPractices.title' | translate }}</h1>
          <p>{{ 'nutriPractices.subtitle' | translate }}</p>
        </div>
      </div>

      @if (!practices().length) {
        <div class="empty card-surface">
          <mat-icon>badge</mat-icon>
          <p>{{ 'nutriPractices.empty' | translate }}</p>
        </div>
      } @else {
        <div class="list">
          @for (p of practices(); track p.id) {
            <mat-card class="practice-card"
                      [class.suspended]="p.status === 'SUSPENDED'"
                      [class.particular]="!p.companyName">
              <div class="head">
                <mat-icon>{{ p.companyName ? 'corporate_fare' : 'person' }}</mat-icon>
                <div>
                  <h3>{{ p.companyName ?? ('nutriPractices.private' | translate) }}</h3>
                  <span class="status-pill" [class]="'pill-' + p.status.toLowerCase()">
                    {{ ('nutriPractices.status.' + p.status) | translate }}
                  </span>
                </div>
              </div>

              @if (p.status === 'SUSPENDED') {
                <p class="text-muted small">{{ 'nutriPractices.suspendedHint' | translate }}</p>
              }

              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'nutriPractices.bio' | translate }}</mat-label>
                <textarea matInput rows="3" [(ngModel)]="p.bio"
                          [placeholder]="'nutriPractices.bioPlaceholder' | translate"></textarea>
              </mat-form-field>

              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'nutriPractices.specialties' | translate }}</mat-label>
                <input matInput [(ngModel)]="p.specialties"
                       [placeholder]="'nutriPractices.specialtiesHint' | translate">
              </mat-form-field>

              <mat-slide-toggle [(ngModel)]="p.acceptsNewClients"
                                [disabled]="p.status === 'SUSPENDED'">
                {{ 'nutriPractices.acceptsNewClients' | translate }}
              </mat-slide-toggle>

              <div class="actions">
                <button mat-flat-button color="primary" (click)="save(p)"
                        [disabled]="p.status === 'SUSPENDED' || saving().has(p.id)">
                  <mat-icon>save</mat-icon>
                  {{ 'app.actions.save' | translate }}
                </button>
              </div>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .list { display: flex; flex-direction: column; gap: 16px; }
    .practice-card {
      padding: 20px !important;
      display: flex; flex-direction: column; gap: 12px;
      border-left: 4px solid var(--nt-primary) !important;
    }
    .practice-card.particular { border-left-color: var(--nt-accent) !important; }
    .practice-card.suspended { opacity: 0.7; border-left-color: var(--nt-text-muted) !important; }

    .head { display: flex; align-items: center; gap: 12px;
      mat-icon { color: var(--nt-primary); font-size: 28px; width: 28px; height: 28px; }
      h3 { margin: 0; font-size: 1.1rem; font-weight: 600; }
    }

    .status-pill {
      display: inline-flex; padding: 2px 10px; border-radius: 12px;
      font-size: 0.72rem; font-weight: 700; letter-spacing: 0.02em; margin-top: 4px;
    }
    .pill-active { background: #d1fae5; color: #065f46; }
    .pill-suspended { background: var(--nt-surface-2); color: var(--nt-text-muted); }

    .actions { display: flex; justify-content: flex-end; }
    .small { font-size: 0.85rem; margin: 0; }
  `]
})
export class NutriPracticesComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  practices = signal<Practice[]>([]);
  saving = signal(new Set<number>());

  ngOnInit(): void { this.load(); }

  load() {
    this.api.nutriMyPractices().subscribe(list => this.practices.set(list));
  }

  save(p: Practice) {
    const s = new Set(this.saving()); s.add(p.id); this.saving.set(s);
    this.api.nutriUpdatePractice(p.id, {
      bio: p.bio ?? undefined,
      specialties: p.specialties ?? undefined,
      acceptsNewClients: p.acceptsNewClients
    }).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriPractices.savedOk'), 'OK', { duration: 2500 });
        const s2 = new Set(this.saving()); s2.delete(p.id); this.saving.set(s2);
      },
      error: err => {
        this.snack.open(err?.error?.error ?? this.translate.instant('nutriPractices.saveErr'),
                        'OK', { duration: 3000 });
        const s2 = new Set(this.saving()); s2.delete(p.id); this.saving.set(s2);
      }
    });
  }
}
