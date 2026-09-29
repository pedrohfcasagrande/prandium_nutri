import { Component, OnDestroy, OnInit, inject, signal, ViewChild, ElementRef, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ChatMessage } from '../../models/models';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'chat.title' | translate }}</h1>
          <p>{{ 'chat.subtitle' | translate }}</p>
        </div>
        @if (backLink()) {
          <a mat-stroked-button [routerLink]="backLink()">
            <mat-icon>arrow_back</mat-icon>
            {{ 'chat.back' | translate }}
          </a>
        }
      </div>

      <mat-card class="chat-card">
        <div class="messages" #scroll>
          @if (loading()) {
            <div class="loading">
              <mat-icon>hourglass_empty</mat-icon>
              {{ 'chat.loading' | translate }}
            </div>
          } @else if (!messages().length) {
            <div class="empty">
              <mat-icon>chat_bubble_outline</mat-icon>
              <span>{{ 'chat.empty' | translate }}</span>
            </div>
          } @else {
            @for (m of messages(); track m.id) {
              <div class="msg" [class.mine]="m.senderId === myId()">
                <div class="bubble">
                  <div class="content">{{ m.content }}</div>
                  <div class="meta">
                    <span>{{ m.createdAt | date:'HH:mm' }}</span>
                    @if (m.senderId === myId() && m.readAt) {
                      <mat-icon class="read-tick" [attr.title]="'chat.read' | translate">done_all</mat-icon>
                    }
                  </div>
                </div>
              </div>
            }
          }
        </div>

        <form class="composer" (ngSubmit)="send()">
          <mat-form-field appearance="outline" class="composer-field">
            <input matInput [(ngModel)]="draft" name="msg"
                   [placeholder]="'chat.placeholder' | translate"
                   [disabled]="sending()"
                   maxlength="2000" />
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit"
                  [disabled]="!draft.trim() || sending()">
            <mat-icon>send</mat-icon>
          </button>
        </form>

        @if (error()) {
          <div class="err">
            <mat-icon>error</mat-icon>{{ error() }}
          </div>
        }
      </mat-card>
    </div>
  `,
  styles: [`
    .chat-card { padding: 0 !important; display: flex; flex-direction: column; height: calc(100vh - 220px); min-height: 440px; }
    .messages {
      flex: 1; overflow-y: auto; padding: 20px;
      display: flex; flex-direction: column; gap: 8px;
      background: var(--nt-surface-2);
    }
    .empty, .loading {
      margin: auto; display: flex; flex-direction: column; align-items: center; gap: 8px;
      color: var(--nt-text-muted);
      mat-icon { font-size: 40px; width: 40px; height: 40px; opacity: 0.4; }
    }
    .msg { display: flex; }
    .msg.mine { justify-content: flex-end; }
    .bubble {
      max-width: 72%;
      padding: 8px 12px;
      border-radius: 14px 14px 14px 4px;
      background: var(--nt-surface); border: 1px solid var(--nt-border);
      box-shadow: 0 1px 2px rgba(0,0,0,0.04);
    }
    .msg.mine .bubble {
      border-radius: 14px 14px 4px 14px;
      background: var(--nt-primary-soft); border-color: transparent;
    }
    .content { white-space: pre-wrap; word-break: break-word; font-size: 0.95rem; }
    .meta {
      display: flex; justify-content: flex-end; align-items: center; gap: 4px;
      margin-top: 4px; font-size: 0.7rem; color: var(--nt-text-muted);
    }
    .read-tick { font-size: 14px; width: 14px; height: 14px; color: var(--nt-primary); }
    .composer {
      display: flex; gap: 8px; padding: 12px 16px;
      border-top: 1px solid var(--nt-border);
    }
    .composer-field { flex: 1; }
    .composer button { height: 56px; }
    .err {
      display: flex; align-items: center; gap: 6px;
      padding: 8px 16px; color: #dc2626; font-size: 0.85rem;
    }
    @media (max-width: 600px) {
      .chat-card { height: calc(100vh - 160px); }
      .bubble { max-width: 88%; }
    }
  `]
})
export class ChatComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);

  linkId!: number;
  messages = signal<ChatMessage[]>([]);
  loading = signal<boolean>(true);
  sending = signal<boolean>(false);
  error = signal<string | null>(null);
  draft = '';

  @ViewChild('scroll', { static: false }) scroll?: ElementRef<HTMLDivElement>;

  private eventSource?: EventSource;
  private pollHandle?: number;

  constructor() {
    effect(() => {
      this.messages();
      queueMicrotask(() => this.scrollToBottom());
    });
  }

  myId(): number | null {
    return this.auth.currentUser()?.userId ?? null;
  }

  backLink(): string {
    const role = this.auth.currentUser()?.role;
    if (role === 'NUTRITIONIST') return '/nutritionist/clients';
    return '/dashboard';
  }

  ngOnInit(): void {
    this.linkId = Number(this.route.snapshot.paramMap.get('linkId'));
    if (!Number.isFinite(this.linkId)) return;
    this.load();
    this.connectStream();
  }

  ngOnDestroy(): void {
    this.eventSource?.close();
    if (this.pollHandle) window.clearInterval(this.pollHandle);
  }

  load() {
    this.loading.set(true);
    this.api.listChat(this.linkId).subscribe({
      next: (list) => {
        this.messages.set(list);
        this.loading.set(false);
        this.api.markChatRead(this.linkId).subscribe();
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.status === 403 ? 'chat.forbidden' : 'chat.loadError');
      }
    });
  }

  send() {
    const content = this.draft.trim();
    if (!content) return;
    this.sending.set(true);
    this.api.sendChat(this.linkId, content).subscribe({
      next: (msg) => {
        this.mergeIncoming(msg);
        this.draft = '';
        this.sending.set(false);
      },
      error: () => {
        this.sending.set(false);
        this.error.set('chat.sendError');
      }
    });
  }

  private connectStream() {
    const token = this.auth.token();
    if (!token) {
      this.startPolling();
      return;
    }
    try {
      const url = `${environment.apiUrl}/chat/${this.linkId}/stream?access_token=${encodeURIComponent(token)}`;
      this.eventSource = new EventSource(url);
      this.eventSource.addEventListener('message', (ev) => {
        try {
          const msg: ChatMessage = JSON.parse((ev as MessageEvent).data);
          this.mergeIncoming(msg);
          if (msg.senderId !== this.myId()) {
            this.api.markChatRead(this.linkId).subscribe();
          }
        } catch { /* ignore malformed */ }
      });
      this.eventSource.addEventListener('error', () => {
        // fallback pra polling se SSE cair (ex.: proxy corta)
        this.eventSource?.close();
        this.eventSource = undefined;
        this.startPolling();
      });
    } catch {
      this.startPolling();
    }
  }

  private startPolling() {
    if (this.pollHandle) return;
    this.pollHandle = window.setInterval(() => {
      this.api.listChat(this.linkId).subscribe(list => {
        if (list.length !== this.messages().length) {
          this.messages.set(list);
          this.api.markChatRead(this.linkId).subscribe();
        }
      });
    }, 5000);
  }

  private mergeIncoming(msg: ChatMessage) {
    const cur = this.messages();
    if (cur.some(m => m.id === msg.id)) return;
    this.messages.set([...cur, msg]);
  }

  private scrollToBottom() {
    const el = this.scroll?.nativeElement;
    if (el) el.scrollTop = el.scrollHeight;
  }
}
