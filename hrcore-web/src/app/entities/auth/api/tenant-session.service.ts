import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { AuthSessionResponse, SessionMutationResponse } from '../model/auth-session-response.model';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
@Injectable({ providedIn: 'root' })
export class TenantSessionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/auth/sessions`;
  list(): Observable<AuthSessionResponse[]> { return this.http.get<ApiResponse<AuthSessionResponse[]>>(this.baseUrl).pipe(map(response => this.unwrap(response))); }
  revoke(sessionId: string): Observable<void> { return this.http.delete<ApiResponse<SessionMutationResponse>>(`${this.baseUrl}/${this.validId(sessionId)}`).pipe(map(response => { this.unwrapMutation(response); })); }
  revokeOthers(): Observable<void> { return this.http.post<ApiResponse<SessionMutationResponse>>(this.baseUrl + '/revoke-others', {}).pipe(map(response => { this.unwrapMutation(response); })); }
  private validId(id: string): string { if (!UUID.test(id)) throw new Error('Identificador de sesión inválido'); return encodeURIComponent(id); }
  private unwrap(response: ApiResponse<AuthSessionResponse[]>): AuthSessionResponse[] { if (!response.success || !Array.isArray(response.data)) throw new Error(response.message || 'No se pudieron cargar las sesiones'); return response.data; }
  private unwrapMutation(response: ApiResponse<SessionMutationResponse>): SessionMutationResponse { if (!response.success || !response.data?.status) throw new Error(response.message || 'No se pudo completar la operación'); return response.data; }
}
