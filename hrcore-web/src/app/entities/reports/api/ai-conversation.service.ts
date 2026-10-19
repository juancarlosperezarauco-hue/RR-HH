import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import { AiConversation, AiMessage, AiMessageRequest, AiReportRefresh, AiTurn } from '../model/ai-conversation.model';

type SpringPage<T> = Omit<PageResponse<T>, 'page'> & { number: number };
function checked<T>(response: ApiResponse<T>): ApiResponse<T> {
  if (!response.success) throw new Error(response.message || 'La operación no se completó.');
  return response;
}
function paginated<T>(response: ApiResponse<SpringPage<T>>): ApiResponse<PageResponse<T>> {
  checked(response);
  return { ...response, data: { ...response.data, page: response.data.number } };
}

@Injectable({ providedIn: 'root' })
export class AiConversationService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/hr/ai/conversations`;
  create() { return this.http.post<ApiResponse<AiConversation>>(this.api, null).pipe(map(checked)); }
  list(page = 0, size = 20) {
    return this.http.get<ApiResponse<SpringPage<AiConversation>>>(this.api, {
      params: new HttpParams().set('page', page).set('size', size)
    }).pipe(map(paginated));
  }
  get(id: string) { return this.http.get<ApiResponse<AiConversation>>(`${this.api}/${encodeURIComponent(id)}`).pipe(map(checked)); }
  messages(id: string, page = 0, size = 50) {
    return this.http.get<ApiResponse<SpringPage<AiMessage>>>(`${this.api}/${encodeURIComponent(id)}/messages`, {
      params: new HttpParams().set('page', page).set('size', size)
    }).pipe(map(paginated));
  }
  delete(id: string) { return this.http.delete<ApiResponse<null>>(`${this.api}/${encodeURIComponent(id)}`).pipe(map(checked)); }
  send(id: string, request: AiMessageRequest) {
    return this.http.post<ApiResponse<AiTurn>>(`${this.api}/${encodeURIComponent(id)}/messages`, request).pipe(map(checked));
  }
  refresh(id: string, runId: string, page?: number, size?: number) {
    let params = new HttpParams();
    if (page !== undefined) params = params.set('page', page);
    if (size !== undefined) params = params.set('size', size);
    return this.http.post<ApiResponse<AiReportRefresh>>(`${this.api}/${encodeURIComponent(id)}/reports/${encodeURIComponent(runId)}/refresh`, null, { params }).pipe(map(checked));
  }
}
