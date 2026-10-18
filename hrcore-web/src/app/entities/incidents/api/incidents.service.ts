import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import { CreateIncidentInput, IncidentAction, IncidentHistory, IncidentResponse, IncidentSeverity, IncidentStatus, IncidentType } from '../model/incident.model';
@Injectable({providedIn:'root'})
export class IncidentsService {
  private readonly http=inject(HttpClient); private readonly url=`${environment.apiUrl}/api/incidents`;
  list(filters?:{employeeId?:string; status?:IncidentStatus; incidentType?:IncidentType; severity?:IncidentSeverity},page=0,size=25):Observable<ApiResponse<PageResponse<IncidentResponse>>>{let p=new HttpParams().set('page',page).set('size',size);Object.entries(filters??{}).forEach(([k,v])=>{if(v)p=p.set(k,v)});return this.http.get<ApiResponse<PageResponse<IncidentResponse>>>(this.url,{params:p});}
  create(input:CreateIncidentInput):Observable<ApiResponse<IncidentResponse>>{return this.http.post<ApiResponse<IncidentResponse>>(this.url,input);}
  detail(id:string):Observable<ApiResponse<IncidentResponse>>{return this.http.get<ApiResponse<IncidentResponse>>(`${this.url}/${id}`);}
  startReview(id:string){return this.http.post<ApiResponse<IncidentResponse>>(`${this.url}/${id}/start-review`,{});}
  resolve(id:string,comment:string){return this.http.patch<ApiResponse<IncidentResponse>>(`${this.url}/${id}/resolve`,{comment});}
  close(id:string,comment?:string){return this.http.patch<ApiResponse<IncidentResponse>>(`${this.url}/${id}/close`,comment?{comment}:{});}
  cancel(id:string,comment?:string){return this.http.patch<ApiResponse<IncidentResponse>>(`${this.url}/${id}/cancel`,comment?{comment}:{});}
  assign(id:string,assignedTo:string){return this.http.patch<ApiResponse<IncidentResponse>>(`${this.url}/${id}/assign`,{assignedTo});}
  employeeResponse(id:string,response:string){return this.http.patch<ApiResponse<IncidentResponse>>(`${this.url}/${id}/employee-response`,{response});}
  actions(id:string){return this.http.get<ApiResponse<IncidentAction[]>>(`${this.url}/${id}/actions`);}
  history(id:string){return this.http.get<ApiResponse<IncidentHistory[]>>(`${this.url}/${id}/history`);}
  addAction(id:string,input:{actionType:string;description:string;actionDate:string;responsibleUserId?:string|null;notes?:string|null}){return this.http.post<ApiResponse<IncidentAction>>(`${this.url}/${id}/actions`,input);}
  startAction(id:string,actionId:string){return this.http.patch<ApiResponse<IncidentAction>>(`${this.url}/${id}/actions/${actionId}/start`,{});}
  completeAction(id:string,actionId:string){return this.http.patch<ApiResponse<IncidentAction>>(`${this.url}/${id}/actions/${actionId}/complete`,{});}
  cancelAction(id:string,actionId:string){return this.http.patch<ApiResponse<IncidentAction>>(`${this.url}/${id}/actions/${actionId}/cancel`,{});}
}
