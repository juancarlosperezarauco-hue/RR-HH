import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { AiConversationFacade } from './ai-conversation.facade';
import { AiConversationService } from '../../../entities/reports/api/ai-conversation.service';
import { AiConversation, AiMessage, AiReportRefresh, AiTurn, QueryResult } from '../../../entities/reports/model/ai-conversation.model';
import { AuthFacade } from '../../../shared/lib/auth/auth.facade';
import { SessionCoordinatorService } from '../../../shared/lib/auth/session-coordinator.service';
import { ToastService } from '../../../shared/ui/toast/toast.service';
import { PageResponse } from '../../../shared/api';

const conversation = (id = 'c'): AiConversation => ({ id, title: 'Nuevo informe', status: 'ACTIVE', createdAt: '', updatedAt: '', lastMessageAt: null, version: 0 });
const message = (id: string, role: 'USER' | 'ASSISTANT' = 'USER'): AiMessage => ({ id, role, content: 'Texto ficticio', messageType: role === 'USER' ? 'TEXT' : 'REPORT', state: 'COMPLETED', createdAt: '', completedAt: null, reportRunId: role === 'ASSISTANT' ? 'run' : null });
const reply = <T>(data: T) => ({ success: true, data, message: 'OK', timestamp: '' });
const page = <T>(content: T[] = [], number = 0, totalPages = 1): PageResponse<T> => ({ content, page: number, size: 50, totalPages, totalElements: content.length });
const result: QueryResult = { columns: [], rows: [], pageNumber: 0, pageSize: 20, hasMore: false, warnings: [] };

describe('AiConversationFacade: identidad, concurrencia e idempotencia', () => {
  let facade: AiConversationFacade, api: jasmine.SpyObj<AiConversationService>;
  const user = signal<{ id: string; tenantSlug: string; scope: string } | null>(null);
  const status = signal('ready');
  beforeEach(() => {
    user.set({ id: 'user1', tenantSlug: 'synthetic', scope: 'TENANT' }); status.set('ready');
    api = jasmine.createSpyObj('AiConversationService', ['list', 'create', 'get', 'messages', 'delete', 'send', 'refresh']);
    api.list.and.returnValue(of(reply(page<AiConversation>([conversation()]))));
    api.get.and.callFake(id => of(reply(conversation(id))));
    api.messages.and.returnValue(of(reply(page<AiMessage>())));
    api.create.and.returnValue(of(reply(conversation('new')))); api.delete.and.returnValue(of(reply(null)));
    TestBed.configureTestingModule({ providers: [AiConversationFacade,
      { provide: AiConversationService, useValue: api },
      { provide: AuthFacade, useValue: { currentUser: user, status } },
      { provide: SessionCoordinatorService, useValue: { isLoggingOut: () => false } },
      { provide: ToastService, useValue: { error: jasmine.createSpy(), success: jasmine.createSpy() } }
    ] });
    facade = TestBed.inject(AiConversationFacade); TestBed.flushEffects();
    facade.initialize(); facade.select(conversation());
  });
  it('inicializa una sola vez y crear/abrir no envía preguntas', () => {
    facade.initialize(); expect(api.list).toHaveBeenCalledTimes(1);
    facade.create(); expect(facade.selected()?.id).toBe('new'); expect(api.create).toHaveBeenCalledTimes(1);
    expect(api.send).not.toHaveBeenCalled();
  });
  it('consulta una página para localizar la última sin cargar todo el historial', () => {
    api.messages.and.callFake((_id, number) => of(reply(page<AiMessage>([], number ?? 0, 4))));
    facade.select(conversation('large'));
    expect(api.messages).toHaveBeenCalledWith('large', 0);
    expect(api.messages).toHaveBeenCalledWith('large', 3);
    expect(facade.history()?.page).toBe(3);
  });
  it('bloquea doble envío y reintenta fallo de red con idéntico clientMessageId', () => {
    const pending = new Subject<ReturnType<typeof reply<AiTurn>>>(); api.send.and.returnValue(pending);
    facade.setDraft('Pregunta ficticia'); facade.send(); facade.send(); expect(api.send).toHaveBeenCalledTimes(1);
    const original = api.send.calls.mostRecent().args[1];
    pending.error(new HttpErrorResponse({ status: 0 }));
    expect(facade.draft()).toBe('Pregunta ficticia'); expect(facade.attempt()?.phase).toBe('uncertain');
    api.send.and.returnValue(of(reply<AiTurn>({ conversationId: 'c', status: 'ok', request: message('request'), response: message('answer', 'ASSISTANT'), reportRunId: 'run' })));
    facade.retry(); expect(api.send.calls.mostRecent().args[1]).toEqual(original); expect(facade.attempt()).toBeNull();
    facade.setDraft('Otra pregunta'); facade.send(); expect(api.send.calls.mostRecent().args[1].clientMessageId).not.toBe(original.clientMessageId);
  });
  it('conserva pending sin polling y permite recarga manual', () => {
    api.send.and.returnValue(of(reply<AiTurn>({ conversationId: 'c', status: 'pending', request: { ...message('request'), state: 'PENDING' } })));
    facade.setDraft('Pregunta ficticia'); facade.send();
    expect(facade.attempt()?.phase).toBe('pending'); expect(api.send).toHaveBeenCalledTimes(1);
    const reads = api.messages.calls.count(); facade.loadHistory(); expect(api.messages.calls.count()).toBe(reads + 1);
    expect(api.send).toHaveBeenCalledTimes(1);
  });
  it('una respuesta de envío tardía nunca aparece en otra conversación', () => {
    const pending = new Subject<ReturnType<typeof reply<AiTurn>>>(); api.send.and.returnValue(pending);
    facade.setDraft('Pregunta ficticia'); facade.send(); facade.select(conversation('other'));
    pending.next(reply({ conversationId: 'c', status: 'ok', request: message('private'), response: message('private-answer', 'ASSISTANT'), reportRunId: 'run', result }));
    expect(facade.selected()?.id).toBe('other'); expect(facade.messages()).toEqual([]); expect(facade.results()).toEqual({});
  });
  it('cancela historial obsoleto al cambiar de conversación', () => {
    const pending = new Subject<ReturnType<typeof reply<PageResponse<AiMessage>>>>(); api.messages.and.returnValue(pending);
    facade.loadHistory(); facade.select(conversation('other'));
    expect(pending.observers.length).toBe(1);
    api.messages.and.returnValue(of(reply(page<AiMessage>()))); facade.select(conversation('third'));
    expect(pending.observers.length).toBe(0); expect(facade.messages()).toEqual([]);
  });
  it('cambio de tenant, usuario o logout limpia datos e invalida peticiones', () => {
    const pending = new Subject<ReturnType<typeof reply<AiTurn>>>(); api.send.and.returnValue(pending);
    facade.setDraft('Pregunta ficticia'); facade.send();
    user.set({ id: 'user1', tenantSlug: 'other', scope: 'TENANT' }); TestBed.flushEffects();
    expect(pending.observers.length).toBe(0); expect(facade.selected()).toBeNull(); expect(facade.draft()).toBe('');
    facade.select(conversation()); facade.setDraft('Borrador');
    user.set({ id: 'user2', tenantSlug: 'other', scope: 'TENANT' }); TestBed.flushEffects(); expect(facade.selected()).toBeNull();
    user.set(null); status.set('idle'); TestBed.flushEffects(); expect(facade.messages()).toEqual([]); expect(facade.results()).toEqual({});
  });
  it('refresh/paginación utiliza runId sin enviar mensajes ni planificar', () => {
    api.refresh.and.returnValue(of(reply({ reportRunId: 'new-run', refreshedFromRunId: 'run', refreshedAt: '2026-10-19T10:00:00Z', result })));
    const report = message('answer', 'ASSISTANT'); facade.refresh(report);
    expect(api.refresh).toHaveBeenCalledWith('c', 'run', undefined, undefined);
    expect(facade.results()['answer'].reportRunId).toBe('new-run');
    facade.refresh(report, 1); expect(api.refresh).toHaveBeenCalledWith('c', 'new-run', 1, 20);
    expect(api.send).not.toHaveBeenCalled(); expect(api.create).not.toHaveBeenCalled();
  });
  it('rechazo de validación libera el envío y conserva el borrador para corregirlo', () => {
    api.send.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'Pregunta no válida' } })));
    facade.setDraft('Pregunta ficticia'); facade.send(); expect(facade.attempt()).toBeNull(); expect(facade.draft()).toBe('Pregunta ficticia');
    expect(facade.error()).toBe('Pregunta no válida');
    api.send.and.returnValue(of(reply<AiTurn>({ conversationId: 'c', status: 'error', request: message('failed'), response: { ...message('failure', 'ASSISTANT'), messageType: 'ERROR', content: 'Error persistido' } })));
    facade.send(); expect(facade.attempt()).toBeNull(); expect(facade.draft()).toBe('Pregunta ficticia');
  });
  it('elimina con el servicio actual y vacía la selección', () => {
    facade.deleteSelected(); expect(api.delete).toHaveBeenCalledOnceWith('c'); expect(facade.selected()).toBeNull();
  });
  it('respeta el límite efectivo del planner y rechaza blancos sin llamar a la API', () => {
    facade.setDraft(' '.repeat(5)); facade.send();
    facade.setDraft('a'.repeat(2001)); facade.send();
    expect(api.send).not.toHaveBeenCalled();
  });
  it('serializa refresh y envíos de la misma conversación para no invalidar un turno en planificación', () => {
    const refreshing = new Subject<ReturnType<typeof reply<AiReportRefresh>>>(); api.refresh.and.returnValue(refreshing);
    const report = message('answer', 'ASSISTANT'); facade.refresh(report);
    facade.setDraft('Pregunta nueva'); facade.send(); facade.refresh(report);
    expect(api.send).not.toHaveBeenCalled(); expect(api.refresh).toHaveBeenCalledTimes(1);
    refreshing.next(reply({ reportRunId: 'next', refreshedFromRunId: 'run', refreshedAt: '', result })); refreshing.complete();
    api.send.and.returnValue(of(reply<AiTurn>({ conversationId: 'c', status: 'pending', request: { ...message('request'), state: 'PENDING' } })));
    facade.send(); facade.refresh(report); expect(api.send).toHaveBeenCalledTimes(1); expect(api.refresh).toHaveBeenCalledTimes(1);
  });
});
