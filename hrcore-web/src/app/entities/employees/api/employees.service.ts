import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../access';
import {
  EmergencyContact, EmergencyContactRequest, Employee, EmployeeAssignment,
  EmployeeAssignmentRequest, EmployeeOnboardingRequest, EmployeeOnboardingResponse,
  EmployeeRequest, EmployeeSummary, EmploymentStatus, EmploymentPeriod, EmployeeTermination,
  RehireEmployeeRequest, RehireEmployeeResponse
} from '../model/employees.model';

@Injectable({ providedIn: 'root' })
export class EmployeesService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/employees`;

  list(page = 0, size = 25, filters: { name?: string; employeeNumber?: string; status?: string } = {}) {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'lastName,asc');
    if (filters.name?.trim()) params = params.set('name', filters.name.trim());
    if (filters.employeeNumber?.trim()) params = params.set('employeeNumber', filters.employeeNumber.trim());
    if (filters.status) params = params.set('status', filters.status);
    return this.http.get<ApiResponse<PageResponse<EmployeeSummary>>>(this.api, { params });
  }
  get(id: string) { return this.http.get<ApiResponse<Employee>>(`${this.api}/${id}`); }
  create(request: EmployeeRequest) { return this.http.post<ApiResponse<Employee>>(this.api, request); }
  createWithAssignment(request: EmployeeOnboardingRequest) {
    return this.http.post<ApiResponse<EmployeeOnboardingResponse>>(`${this.api}/with-assignment`, request);
  }
  update(id: string, request: EmployeeRequest) { return this.http.put<ApiResponse<Employee>>(`${this.api}/${id}`, request); }
  changeStatus(id: string, status: EmploymentStatus, reason: string) {
    return this.http.patch<ApiResponse<Employee>>(`${this.api}/${id}/status`, { status, reason });
  }
  assignments(employeeId: string) {
    return this.http.get<ApiResponse<PageResponse<EmployeeAssignment>>>(`${this.api}/${employeeId}/assignments`, {
      params: new HttpParams().set('page', 0).set('size', 100).set('sort', 'startDate,desc')
    });
  }
  createAssignment(employeeId: string, request: EmployeeAssignmentRequest) {
    return this.http.post<ApiResponse<EmployeeAssignment>>(`${this.api}/${employeeId}/assignments`, request);
  }
  closeAssignment(employeeId: string, assignmentId: string, endDate: string) {
    return this.http.patch<ApiResponse<EmployeeAssignment>>(`${this.api}/${employeeId}/assignments/${assignmentId}/close`, { endDate });
  }
  cancelAssignment(employeeId: string, assignmentId: string) {
    return this.http.patch<ApiResponse<EmployeeAssignment>>(`${this.api}/${employeeId}/assignments/${assignmentId}/cancel`, {});
  }
  transfer(employeeId: string, request: { departmentId: string; positionId: string; supervisorEmployeeId: string | null; effectiveDate: string; reason: string }) {
    return this.http.post<ApiResponse<unknown>>(`${this.api}/${employeeId}/transfer`, request);
  }
  terminate(employeeId: string, request: { effectiveDate: string; reason: string }) {
    return this.http.post<ApiResponse<unknown>>(`${this.api}/${employeeId}/terminate`, request);
  }
  termination(employeeId: string) {
    return this.http.get<ApiResponse<EmployeeTermination>>(`${this.api}/${employeeId}/termination`);
  }
  periods(employeeId: string) {
    return this.http.get<ApiResponse<EmploymentPeriod[]>>(`${this.api}/${employeeId}/employment-periods`);
  }
  period(employeeId: string, periodId: string) {
    return this.http.get<ApiResponse<EmploymentPeriod>>(`${this.api}/${employeeId}/employment-periods/${periodId}`);
  }
  terminations(employeeId: string) {
    return this.http.get<ApiResponse<EmployeeTermination[]>>(`${this.api}/${employeeId}/terminations`);
  }
  terminationForPeriod(employeeId: string, periodId: string) {
    return this.http.get<ApiResponse<EmployeeTermination>>(`${this.api}/${employeeId}/employment-periods/${periodId}/termination`);
  }
  rehire(employeeId: string, request: RehireEmployeeRequest) {
    return this.http.post<ApiResponse<RehireEmployeeResponse>>(`${this.api}/${employeeId}/rehire`, request);
  }
  getAssignment(employeeId: string, assignmentId: string) {
    return this.http.get<ApiResponse<EmployeeAssignment>>(`${this.api}/${employeeId}/assignments/${assignmentId}`);
  }
  updateAssignment(employeeId: string, assignmentId: string, request: EmployeeAssignmentRequest) {
    return this.http.put<ApiResponse<EmployeeAssignment>>(`${this.api}/${employeeId}/assignments/${assignmentId}`, request);
  }
  contacts(employeeId: string) {
    return this.http.get<ApiResponse<PageResponse<EmergencyContact>>>(`${this.api}/${employeeId}/emergency-contacts`, {
      params: new HttpParams().set('page', 0).set('size', 100).set('sort', 'fullName,asc')
    });
  }
  createContact(employeeId: string, request: EmergencyContactRequest) {
    return this.http.post<ApiResponse<EmergencyContact>>(`${this.api}/${employeeId}/emergency-contacts`, request);
  }
  getContact(employeeId: string, contactId: string) {
    return this.http.get<ApiResponse<EmergencyContact>>(`${this.api}/${employeeId}/emergency-contacts/${contactId}`);
  }
  updateContact(employeeId: string, contactId: string, request: EmergencyContactRequest) {
    return this.http.put<ApiResponse<EmergencyContact>>(`${this.api}/${employeeId}/emergency-contacts/${contactId}`, request);
  }
  deleteContact(employeeId: string, contactId: string) {
    return this.http.delete<ApiResponse<null>>(`${this.api}/${employeeId}/emergency-contacts/${contactId}`);
  }
}
