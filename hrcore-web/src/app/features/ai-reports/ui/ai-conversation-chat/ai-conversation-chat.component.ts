import { CommonModule } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Subscription } from 'rxjs';
import { AI_REPORT_QUESTION_MAX_LENGTH, AiConversationFacade } from '../../application/ai-conversation.facade';
import { aiReportError } from '../../application/ai-report-error';
import { AiMessageType, AiReportExportFormat } from '../../../../entities/reports/model/ai-conversation.model';
import { AiVoiceConfig, AiVoiceService } from '../../../../entities/reports/api/ai-voice.service';
import { AuthFacade } from '../../../../shared/lib/auth/auth.facade';
import { AiReportResultComponent } from '../ai-report-result/ai-report-result.component';

@Component({ selector: 'app-ai-conversation-chat', standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, AiReportResultComponent], templateUrl: './ai-conversation-chat.component.html' })
export class AiConversationChatComponent {
  readonly facade = inject(AiConversationFacade);
  private readonly voiceApi = inject(AiVoiceService);
  private readonly auth = inject(AuthFacade);
  readonly maxQuestionLength = AI_REPORT_QUESTION_MAX_LENGTH;
  readonly voiceState = signal<'loading' | 'requesting' | 'available' | 'recording' | 'processing' | 'error' | 'unsupported' | 'disabled'>('loading');
  readonly voiceError = signal('');
  private voiceConfig: AiVoiceConfig | null = null;
  private voiceIdentity = '';
  private conversationId = '';
  private generation = 0;
  private configRequest?: Subscription;
  private uploadRequest?: Subscription;
  private stream?: MediaStream;
  private recorder?: MediaRecorder;
  private chunks: Blob[] = [];
  private recordedBytes = 0;
  private timer?: ReturnType<typeof setTimeout>;
  readonly formats: { format: AiReportExportFormat; label: string }[] = [
    { format: 'csv', label: 'CSV' }, { format: 'xlsx', label: 'Excel' }, { format: 'pdf', label: 'PDF' }
  ];
  readonly labels: Record<AiMessageType, string> = { TEXT: 'Pregunta', REPORT: 'Reporte', CLARIFICATION: 'Aclaración necesaria', UNSUPPORTED: 'Solicitud no soportada', ERROR: 'Consulta no completada' };
  get characterCount(): number { return [...this.facade.draft()].length; }
  get voiceConfigDuration(): number { return this.voiceConfig?.maxDurationSeconds ?? 30; }
  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      const identity = this.auth.status() === 'ready' && user?.scope === 'TENANT'
        ? `${user.tenantSlug}:${user.id}:${user.authorizationVersion}:${user.securityVersion}` : '';
      const conversation = this.facade.selected()?.id ?? '';
      if (identity !== this.voiceIdentity || conversation !== this.conversationId) untracked(() => {
        const identityChanged = identity !== this.voiceIdentity;
        this.cancelVoice();
        this.voiceIdentity = identity;
        this.conversationId = conversation;
        if (identityChanged) {
          this.configRequest?.unsubscribe(); this.voiceConfig = null;
          if (identity) this.loadVoiceConfig(identity);
          else this.voiceState.set('disabled');
        }
      });
    });
    inject(DestroyRef).onDestroy(() => { this.configRequest?.unsubscribe(); this.cancelVoice(); });
  }

  private loadVoiceConfig(identity: string): void {
    this.voiceState.set('loading');
    this.configRequest = this.voiceApi.config().subscribe({
      next: config => {
        if (identity !== this.voiceIdentity) return;
        this.voiceConfig = config;
        this.voiceState.set(!config.enabled ? 'disabled' : this.supported() ? 'available' : 'unsupported');
      },
      error: error => {
        if (identity !== this.voiceIdentity) return;
        this.voiceError.set(aiReportError(error)); this.voiceState.set('error');
      }
    });
  }

  private supported(): boolean {
    return typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia
      && typeof MediaRecorder !== 'undefined' && this.chooseMime() !== null;
  }

  private chooseMime(): string | null {
    const candidates = [
      'audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/mp4;codecs=mp4a.40.2',
      'audio/webm', 'audio/ogg', 'audio/mp4'
    ];
    if (typeof MediaRecorder === 'undefined') return null;
    if (typeof MediaRecorder.isTypeSupported !== 'function') return '';
    return candidates.find(mime => MediaRecorder.isTypeSupported(mime)) ?? null;
  }

  async startVoice(): Promise<void> {
    if (this.voiceState() !== 'available' && this.voiceState() !== 'error') return;
    if (!this.voiceConfig?.enabled) { if (this.voiceIdentity) this.loadVoiceConfig(this.voiceIdentity); return; }
    if (!this.supported() || !this.conversationId) return;
    this.cancelVoice();
    const generation = this.generation;
    this.voiceError.set(''); this.voiceState.set('requesting');
    let stream: MediaStream;
    try { stream = await navigator.mediaDevices.getUserMedia({ audio: true }); }
    catch (error) {
      if (generation !== this.generation) return;
      this.voiceError.set(error instanceof DOMException && error.name === 'NotAllowedError'
        ? 'Permite el acceso al micrófono para dictar.' : 'No se pudo abrir el micrófono.');
      this.voiceState.set('error'); return;
    }
    if (generation !== this.generation) { stream.getTracks().forEach(track => track.stop()); return; }
    this.stream = stream;
    try {
      const mime = this.chooseMime();
      const recorder = mime ? new MediaRecorder(stream, { mimeType: mime }) : new MediaRecorder(stream);
      const type = recorder.mimeType || mime || '';
      const base = type.split(';', 1)[0].toLowerCase();
      if (!['audio/webm', 'audio/ogg', 'audio/mp4'].includes(base)) throw new Error('Formato de audio no compatible.');
      this.recorder = recorder; this.chunks = []; this.recordedBytes = 0;
      recorder.ondataavailable = event => {
        if (generation !== this.generation || !event.data?.size) return;
        this.recordedBytes += event.data.size;
        if (this.recordedBytes > this.voiceConfig!.maxBytes) {
          this.failVoice('La grabación supera el tamaño permitido.'); return;
        }
        this.chunks.push(event.data);
      };
      recorder.onerror = () => this.failVoice('La grabación se interrumpió.');
      recorder.onstop = () => {
        if (generation !== this.generation) return;
        this.stopTracks();
        recorder.ondataavailable = null; recorder.onstop = null; recorder.onerror = null;
        this.recorder = undefined;
        const audio = new Blob(this.chunks, { type: base });
        this.chunks = [];
        if (!audio.size) { this.failVoice('La grabación está vacía.'); return; }
        const extension = base === 'audio/webm' ? 'webm' : base === 'audio/ogg' ? 'ogg' : 'mp4';
        this.voiceState.set('processing');
        const identity = this.voiceIdentity, conversation = this.conversationId;
        this.uploadRequest = this.voiceApi.transcribe(audio, extension).subscribe({
          next: text => {
            if (generation !== this.generation || identity !== this.voiceIdentity || conversation !== this.conversationId) return;
            const current = this.facade.draft();
            const combined = current + (current && !/\s$/.test(current) ? ' ' : '') + text.trim();
            if ([...combined].length > this.maxQuestionLength) {
              this.failVoice('La transcripción supera el límite del campo. Acorta el texto y vuelve a intentar.'); return;
            }
            this.facade.setDraft(combined);
            this.voiceState.set('available');
          },
          error: error => this.failVoice(aiReportError(error))
        });
      };
      stream.getAudioTracks().forEach(track => { track.onended = () => {
        if (generation === this.generation && this.voiceState() === 'recording') this.stopVoice();
      }; });
      recorder.start(1000);
      this.voiceState.set('recording');
      this.timer = setTimeout(() => this.stopVoice(), Math.max(100, this.voiceConfig.maxDurationSeconds * 1000 - 500));
    } catch (error) {
      this.failVoice(error instanceof Error ? error.message : 'No se pudo iniciar la grabación.');
    }
  }

  stopVoice(): void {
    if (this.voiceState() !== 'recording' || !this.recorder) return;
    if (this.timer) clearTimeout(this.timer);
    this.timer = undefined;
    this.voiceState.set('processing');
    try { this.recorder.stop(); } catch { this.failVoice('La grabación se interrumpió.'); }
  }

  cancelVoice(): void {
    this.generation++;
    this.uploadRequest?.unsubscribe();
    if (this.timer) clearTimeout(this.timer);
    this.timer = undefined;
    if (this.recorder) {
      this.recorder.ondataavailable = null; this.recorder.onstop = null; this.recorder.onerror = null;
      if (this.recorder.state !== 'inactive') try { this.recorder.stop(); } catch { /* already stopped */ }
    }
    this.recorder = undefined;
    this.stopTracks(); this.chunks = []; this.recordedBytes = 0;
    this.voiceState.set(this.voiceConfig?.enabled ? (this.supported() ? 'available' : 'unsupported') : 'disabled');
  }

  private stopTracks(): void {
    this.stream?.getTracks().forEach(track => { track.onended = null; track.stop(); });
    this.stream = undefined;
  }

  private failVoice(message: string): void {
    this.cancelVoice(); this.voiceError.set(message); this.voiceState.set('error');
  }

  onEnter(event: Event): void {
    const keyboard = event as KeyboardEvent;
    if (keyboard.shiftKey || keyboard.isComposing) return;
    keyboard.preventDefault(); this.facade.send();
  }
}
