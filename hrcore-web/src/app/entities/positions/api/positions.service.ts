import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, OperatorFunction, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../access';
import { Position, PositionFilters, PositionRequest, PositionRequirement, PositionRequirementRequest } from '../model/positions.model';
import { parsePositionJson, serializePosition } from '../model/position-salary';

@Injectable({ providedIn: 'root' })
export class PositionsService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/positions`;
  list(page = 0, size = 25, filters: Partial<PositionFilters> = {}, sort = 'name,asc') {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    for (const [key, value] of Object.entries(filters)) if (value?.trim()) params = params.set(key, value.trim());
    return this.http.get(this.api, { params, responseType: 'text' }).pipe(positionResponse<PageResponse<Position>>());
  }
  get(id: string) { return this.http.get(`${this.api}/${id}`, { responseType: 'text' }).pipe(positionResponse<Position>()); }
  create(request: PositionRequest) {
    return this.http.post(this.api, serializePosition(request), { headers: { 'Content-Type': 'application/json' }, responseType: 'text' }).pipe(positionResponse<Position>());
  }
  update(id: string, request: PositionRequest) {
    return this.http.put(`${this.api}/${id}`, serializePosition(request), { headers: { 'Content-Type': 'application/json' }, responseType: 'text' }).pipe(positionResponse<Position>());
  }
  changeStatus(id: string, activate: boolean) {
    return this.http.patch(`${this.api}/${id}/${activate ? 'activate' : 'deactivate'}`, null, { responseType: 'text' }).pipe(positionResponse<Position>());
  }
  requirements(id: string, page = 0) {
    return this.http.get<ApiResponse<PageResponse<PositionRequirement>>>(`${this.api}/${id}/requirements`, {
      params: new HttpParams().set('page', page).set('size', 25).set('sort', 'createdAt,asc')
    }).pipe(map(checked));
  }
  createRequirement(id: string, request: PositionRequirementRequest) {
    return this.http.post<ApiResponse<PositionRequirement>>(`${this.api}/${id}/requirements`, request).pipe(map(checked));
  }
  updateRequirement(id: string, requirementId: string, request: PositionRequirementRequest) {
    return this.http.put<ApiResponse<PositionRequirement>>(`${this.api}/${id}/requirements/${requirementId}`, request).pipe(map(checked));
  }
  deleteRequirement(id: string, requirementId: string) {
    return this.http.delete<ApiResponse<null>>(`${this.api}/${id}/requirements/${requirementId}`).pipe(map(checked));
  }
}
function positionResponse<T>(): OperatorFunction<string, ApiResponse<T>> {
  return source => source.pipe(
    map(json => checked(parsePositionJson<ApiResponse<T>>(json))),
    catchError((error: unknown) => {
      // responseType=text also applies to error bodies; retain the existing API error messages.
      if (error instanceof HttpErrorResponse && typeof error.error === 'string') {
        try {
          const body: unknown = JSON.parse(error.error);
          return throwError(() => new HttpErrorResponse({ error: body, headers: error.headers, status: error.status, statusText: error.statusText, url: error.url ?? undefined }));
        } catch { /* Non-JSON errors keep the existing fallback. */ }
      }
      return throwError(() => error);
    })
  );
}
function checked<T>(response: ApiResponse<T>): ApiResponse<T> {
  if (!response.success) throw new Error(response.message || 'La operación no se completó.');
  return response;
}
