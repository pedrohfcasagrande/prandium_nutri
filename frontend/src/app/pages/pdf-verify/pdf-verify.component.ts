import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PdfVerification } from '../../models/models';

/**
 * Fase 9D-3: pagina publica de verificacao de autenticidade de PDF.
 * URL: /verify OU /verify/:hash. Sem autenticacao — qualquer um pode conferir.
 */
@Component({
  selector: 'app-pdf-verify',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatFormFieldModule,
            MatInputModule, MatButtonModule, MatIconModule, TranslateModule],
  template: `
    <div class="verify-page">
      <div class="verify-shell">
        <div class="hero">
          <mat-icon class="brand-icon">verified</mat-icon>
          <h1>{{ 'pdfVerify.title' | translate }}</h1>
          <p class="text-muted">{{ 'pdfVerify.subtitle' | translate }}</p>
        </div>

        <mat-card class="verify-card">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'pdfVerify.hashLabel' | translate }}</mat-label>
            <input matInput [(ngModel)]="hashInput" (keydown.enter)="verify()"
                   placeholder="64 caracteres hex" maxlength="64">
            <mat-hint>{{ 'pdfVerify.hashHint' | translate }}</mat-hint>
          </mat-form-field>
          <button mat-flat-button color="primary" (click)="verify()"
                  [disabled]="loading() || hashInput.length !== 64">
            <mat-icon>search</mat-icon>
            {{ (loading() ? 'pdfVerify.checking' : 'pdfVerify.check') | translate }}
          </button>

          @if (result(); as r) {
            <div class="result" [class.result-valid]="r.valid" [class.result-invalid]="!r.valid">
              @if (r.valid) {
                <mat-icon class="big">check_circle</mat-icon>
                <h2>{{ 'pdfVerify.valid' | translate }}</h2>
                <ul>
                  <li><strong>{{ 'pdfVerify.issuedAt' | translate }}</strong>: {{ r.issuedAt | date:'medium' }}</li>
                  <li><strong>{{ 'pdfVerify.by' | translate }}</strong>: {{ r.nutriName }}
                    @if (r.nutriCrn) { — CRN {{ r.nutriCrn }} }
                  </li>
                  <li><strong>{{ 'pdfVerify.forClient' | translate }}</strong>:
                    {{ r.clientInitials }} · CPF {{ r.clientCpfMasked }}</li>
                </ul>
                <code class="hash-full">{{ r.pdfSha256 }}</code>
              } @else {
                <mat-icon class="big">error</mat-icon>
                <h2>{{ 'pdfVerify.invalid' | translate }}</h2>
                <p class="text-muted">{{ 'pdfVerify.invalidHint' | translate }}</p>
              }
            </div>
          }
        </mat-card>

        <p class="footer-note text-muted small">
          {{ 'pdfVerify.footerNote' | translate }}
        </p>
      </div>
    </div>
  `,
  styles: [`
    .verify-page { min-height: 100vh; padding: 40px 20px;
      background: var(--nt-hero-bg); display: grid; place-items: start center; }
    .verify-shell { width: 100%; max-width: 640px; display: flex; flex-direction: column; gap: 20px; }
    .hero { text-align: center; }
    .brand-icon { font-size: 44px !important; width: 44px !important; height: 44px !important;
      color: var(--nt-primary); }
    .hero h1 { font-size: 1.75rem; margin: 12px 0 6px; letter-spacing: -0.02em; }
    .verify-card { padding: 24px !important; display: flex; flex-direction: column; gap: 16px; }
    mat-form-field { display: block; }
    .result { padding: 20px; border-radius: 12px; text-align: center; margin-top: 8px;
      display: flex; flex-direction: column; align-items: center; gap: 10px; }
    .result-valid { background: color-mix(in oklab, var(--nt-success), transparent 88%);
      color: var(--nt-success); }
    .result-invalid { background: color-mix(in oklab, var(--nt-danger), transparent 88%);
      color: var(--nt-danger); }
    .result h2 { margin: 0; font-size: 1.2rem; }
    .result ul { list-style: none; margin: 0; padding: 0; color: var(--nt-text);
      display: flex; flex-direction: column; gap: 4px; font-size: 0.95rem; }
    .result .big { font-size: 40px !important; width: 40px !important; height: 40px !important; }
    .hash-full { display: block; font-family: 'JetBrains Mono', monospace; font-size: 0.72rem;
      background: var(--nt-surface); padding: 8px 12px; border-radius: 6px; word-break: break-all;
      color: var(--nt-text-muted); max-width: 100%; }
    .footer-note { text-align: center; }
  `]
})
export class PdfVerifyComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);

  hashInput = '';
  loading = signal(false);
  result = signal<PdfVerification | null>(null);

  ngOnInit() {
    const h = this.route.snapshot.paramMap.get('hash');
    if (h && h.length === 64) {
      this.hashInput = h;
      this.verify();
    }
  }

  verify() {
    if (this.hashInput.length !== 64) return;
    this.loading.set(true);
    this.api.verifyPdf(this.hashInput.toLowerCase()).subscribe({
      next: r => { this.loading.set(false); this.result.set(r); },
      error: () => { this.loading.set(false); this.result.set({
        valid: false, pdfSha256: this.hashInput
      }); }
    });
  }
}
