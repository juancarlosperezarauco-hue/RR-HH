export interface AiConversation {
  id: string; title: string; status: string; createdAt: string; updatedAt: string;
  lastMessageAt: string | null; version: number;
}
export type AiMessageType = 'TEXT' | 'REPORT' | 'CLARIFICATION' | 'UNSUPPORTED' | 'ERROR';
export interface AiMessage {
  id: string; role: 'USER' | 'ASSISTANT'; content: string; messageType: AiMessageType;
  state: 'PENDING' | 'COMPLETED' | 'FAILED'; createdAt: string; completedAt: string | null;
  reportRunId?: string | null;
}
export interface AiMessageRequest { clientMessageId: string; message: string; }
export type AiTurnStatus = 'ok' | 'needs_clarification' | 'unsupported' | 'pending' | 'error';
export type ReportValue = string | number | boolean | null;
export interface ReportColumn { key: string; type: 'UUID' | 'TEXT' | 'DATE' | 'LOCAL_TIME' | 'ENUM' | 'INTEGER'; }
export interface QueryResult {
  columns: ReportColumn[]; rows: Record<string, ReportValue>[];
  pageNumber: number; pageSize: number; hasMore: boolean; warnings: string[];
}
export interface AiTurn {
  conversationId: string; request: AiMessage; response?: AiMessage | null; status: AiTurnStatus;
  reportRunId?: string | null; result?: QueryResult | null;
}
export interface AiReportRefresh {
  reportRunId: string; refreshedFromRunId: string; refreshedAt: string; result: QueryResult;
}
export interface AiReportView { reportRunId: string; result: QueryResult; executedAt: string | null; }
export type AiReportExportFormat = 'csv' | 'xlsx' | 'pdf';
