import { computed, DestroyRef, effect, inject, Injectable, signal, untracked } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AiConversationService } from '../../../entities/reports/api/ai-conversation.service';
import { AiConversation, AiMessage, AiMessageRequest, AiReportView, AiTurn } from '../../../entities/reports/model/ai-conversation.model';
import { PageResponse } from '../../../shared/api';
import { AuthFacade } from '../../../shared/lib/auth/auth.facade';
import { SessionCoordinatorService } from '../../../shared/lib/auth/session-coordinator.service';
import { ToastService } from '../../../shared/ui/toast/toast.service';
import { aiReportError } from './ai-report-error';

export interface AiSendAttempt extends AiMessageRequest {
  phase: 'sending' | 'pending' | 'uncertain'; requestId?: string; error?: string;
}
// Conversation storage accepts 4000, but the current planner request accepts 2000.
export const AI_REPORT_QUESTION_MAX_LENGTH = 2000;

/** Scoped to the assistant panel: no conversations or personal rows survive its lifetime. */
@Injectable()
export class AiConversationFacade {
  private readonly api = inject(AiConversationService);
  private readonly auth = inject(AuthFacade);
  private readonly sessions = inject(SessionCoordinatorService);
  private readonly toast = inject(ToastService);
  private subscriptions = new Subscription();
  private historyRequest?: Subscription;
  private listRequest?: Subscription;
  private epoch = 0;
  private selection = 0;
  private listGeneration = 0;
  private historyGeneration = 0;
  private identity = '';
  private initialized = false;

  readonly conversations = signal<PageResponse<AiConversation> | null>(null);
  readonly selected = signal<AiConversation | null>(null);
  readonly history = signal<PageResponse<AiMessage> | null>(null);
  readonly messages = signal<AiMessage[]>([]);
  readonly loadingList = signal(false);
  readonly loadingHistory = signal(false);
  readonly creating = signal(false);
  readonly deleting = signal(false);
  readonly error = signal('');
  readonly results = signal<Record<string, AiReportView>>({});
  readonly refreshing = signal<Record<string, boolean>>({});
  readonly updatingReport = computed(() => Object.values(this.refreshing()).some(Boolean));
  private readonly drafts = signal<Record<string, string>>({});
  private readonly attempts = signal<Record<string, AiSendAttempt>>({});
  readonly draft = computed(() => this.drafts()[this.selected()?.id ?? ''] ?? '');
  readonly attempt = computed<AiSendAttempt | null>(() => this.attempts()[this.selected()?.id ?? ''] ?? null);
  readonly showLocalAttempt = computed(() => {
    const attempt = this.attempt();
    return !!attempt && !this.messages().some(message => message.id === attempt.requestId);
  });

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      const identity = this.auth.status() === 'ready' && user?.scope === 'TENANT' ? `${user.tenantSlug}:${user.id}:${user.authorizationVersion}:${user.securityVersion}` : '';
      if (identity !== this.identity) untracked(() => {
        this.reset(); this.identity = identity;
        if (this.initialized && identity) this.loadConversations();
      });
    });
    inject(DestroyRef).onDestroy(() => this.reset());
  }

  initialize(): void {
    if (this.initialized) return;
    this.initialized = true;
    this.identity = this.currentIdentity();
    this.loadConversations();
  }
  setDraft(value: string): void {
    const id = this.selected()?.id;
    if (id) this.drafts.update(drafts => ({ ...drafts, [id]: value }));
  }
  loadConversations(page = this.conversations()?.page ?? 0): void {
    if (!this.currentIdentity()) return;
    this.listRequest?.unsubscribe();
    const generation = ++this.listGeneration;
    this.loadingList.set(true); this.error.set('');
    this.listRequest = this.observe(this.api.list(page), response => {
      if (generation !== this.listGeneration) return;
      this.conversations.set(response.data); this.loadingList.set(false);
      if (page > 0 && response.data.content.length === 0) this.loadConversations(page - 1);
    }, error => { if (generation === this.listGeneration) { this.loadingList.set(false); this.fail(error); } });
  }
  create(): void {
    if (this.creating() || !this.currentIdentity()) return;
    this.creating.set(true); this.error.set('');
    this.observe(this.api.create(), response => {
      this.creating.set(false); this.loadConversations(0); this.select(response.data);
    }, error => { this.creating.set(false); this.fail(error); });
  }
  select(conversation: AiConversation): void {
    if (this.selected()?.id === conversation.id) return;
    this.selection++; this.historyGeneration++; this.historyRequest?.unsubscribe();
    this.selected.set(conversation); this.history.set(null); this.messages.set([]);
    this.results.set({}); this.refreshing.set({}); this.error.set('');
    const selection = this.selection;
    this.loadingHistory.set(true);
    this.observe(this.api.get(conversation.id), response => {
      if (selection !== this.selection) return;
      this.selected.set(response.data); this.loadHistory(0, true);
    }, error => { if (selection === this.selection) { this.loadingHistory.set(false); this.fail(error); } });
  }
  loadHistory(page = this.history()?.page ?? 0, latest = false): void {
    const id = this.selected()?.id;
    if (!id) return;
    this.historyRequest?.unsubscribe();
    const generation = ++this.historyGeneration;
    const selection = this.selection;
    this.loadingHistory.set(true); this.error.set('');
    this.historyRequest = this.observe(this.api.messages(id, page), response => {
      if (selection !== this.selection || generation !== this.historyGeneration) return;
      if (latest && response.data.totalPages > 0 && response.data.page !== response.data.totalPages - 1) {
        this.loadHistory(response.data.totalPages - 1); return;
      }
      this.history.set(response.data); this.messages.set(response.data.content); this.loadingHistory.set(false);
      const ids = new Set(response.data.content.map(message => message.id));
      this.results.update(results => Object.fromEntries(Object.entries(results).filter(([key]) => ids.has(key))));
      const attempt = this.attempts()[id];
      if (attempt?.requestId) {
        const request = response.data.content.find(message => message.id === attempt.requestId && message.state !== 'PENDING');
        if (request) {
          this.removeAttempt(id);
          if (request.state === 'COMPLETED') this.drafts.update(drafts => ({ ...drafts, [id]: drafts[id]?.trim() === attempt.message ? '' : drafts[id] ?? '' }));
        }
      }
    }, error => { if (selection === this.selection && generation === this.historyGeneration) { this.loadingHistory.set(false); this.fail(error); } });
  }
  deleteSelected(): void {
    const id = this.selected()?.id;
    if (!id || this.deleting() || this.attempt()?.phase === 'sending') return;
    this.deleting.set(true);
    this.observe(this.api.delete(id), () => {
      this.deleting.set(false);
      if (this.selected()?.id === id) {
        this.selection++; this.historyRequest?.unsubscribe();
        this.selected.set(null); this.messages.set([]); this.history.set(null); this.results.set({});
        this.loadingHistory.set(false); this.refreshing.set({});
      }
      this.removeAttempt(id);
      this.drafts.update(drafts => { const next = { ...drafts }; delete next[id]; return next; });
      this.loadConversations(); this.toast.success('Conversación eliminada.');
    }, error => { this.deleting.set(false); this.fail(error); });
  }
  send(): void {
    const id = this.selected()?.id, message = this.draft().trim();
    if (!id || this.attempt() || this.updatingReport() || !message || [...message].length > AI_REPORT_QUESTION_MAX_LENGTH) return;
    this.executeSend(id, { clientMessageId: crypto.randomUUID(), message, phase: 'sending' });
  }
  retry(): void {
    const id = this.selected()?.id, attempt = this.attempt();
    if (!id || !attempt || attempt.phase === 'sending' || this.updatingReport()) return;
    this.executeSend(id, { ...attempt, phase: 'sending', error: undefined });
  }
  refresh(message: AiMessage, page?: number): void {
    const id = this.selected()?.id, view = this.results()[message.id];
    const runId = view?.reportRunId ?? message.reportRunId;
    if (!id || !runId || this.attempt() || this.updatingReport()) return;
    const selection = this.selection;
    this.refreshing.update(current => ({ ...current, [message.id]: true }));
    this.error.set('');
    this.observe(this.api.refresh(id, runId, page ?? view?.result.pageNumber, view?.result.pageSize), response => {
      if (selection !== this.selection || this.selected()?.id !== id) return;
      this.results.update(results => ({ ...results, [message.id]: {
        reportRunId: response.data.reportRunId, result: response.data.result, executedAt: response.data.refreshedAt
      } }));
      this.messages.update(messages => messages.map(item => item.id === message.id ? { ...item, reportRunId: response.data.reportRunId } : item));
      this.refreshing.update(current => ({ ...current, [message.id]: false }));
    }, error => { if (selection === this.selection) { this.refreshing.update(current => ({ ...current, [message.id]: false })); this.fail(error); } });
  }

  private executeSend(id: string, attempt: AiSendAttempt): void {
    if (!this.currentIdentity()) return;
    const selection = this.selection;
    this.attempts.update(attempts => ({ ...attempts, [id]: attempt })); this.error.set('');
    const request: AiMessageRequest = { clientMessageId: attempt.clientMessageId, message: attempt.message };
    this.observe(this.api.send(id, request), response => {
      if (response.data.conversationId !== id) {
        const error = 'La respuesta no corresponde a la conversación.';
        this.attempts.update(attempts => ({ ...attempts, [id]: { ...attempt, phase: 'uncertain', error } }));
        if (selection === this.selection) this.fail(new Error(error));
        return;
      }
      const turn = response.data;
      if (turn.status === 'pending') this.attempts.update(attempts => ({ ...attempts, [id]: { ...attempt, requestId: turn.request.id, phase: 'pending' } }));
      else {
        this.removeAttempt(id);
        if (turn.status !== 'error') this.drafts.update(drafts => ({ ...drafts, [id]: drafts[id]?.trim() === attempt.message ? '' : drafts[id] ?? '' }));
      }
      if (selection !== this.selection || this.selected()?.id !== id) return;
      this.applyTurn(turn);
      this.loadHistory(0, true);
    }, error => {
      if (error instanceof HttpErrorResponse && [400, 401, 403, 404].includes(error.status)) this.removeAttempt(id);
      else this.attempts.update(attempts => ({ ...attempts, [id]: { ...attempt, phase: 'uncertain', error: aiReportError(error) } }));
      if (selection === this.selection) this.fail(error);
    });
  }
  private applyTurn(turn: AiTurn): void {
    const response = turn.response ? { ...turn.response, reportRunId: turn.reportRunId ?? null } : null;
    const added = response ? [turn.request, response] : [turn.request];
    this.messages.update(messages => [...messages.filter(item => !added.some(value => value.id === item.id)), ...added]);
    if (response && turn.result && turn.reportRunId) this.results.update(results => ({ ...results, [response.id]: {
      reportRunId: turn.reportRunId!, result: turn.result!, executedAt: response.completedAt
    } }));
    if (turn.status === 'error') this.error.set(response?.content ?? 'No se pudo completar la consulta.');
  }
  private removeAttempt(id: string): void {
    this.attempts.update(attempts => { const next = { ...attempts }; delete next[id]; return next; });
  }
  private currentIdentity(): string {
    const user = this.auth.currentUser();
    return this.auth.status() === 'ready' && user?.scope === 'TENANT' && !this.sessions.isLoggingOut('TENANT') ? `${user.tenantSlug}:${user.id}:${user.authorizationVersion}:${user.securityVersion}` : '';
  }
  private observe<T>(source: Observable<T>, next: (value: T) => void, error: (failure: unknown) => void = failure => this.fail(failure)): Subscription {
    const epoch = this.epoch, identity = this.currentIdentity();
    if (!identity) return Subscription.EMPTY;
    const valid = () => epoch === this.epoch && identity === this.currentIdentity();
    const subscription = source.subscribe({ next: value => { if (valid()) next(value); }, error: failure => { if (valid()) error(failure); } });
    this.subscriptions.add(subscription); return subscription;
  }
  private fail(error: unknown): void { const message = aiReportError(error); this.error.set(message); this.toast.error(message); }
  private reset(): void {
    this.epoch++; this.selection++; this.listGeneration++; this.historyGeneration++;
    this.subscriptions.unsubscribe(); this.subscriptions = new Subscription();
    this.conversations.set(null); this.selected.set(null); this.history.set(null); this.messages.set([]);
    this.results.set({}); this.refreshing.set({}); this.drafts.set({}); this.attempts.set({});
    this.loadingList.set(false); this.loadingHistory.set(false); this.creating.set(false); this.deleting.set(false); this.error.set('');
  }
}
