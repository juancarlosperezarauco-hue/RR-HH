import { CommonModule } from '@angular/common';
import { Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AiConversationFacade } from '../../application/ai-conversation.facade';
import { AiConversationChatComponent } from '../ai-conversation-chat/ai-conversation-chat.component';

@Component({
  selector: 'app-ai-conversation-panel', standalone: true,
  imports: [CommonModule, LucideAngularModule, AiConversationChatComponent],
  providers: [AiConversationFacade], templateUrl: './ai-conversation-panel.component.html'
})
export class AiConversationPanelComponent implements OnInit {
  readonly facade = inject(AiConversationFacade);
  private readonly confirmation = viewChild<ElementRef<HTMLDialogElement>>('confirmation');
  ngOnInit(): void { this.facade.initialize(); }
  confirmDelete(): void { this.confirmation()?.nativeElement.showModal(); }
  cancelDelete(): void { this.confirmation()?.nativeElement.close(); }
  deleteConversation(): void { this.cancelDelete(); this.facade.deleteSelected(); }
}
