import { computed, importProvidersFrom, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FileChartColumn, LoaderCircle, LucideAngularModule, Plus, RefreshCcw, Send, Sparkles, Trash2 } from 'lucide-angular';
import { AiConversationFacade, AiSendAttempt } from '../../application/ai-conversation.facade';
import { AiMessage, AiReportView } from '../../../../entities/reports/model/ai-conversation.model';
import { AiConversationChatComponent } from './ai-conversation-chat.component';
import { AiConversationPanelComponent } from '../ai-conversation-panel/ai-conversation-panel.component';
import { AiVoiceService } from '../../../../entities/reports/api/ai-voice.service';
import { AuthFacade } from '../../../../shared/lib/auth/auth.facade';
import { of } from 'rxjs';
import { Mic } from 'lucide-angular';

describe('AI Reports: chat y confirmación accesible', () => {
  let fixture: ComponentFixture<AiConversationChatComponent>;
  let messages: ReturnType<typeof signal<AiMessage[]>>, attempt: ReturnType<typeof signal<AiSendAttempt | null>>;
  let fake: ReturnType<typeof makeFacade>;
  function makeFacade() {
    const draft = signal('');
    return { messages, attempt, draft, setDraft: (value: string) => draft.set(value),
      canExport: signal(false), exporting: signal<Record<string, string>>({}), downloading: signal(false), export: jasmine.createSpy('export'),
      loadingHistory: signal(false), history: signal(null), results: signal<Record<string, AiReportView>>({}), refreshing: signal<Record<string, boolean>>({}), updatingReport: signal(false),
      showLocalAttempt: computed(() => !!attempt()), send: jasmine.createSpy('send'), retry: jasmine.createSpy('retry'), refresh: jasmine.createSpy('refresh'), loadHistory: jasmine.createSpy(),
      initialize: jasmine.createSpy(), create: jasmine.createSpy(), loadConversations: jasmine.createSpy(), select: jasmine.createSpy(), deleteSelected: jasmine.createSpy(),
      selected: signal({ id: 'c', title: 'Nuevo informe' }), conversations: signal(null), loadingList: signal(false), creating: signal(false), deleting: signal(false), error: signal('') };
  }
  const record = (type: AiMessage['messageType'], content: string): AiMessage => ({ id: type, role: type === 'TEXT' ? 'USER' : 'ASSISTANT', content, messageType: type, state: 'COMPLETED', createdAt: '', completedAt: null, reportRunId: type === 'REPORT' ? 'run' : null });
  beforeEach(async () => {
    messages = signal<AiMessage[]>([]); attempt = signal<AiSendAttempt | null>(null); fake = makeFacade();
    await TestBed.configureTestingModule({ imports: [AiConversationChatComponent, AiConversationPanelComponent], providers: [
      { provide: AiConversationFacade, useValue: fake },
      { provide: AiVoiceService, useValue: { config: () => of({ enabled: false, maxDurationSeconds: 30, maxBytes: 8388608 }), transcribe: jasmine.createSpy('transcribe') } },
      { provide: AuthFacade, useValue: { currentUser: signal({ scope: 'TENANT', tenantSlug: 'test', id: 'u', authorizationVersion: 1, securityVersion: 1 }), status: signal('ready') } },
      importProvidersFrom(LucideAngularModule.pick({ Send, LoaderCircle, Sparkles, Trash2, Plus, RefreshCcw, FileChartColumn, Mic }))
    ] }).overrideComponent(AiConversationPanelComponent, { set: { providers: [{ provide: AiConversationFacade, useValue: fake }] } }).compileComponents();
    fixture = TestBed.createComponent(AiConversationChatComponent); fixture.detectChanges();
  });
  it('muestra mensajes, aclaraciones y rechazos sin interpretar HTML recibido', () => {
    messages.set([record('TEXT', '<img src=x onerror=alert(1)>'), record('CLARIFICATION', 'Indica el período'), record('UNSUPPORTED', 'No disponible'), record('ERROR', 'Consulta fallida')]); fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('<img src=x onerror=alert(1)>'); expect(root.querySelector('img')).toBeNull();
    expect(root.textContent).toContain('Aclaración necesaria'); expect(root.textContent).toContain('Solicitud no soportada'); expect(root.textContent).toContain('Consulta no completada');
  });
  it('Enter envía; Shift+Enter y composición IME permiten seguir escribiendo', () => {
    const input: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true, bubbles: true }));
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', isComposing: true, bubbles: true })); expect(fake.send).not.toHaveBeenCalled();
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true })); expect(fake.send).toHaveBeenCalledTimes(1);
    expect(input.getAttribute('aria-describedby')).toBe('ai-question-help');
  });
  it('pending mantiene el texto visible, bloquea otro envío y ofrece recuperación manual', () => {
    attempt.set({ clientMessageId: 'logical-id', message: 'Pregunta sin confirmar', phase: 'pending' }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Pregunta sin confirmar');
    expect((fixture.nativeElement.querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBeTrue();
    const retry = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('button')).find(button => button.textContent?.includes('Reintentar el mismo envío'))!;
    retry.click(); expect(fake.retry).toHaveBeenCalledTimes(1); expect(fake.send).not.toHaveBeenCalled();
  });
  it('un reporte histórico ofrece actualización explícita sin nuevas preguntas', () => {
    const report = record('REPORT', 'Reporte ejecutado correctamente.'); messages.set([report]); fixture.detectChanges();
    const button = Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button')).find(item => item.textContent?.includes('Consultar datos actuales'))!;
    button.click(); expect(fake.refresh).toHaveBeenCalledWith(report); expect(fake.send).not.toHaveBeenCalled();
  });
  it('ofrece los tres formatos desde historial solo con permiso y bloquea durante descarga', () => {
    const report = record('REPORT', 'Reporte ejecutado'); messages.set([report]); fixture.detectChanges();
    const buttons = () => Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('[aria-label="Descargar reporte"] button'));
    expect(buttons().length).toBe(0);
    fake.canExport.set(true); fixture.detectChanges();
    expect(buttons().length).toBe(3);
    buttons()[1].click(); expect(fake.export).toHaveBeenCalledOnceWith(report, 'xlsx');
    fake.downloading.set(true); fake.exporting.set({ REPORT: 'xlsx' }); fixture.detectChanges();
    expect(buttons().every(button => button.disabled)).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Preparando archivo');
  });
  it('exige confirmación nativa antes de eliminar y permite cancelación por teclado', () => {
    const panel = TestBed.createComponent(AiConversationPanelComponent); panel.detectChanges();
    panel.componentInstance.confirmDelete(); const dialog: HTMLDialogElement = panel.nativeElement.querySelector('dialog');
    expect(dialog.open).toBeTrue(); expect(dialog.getAttribute('aria-labelledby')).toBe('delete-conversation-title'); expect(fake.deleteSelected).not.toHaveBeenCalled();
    panel.componentInstance.cancelDelete(); expect(dialog.open).toBeFalse();
    panel.componentInstance.confirmDelete(); panel.componentInstance.deleteConversation(); expect(fake.deleteSelected).toHaveBeenCalledTimes(1);
  });
});
