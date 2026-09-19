import { Component, OnInit, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { ApiService } from '../services/api.service';
import { AppAlert } from '../models/models';

@Component({
  selector: 'app-alert-banner',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, TranslateModule],
  template: `
    @for (a of alerts(); track a.id) {
      <div class="alert" [class.alert-warn]="a.severity === 'WARN'"
                         [class.alert-critical]="a.severity === 'CRITICAL'">
        <mat-icon>{{ icon(a.severity) }}</mat-icon>
        <div class="alert-body">
          <strong>{{ a.title }}</strong>
          @if (a.message) { <div class="text-muted small">{{ a.message }}</div> }
        </div>
        <button mat-icon-button (click)="dismiss(a)"
                [attr.aria-label]="'alerts.dismiss' | translate">
          <mat-icon>close</mat-icon>
        </button>
      </div>
    }
  `,
  styles: [`
    .alert {
      display: flex; align-items: center; gap: 12px;
      padding: 14px 18px; border-radius: var(--nt-radius);
      background: #dbeafe; color: #1e40af; margin-bottom: 12px;
      border-left: 4px solid #3b82f6;
      mat-icon { color: #1d4ed8; }
      .small { color: #1e3a8a; font-size: 0.85rem; }
      .alert-body { flex: 1; }
    }
    .alert-warn { background: #fef3c7; color: #92400e; border-left-color: #f59e0b;
      mat-icon { color: #b45309; }
      .small { color: #78350f; }
    }
    .alert-critical { background: #fee2e2; color: #991b1b; border-left-color: #ef4444;
      mat-icon { color: #b91c1c; }
      .small { color: #7f1d1d; }
    }
  `]
})
export class AlertBannerComponent implements OnInit {
  private api = inject(ApiService);

  @Input({ required: true }) scope!: 'admin' | 'company' | 'nutritionist';
  alerts = signal<AppAlert[]>([]);

  ngOnInit() { this.load(); }

  load() {
    this.api.listAlerts(this.scope).subscribe({
      next: a => this.alerts.set(a),
      error: () => this.alerts.set([])
    });
  }

  dismiss(a: AppAlert) {
    this.api.dismissAlert(a.id).subscribe(() => this.alerts.update(list => list.filter(x => x.id !== a.id)));
  }

  icon(sev: string): string {
    switch (sev) {
      case 'CRITICAL': return 'error';
      case 'WARN': return 'warning';
      default: return 'info';
    }
  }
}
