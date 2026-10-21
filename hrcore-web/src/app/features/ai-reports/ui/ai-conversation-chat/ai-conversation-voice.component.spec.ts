import { importProvidersFrom, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FileChartColumn, LoaderCircle, LucideAngularModule, Mic, Send } from 'lucide-angular';
import { of, Subject } from 'rxjs';
import { AiConversationFacade } from '../../application/ai-conversation.facade';
import { AiVoiceService } from '../../../../entities/reports/api/ai-voice.service';
import { AuthFacade } from '../../../../shared/lib/auth/auth.facade';
import { AiConversationChatComponent } from './ai-conversation-chat.component';

class FakeRecorder {
  static supports = (mime: string) => mime.startsWith('audio/webm');
  static isTypeSupported(mime: string) { return FakeRecorder.supports(mime); }
  static latest: FakeRecorder;
  mimeType: string;
  state = 'inactive';
  ondataavailable: ((event: { data: Blob }) => void) | null = null;
  onstop: (() => void) | null = null;
  onerror: (() => void) | null = null;
  constructor(_stream: MediaStream, options?: MediaRecorderOptions) {
    this.mimeType = options?.mimeType ?? 'audio/webm'; FakeRecorder.latest = this;
  }
  start() { this.state = 'recording'; }
  stop() {
    this.state = 'inactive';
    this.ondataavailable?.({ data: new Blob(['synthetic'], { type: this.mimeType }) });
    this.onstop?.();
  }
}

describe('AI Reports: dictado', () => {
  let facade: any, auth: any, voiceApi: any;
  let originalRecorder: PropertyDescriptor | undefined;
  let originalMediaDevices: PropertyDescriptor | undefined;
  let track: { stop: jasmine.Spy; onended: (() => void) | null };
  let input: Subject<string>;

  beforeEach(async () => {
    originalRecorder = Object.getOwnPropertyDescriptor(window, 'MediaRecorder');
    originalMediaDevices = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices');
    Object.defineProperty(window, 'MediaRecorder', { configurable: true, value: FakeRecorder });
    track = { stop: jasmine.createSpy('stop'), onended: null };
    const stream = { getTracks: () => [track], getAudioTracks: () => [track] } as unknown as MediaStream;
    Object.defineProperty(navigator, 'mediaDevices', { configurable: true,
      value: { getUserMedia: jasmine.createSpy('getUserMedia').and.resolveTo(stream) } });
    input = new Subject<string>();
    const draft = signal('texto escrito');
    facade = {
      selected: signal({ id: 'conversation' }), draft, setDraft: jasmine.createSpy('setDraft').and.callFake((value: string) => draft.set(value)),
      send: jasmine.createSpy('send'), messages: signal([]), history: signal(null), loadingHistory: signal(false),
      results: signal({}), refreshing: signal({}), attempt: signal(null), showLocalAttempt: signal(false),
      updatingReport: signal(false), canExport: signal(false), exporting: signal({}), downloading: signal(false)
    };
    auth = { currentUser: signal({ scope: 'TENANT', tenantSlug: 'tenant', id: 'user', authorizationVersion: 1, securityVersion: 1 }), status: signal('ready') };
    voiceApi = { config: jasmine.createSpy('config').and.returnValue(of({ enabled: true, maxDurationSeconds: 30, maxBytes: 8388608 })),
      transcribe: jasmine.createSpy('transcribe').and.returnValue(input.asObservable()) };
    await TestBed.configureTestingModule({ imports: [AiConversationChatComponent], providers: [
      { provide: AiConversationFacade, useValue: facade }, { provide: AuthFacade, useValue: auth },
      { provide: AiVoiceService, useValue: voiceApi },
      importProvidersFrom(LucideAngularModule.pick({ Send, LoaderCircle, FileChartColumn, Mic }))
    ] }).compileComponents();
  });

  afterEach(() => {
    if (originalRecorder) Object.defineProperty(window, 'MediaRecorder', originalRecorder);
    else delete (window as any).MediaRecorder;
    if (originalMediaDevices) Object.defineProperty(navigator, 'mediaDevices', originalMediaDevices);
    else delete (navigator as any).mediaDevices;
    FakeRecorder.supports = mime => mime.startsWith('audio/webm');
  });

  it('graba, transcribe y añade texto editable sin enviarlo', async () => {
    const fixture = TestBed.createComponent(AiConversationChatComponent); fixture.detectChanges();
    await fixture.componentInstance.startVoice();
    expect(fixture.componentInstance.voiceState()).toBe('recording');
    fixture.componentInstance.stopVoice();
    expect(track.stop).toHaveBeenCalled();
    expect(voiceApi.transcribe).toHaveBeenCalled();
    input.next('pregunta hablada');
    expect(facade.draft()).toBe('texto escrito pregunta hablada');
    expect(facade.send).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('cancela grabación y una respuesta tardía al cambiar de conversación', async () => {
    const fixture = TestBed.createComponent(AiConversationChatComponent); fixture.detectChanges();
    await fixture.componentInstance.startVoice();
    fixture.componentInstance.cancelVoice();
    expect(track.stop).toHaveBeenCalled();
    expect(voiceApi.transcribe).not.toHaveBeenCalled();
    await fixture.componentInstance.startVoice();
    fixture.componentInstance.stopVoice();
    facade.selected.set({ id: 'another' }); fixture.detectChanges();
    input.next('respuesta vieja');
    expect(facade.setDraft).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('muestra permiso denegado y detecta Ogg cuando WebM no está disponible', async () => {
    const fixture = TestBed.createComponent(AiConversationChatComponent); fixture.detectChanges();
    (navigator.mediaDevices.getUserMedia as jasmine.Spy).and.rejectWith(new DOMException('denied', 'NotAllowedError'));
    await fixture.componentInstance.startVoice();
    expect(fixture.componentInstance.voiceState()).toBe('error');
    expect(fixture.componentInstance.voiceError()).toContain('Permite');
    (navigator.mediaDevices.getUserMedia as jasmine.Spy).and.resolveTo({
      getTracks: () => [track], getAudioTracks: () => [track]
    } as unknown as MediaStream);
    FakeRecorder.supports = mime => mime.startsWith('audio/ogg');
    await fixture.componentInstance.startVoice();
    expect(FakeRecorder.latest.mimeType).toContain('audio/ogg');
    fixture.componentInstance.cancelVoice(); fixture.destroy();
  });

  it('deshabilita voz cuando no hay formato compatible', () => {
    FakeRecorder.supports = () => false;
    const fixture = TestBed.createComponent(AiConversationChatComponent); fixture.detectChanges();
    expect(fixture.componentInstance.voiceState()).toBe('unsupported');
    expect(voiceApi.transcribe).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('selecciona MP4 y libera el micrófono al cambiar de tenant', async () => {
    FakeRecorder.supports = mime => mime.startsWith('audio/mp4');
    const fixture = TestBed.createComponent(AiConversationChatComponent); fixture.detectChanges();
    await fixture.componentInstance.startVoice();
    expect(FakeRecorder.latest.mimeType).toContain('audio/mp4');
    auth.currentUser.set({ ...auth.currentUser(), tenantSlug: 'otro-tenant' });
    fixture.detectChanges();
    expect(track.stop).toHaveBeenCalled();
    expect(voiceApi.transcribe).not.toHaveBeenCalled();
    fixture.destroy();
  });
});
