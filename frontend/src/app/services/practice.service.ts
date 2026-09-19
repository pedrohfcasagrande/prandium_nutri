import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { ApiService } from './api.service';
import { Practice } from '../models/models';

const KEY = 'prandiumnutri.practice';

/**
 * Contexto de "practice ativa" do nutri (Fase 5D).
 * Persistido em sessionStorage. Componentes do nutri leem
 * `activePracticeId()` e enviam como query param quando aplicavel.
 */
@Injectable({ providedIn: 'root' })
export class PracticeService {
  private auth = inject(AuthService);
  private api = inject(ApiService);

  readonly practices = signal<Practice[]>([]);
  readonly activePracticeId = signal<number | null>(this.loadStoredId());

  /** Chamado no login/refresh de sessao do nutri. Idempotente. */
  loadForCurrentNutri(): void {
    if (this.auth.currentUser()?.role !== 'NUTRITIONIST') {
      this.practices.set([]);
      return;
    }
    this.api.nutriMyPractices().subscribe({
      next: list => {
        this.practices.set(list);
        // Se o id salvo nao esta mais na lista, reseta pra particular
        const current = this.activePracticeId();
        if (current && !list.some(p => p.id === current)) {
          this.setActive(list.find(p => !p.companyId)?.id ?? list[0]?.id ?? null);
        } else if (!current && list.length) {
          // Default: particular
          this.setActive(list.find(p => !p.companyId)?.id ?? list[0].id);
        }
      },
      error: () => this.practices.set([])
    });
  }

  setActive(id: number | null) {
    this.activePracticeId.set(id);
    if (id == null) sessionStorage.removeItem(KEY);
    else sessionStorage.setItem(KEY, String(id));
  }

  clear() {
    this.practices.set([]);
    this.setActive(null);
  }

  private loadStoredId(): number | null {
    const raw = sessionStorage.getItem(KEY);
    return raw ? Number(raw) : null;
  }
}
