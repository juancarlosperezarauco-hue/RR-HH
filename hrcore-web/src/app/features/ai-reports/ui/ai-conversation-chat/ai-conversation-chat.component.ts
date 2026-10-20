import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AI_REPORT_QUESTION_MAX_LENGTH, AiConversationFacade } from '../../application/ai-conversation.facade';
import { AiMessageType, AiReportExportFormat } from '../../../../entities/reports/model/ai-conversation.model';
import { AiReportResultComponent } from '../ai-report-result/ai-report-result.component';

@Component({ selector: 'app-ai-conversation-chat', standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, AiReportResultComponent], templateUrl: './ai-conversation-chat.component.html' })
export class AiConversationChatComponent {
  readonly facade = inject(AiConversationFacade);
  readonly maxQuestionLength = AI_REPORT_QUESTION_MAX_LENGTH;
  readonly formats: { format: AiReportExportFormat; label: string }[] = [
    { format: 'csv', label: 'CSV' }, { format: 'xlsx', label: 'Excel' }, { format: 'pdf', label: 'PDF' }
  ];
  readonly labels: Record<AiMessageType, string> = { TEXT: 'Pregunta', REPORT: 'Reporte', CLARIFICATION: 'Aclaración necesaria', UNSUPPORTED: 'Solicitud no soportada', ERROR: 'Consulta no completada' };
  get characterCount(): number { return [...this.facade.draft()].length; }
  onEnter(event: Event): void {
    const keyboard = event as KeyboardEvent;
    if (keyboard.shiftKey || keyboard.isComposing) return;
    keyboard.preventDefault(); this.facade.send();
  }
}
