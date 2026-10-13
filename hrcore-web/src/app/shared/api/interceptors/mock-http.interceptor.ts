import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of, delay } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MOCK_DOSSIERS } from './data/mock-dossiers.data';
import { MOCK_SHIFTS } from './data/mock-shifts.data';
import { MOCK_ATTENDANCE } from './data/mock-attendance.data';
import { MOCK_LEAVES } from './data/mock-leaves.data';

export const mockHttpInterceptor: HttpInterceptorFn = (req, next) => {
  // Si el modo mock no está activo o la petición no es a /api/v1, pasar al backend real
  if (!environment.useMocks || !req.url.includes('/api/v1')) {
    return next(req);
  }

  const url = req.url;
  const method = req.method;
  const now = new Date().toISOString();

  // ============================================================
  // HU15: EXPEDIENTES LABORALES (Dominio 6)
  // ============================================================
  if (url.includes('/document-types') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Catálogo de tipos de documento obtenido con éxito',
        data: MOCK_DOSSIERS.documentTypes,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/documents') && url.includes('/preview') && method === 'GET') {
    const parts = url.split('/');
    const docId = parts[parts.indexOf('preview') - 1] || parts[parts.length - 2];
    MOCK_DOSSIERS.documentAudits.unshift({
      id: 'aud-' + Date.now(),
      documentId: docId,
      userId: '550e8400-e29b-41d4-a716-446655440000',
      userEmail: 'rrhh@hrcore.com',
      action: 'VIEW' as const,
      performedAt: now,
      ipAddress: '190.181.12.45'
    });
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'URL firmada de visualización generada',
        data: {
          downloadUrl: 'https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf',
          expiresInMinutes: 15
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/documents') && url.includes('/audits') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Auditoría de documento obtenida',
        data: MOCK_DOSSIERS.documentAudits,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/documents') && method === 'DELETE') {
    const parts = url.split('/');
    const docId = parts[parts.length - 1];
    const idx = MOCK_DOSSIERS.employeeDocuments.findIndex(d => d.id === docId);
    if (idx !== -1) {
      MOCK_DOSSIERS.employeeDocuments.splice(idx, 1);
      MOCK_DOSSIERS.documentAudits.unshift({
        id: 'aud-' + Date.now(),
        documentId: docId,
        userId: '550e8400-e29b-41d4-a716-446655440000',
        userEmail: 'rrhh@hrcore.com',
        action: 'ARCHIVE' as const,
        performedAt: now,
        ipAddress: '190.181.12.45'
      });
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Documento archivado (soft delete aplicado)',
        data: null,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/documents') && method === 'GET') {
    const urlObj = new URL(req.urlWithParams, 'http://localhost');
    const category = urlObj.searchParams.get('category');
    let filtered = MOCK_DOSSIERS.employeeDocuments;
    if (category && category !== 'ALL') {
      filtered = filtered.filter(d => d.documentCategory === category);
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Documentos del empleado obtenidos',
        data: {
          content: filtered,
          page: 0,
          size: 20,
          totalElements: filtered.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(250));
  }

  if (url.includes('/documents') && method === 'POST') {
    let typeName = 'Documento Legal';
    let cat: any = 'CONTRACT';
    let docNumber = '';
    let fileName = 'Documento_' + Date.now() + '.pdf';
    let issueDate: string | undefined;
    let expDate: string | undefined;

    if (req.body instanceof FormData) {
      const typeId = req.body.get('documentTypeId') as string;
      const foundType = MOCK_DOSSIERS.documentTypes.find(t => t.id === typeId);
      if (foundType) {
        typeName = foundType.name;
        cat = foundType.category;
      }
      docNumber = (req.body.get('documentNumber') as string) || '';
      issueDate = (req.body.get('issueDate') as string) || undefined;
      expDate = (req.body.get('expirationDate') as string) || undefined;
      const file = req.body.get('file') as File;
      if (file && file.name) {
        fileName = file.name;
      }
    }

    const newDoc: any = {
      id: 'doc-' + Date.now(),
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      documentTypeId: (req.body instanceof FormData ? req.body.get('documentTypeId') : '11111111-1111-1111-1111-111111111101') as string,
      documentTypeName: typeName,
      documentCategory: cat,
      documentNumber: docNumber || undefined,
      fileName: fileName,
      filePath: `tenants/uploads/${fileName}`,
      fileSizeBytes: 1048576,
      mimeType: fileName.endsWith('.docx') ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' : (fileName.endsWith('.png') ? 'image/png' : 'application/pdf'),
      versionNumber: 1,
      issueDate: issueDate,
      expirationDate: expDate,
      status: 'ACTIVE' as const,
      createdAt: now,
      updatedAt: now,
      downloadUrl: 'https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf'
    };

    MOCK_DOSSIERS.employeeDocuments.unshift(newDoc);
    MOCK_DOSSIERS.documentAudits.unshift({
      id: 'aud-' + Date.now(),
      documentId: newDoc.id,
      userId: '550e8400-e29b-41d4-a716-446655440000',
      userEmail: 'rrhh@hrcore.com',
      action: 'UPLOAD' as const,
      performedAt: now,
      ipAddress: '190.181.12.45'
    });

    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Documento cargado y registrado en el expediente',
        data: newDoc,
        timestamp: now
      }
    })).pipe(delay(300));
  }

  // ============================================================
  // HU16: GESTIÓN DE TURNOS (Dominio 7)
  // ============================================================
  if (url.includes('/shifts/assignments/employee/') && method === 'GET') {
    const parts = url.split('/');
    const empId = parts[parts.length - 1];
    const assignments = MOCK_SHIFTS.employeeShiftAssignments.filter(a => a.employeeId === empId);
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Asignaciones del empleado obtenidas',
        data: assignments,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts/assignments') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Asignaciones de turno obtenidas',
        data: MOCK_SHIFTS.employeeShiftAssignments,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts/assignments') && method === 'POST') {
    const body = req.body as any;
    const targetShift = MOCK_SHIFTS.shifts.find(s => s.id === body.shiftId);
    const newAssignments: any[] = [];
    const empNames: Record<string, { name: string; code: string }> = {
      '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d': { name: 'Carlos Mamani Quispe', code: 'EMP-00104' },
      '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6e': { name: 'Ana Morales Condori', code: 'EMP-00105' },
      '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6f': { name: 'Roberto Gómez Flores', code: 'EMP-00106' }
    };

    if (Array.isArray(body?.employeeIds)) {
      body.employeeIds.forEach((empId: string) => {
        const emp = empNames[empId] || { name: 'Colaborador Asignado', code: 'EMP-99999' };
        const asg = {
          id: 'asg-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
          tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
          employeeId: empId,
          employeeFullName: emp.name,
          employeeCode: emp.code,
          shiftId: body.shiftId,
          shiftName: targetShift?.name || 'Turno Asignado',
          startDate: body.startDate,
          endDate: body.endDate || null,
          isRecurring: !!body.isRecurring,
          status: 'ACTIVE' as const,
          createdAt: now
        };
        MOCK_SHIFTS.employeeShiftAssignments.unshift(asg);
        newAssignments.push(asg);
      });
    }

    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Turno asignado exitosamente a los colaboradores',
        data: newAssignments,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  if (url.includes('/shifts') && url.includes('/status') && method === 'PATCH') {
    const parts = url.split('/');
    const shiftId = parts[parts.indexOf('status') - 1];
    const target = MOCK_SHIFTS.shifts.find(s => s.id === shiftId);
    if (target) {
      target.status = (req.body as any)?.status || (target.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE');
      target.updatedAt = now;
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Estado del turno actualizado',
        data: target,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts/') && method === 'PUT') {
    const parts = url.split('/');
    const shiftId = parts[parts.length - 1];
    const body = req.body as any;
    const target = MOCK_SHIFTS.shifts.find(s => s.id === shiftId);
    if (target) {
      target.code = body.code || target.code;
      target.name = body.name || target.name;
      target.shiftType = body.shiftType || target.shiftType;
      target.startTime = body.startTime || target.startTime;
      target.endTime = body.endTime || target.endTime;
      target.breakMinutes = body.breakMinutes ?? target.breakMinutes;
      target.gracePeriodMinutes = body.gracePeriodMinutes ?? target.gracePeriodMinutes;
      target.isOvernight = body.isOvernight ?? target.isOvernight;
      target.updatedAt = now;
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Turno actualizado correctamente',
        data: target,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  if (url.includes('/shifts') && method === 'GET') {
    const urlObj = new URL(req.urlWithParams, 'http://localhost');
    const status = urlObj.searchParams.get('status');
    let filtered = MOCK_SHIFTS.shifts;
    if (status && status !== 'ALL') {
      filtered = filtered.filter(s => s.status === status);
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Catálogo de turnos obtenido',
        data: {
          content: filtered,
          page: 0,
          size: 20,
          totalElements: filtered.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts') && method === 'POST') {
    const body = req.body as any;
    const newShift = {
      id: 'shift-' + Date.now(),
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      code: body?.code || 'TUR_NUEVO',
      name: body?.name || 'Turno Nuevo',
      shiftType: body?.shiftType || 'CONTINUOUS',
      startTime: body?.startTime || '08:00:00',
      endTime: body?.endTime || '16:00:00',
      breakMinutes: body?.breakMinutes || 0,
      gracePeriodMinutes: body?.gracePeriodMinutes || 10,
      isOvernight: body?.isOvernight || false,
      status: 'ACTIVE' as const,
      createdAt: now,
      updatedAt: now
    };
    MOCK_SHIFTS.shifts.unshift(newShift);
    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Turno configurado y guardado exitosamente',
        data: newShift,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  // ============================================================
  // HU17: CONTROL DE ASISTENCIA (Dominio 8)
  // ============================================================
  if (url.includes('/attendance/today/') && method === 'GET') {
    const parts = url.split('/');
    const empId = parts[parts.length - 1];
    const todayStr = now.substring(0, 10);
    const existing = MOCK_ATTENDANCE.attendanceRecords.find(r => r.employeeId === empId && r.workDate === todayStr) || null;
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Estado de asistencia del día obtenido',
        data: existing,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/attendance/records') && method === 'GET') {
    const urlObj = new URL(req.urlWithParams, 'http://localhost');
    const empId = urlObj.searchParams.get('employeeId');
    const status = urlObj.searchParams.get('status');
    const startDate = urlObj.searchParams.get('startDate');
    const endDate = urlObj.searchParams.get('endDate');

    let filtered = [...MOCK_ATTENDANCE.attendanceRecords];
    if (empId) {
      filtered = filtered.filter(r => r.employeeId === empId);
    }
    if (status && status !== 'ALL') {
      filtered = filtered.filter(r => r.status === status);
    }
    if (startDate) {
      filtered = filtered.filter(r => r.workDate >= startDate);
    }
    if (endDate) {
      filtered = filtered.filter(r => r.workDate <= endDate);
    }

    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Registros de asistencia obtenidos',
        data: {
          content: filtered,
          page: 0,
          size: 20,
          totalElements: filtered.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(250));
  }

  if (url.includes('/attendance/clock') && method === 'POST') {
    const body = req.body as any;
    const empId = body?.employeeId || '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d';
    const punchType = body?.type || 'IN';
    const todayStr = now.substring(0, 10);

    const empCatalog: Record<string, { name: string; code: string }> = {
      '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d': { name: 'Carlos Mamani Quispe', code: 'EMP-00104' },
      '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c': { name: 'Valeria Rios Salazar', code: 'EMP-00105' },
      '7b3dfc4d-5c7d-5dcf-9bdd-3b1d8c4dcb7e': { name: 'Rodrigo Flores Condori', code: 'EMP-00106' }
    };
    const empInfo = empCatalog[empId] || { name: 'Carlos Mamani Quispe', code: 'EMP-00104' };

    let record: any = MOCK_ATTENDANCE.attendanceRecords.find(r => r.employeeId === empId && r.workDate === todayStr);

    if (!record) {
      const currentHour = new Date().getHours();
      const currentMinute = new Date().getMinutes();
      const isLate = punchType === 'IN' && (currentHour > 8 || (currentHour === 8 && currentMinute > 40));

      record = {
        id: 'rec-' + Date.now(),
        tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
        employeeId: empId,
        employeeFullName: empInfo.name,
        employeeCode: empInfo.code,
        shiftId: '22222222-2222-2222-2222-222222222201',
        workDate: todayStr,
        checkInTime: punchType === 'IN' ? now : '2026-10-12T08:30:00Z',
        checkOutTime: punchType === 'OUT' ? now : null,
        workedHours: punchType === 'OUT' ? 8.0 : 0.0,
        overtimeHours: 0.0,
        lateMinutes: isLate ? 18 : 0,
        earlyDepartureMinutes: 0,
        status: isLate ? ('LATE' as const) : ('PRESENT' as const),
        source: body?.method || ('WEB' as const),
        latitude: body?.latitude ?? -16.5002450,
        longitude: body?.longitude ?? -68.1325400,
        createdAt: now,
        updatedAt: now
      };
      MOCK_ATTENDANCE.attendanceRecords.unshift(record);
    } else {
      if (punchType === 'IN') {
        record.checkInTime = now;
      } else {
        record.checkOutTime = now;
        record.workedHours = 8.0;
      }
      record.updatedAt = now;
    }

    // Registrar marcación individual
    MOCK_ATTENDANCE.attendancePunches.unshift({
      id: 'pnc-' + Date.now(),
      attendanceRecordId: record.id,
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: empId,
      punchTime: now,
      punchType: punchType as 'IN' | 'OUT',
      source: body?.method || ('WEB' as const),
      deviceId: 'BROWSER-NAVIGATOR',
      latitude: body?.latitude ?? -16.5002450,
      longitude: body?.longitude ?? -68.1325400,
      createdAt: now
    });

    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Marcación sellada con sello de tiempo del servidor',
        data: record,
        timestamp: now
      }
    })).pipe(delay(350));
  }

  if (url.includes('/attendance/corrections') && method === 'POST') {
    const body = req.body as any;
    if (!body?.reason || body.reason.trim().length < 10) {
      return of(new HttpResponse({
        status: 400,
        body: {
          success: false,
          message: 'La justificación es obligatoria y debe tener al menos 10 caracteres.',
          timestamp: now
        }
      })).pipe(delay(200));
    }

    const targetRec = MOCK_ATTENDANCE.attendanceRecords.find(r => r.id === body.attendanceRecordId);
    if (targetRec) {
      if (body.correctedCheckIn) {
        targetRec.checkInTime = body.correctedCheckIn;
        targetRec.status = 'PRESENT';
        targetRec.lateMinutes = 0;
      }
      if (body.correctedCheckOut) {
        targetRec.checkOutTime = body.correctedCheckOut;
      }
      targetRec.updatedAt = now;
    }

    const correction: any = {
      id: 'cor-' + Date.now(),
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      attendanceRecordId: body?.attendanceRecordId || '33333333-3333-3333-3333-333333333301',
      requestedBy: '550e8400-e29b-41d4-a716-446655440000',
      approvedBy: '550e8400-e29b-41d4-a716-446655440000',
      originalCheckIn: targetRec?.checkInTime || null,
      correctedCheckIn: body?.correctedCheckIn || null,
      originalCheckOut: targetRec?.checkOutTime || null,
      correctedCheckOut: body?.correctedCheckOut || null,
      reason: body?.reason,
      decisionNotes: 'Regularización aprobada y aplicada automáticamente en modo demostración.',
      status: 'APPROVED' as const,
      createdAt: now,
      decidedAt: now
    };
    MOCK_ATTENDANCE.attendanceCorrections.unshift(correction);

    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Regularización de asistencia enviada y aprobada en auditoría',
        data: correction,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  // ============================================================
  // HU18: AUSENCIAS Y VACACIONES (Dominio 9)
  // ============================================================
  if (url.includes('/leaves/types') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Tipos de ausencia obtenidos',
        data: MOCK_LEAVES.leaveTypes,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/leaves/balances') && method === 'GET') {
    const parts = url.split('/');
    const empId = parts[parts.length - 1];
    const match = MOCK_LEAVES.leaveBalances.find(b => b.employeeId === empId) || MOCK_LEAVES.leaveBalances[0];
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Saldo legal de vacaciones obtenido',
        data: match,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/leaves/requests') && url.includes('/review') && method === 'PATCH') {
    const parts = url.split('/');
    const reqId = parts[parts.indexOf('review') - 1];
    const body = req.body as any;
    const approved = !!body?.approved;
    const rejectionReason = (body?.rejectionReason || '').trim();

    if (!approved && rejectionReason.length < 10) {
      return of(new HttpResponse({
        status: 400,
        body: {
          success: false,
          message: 'El motivo de rechazo es obligatorio (mínimo 10 caracteres).',
          timestamp: now
        }
      })).pipe(delay(200));
    }

    const targetReq = MOCK_LEAVES.leaveRequests.find(r => r.id === reqId);
    if (targetReq) {
      targetReq.status = approved ? 'APPROVED' : 'REJECTED';
      targetReq.rejectionReason = approved ? null : rejectionReason;
      targetReq.reviewedBy = '550e8400-e29b-41d4-a716-446655440000';
      targetReq.reviewedAt = now;

      // Sincronización con Saldo de Vacación
      if (approved && targetReq.leaveTypeId === '44444444-4444-4444-4444-444444444401') {
        const bal = MOCK_LEAVES.leaveBalances.find(b => b.employeeId === targetReq.employeeId);
        if (bal) {
          bal.usedDays += targetReq.totalDays;
          bal.pendingDays = Math.max(0, bal.pendingDays - targetReq.totalDays);
          bal.remainingDays = Math.max(0, bal.entitledDays - bal.usedDays);
          bal.updatedAt = now;
        }
      }

      // Sincronización con Asistencia HU17: Marcar días como EXCUSED
      if (approved) {
        const start = new Date(targetReq.startDate + 'T00:00:00');
        const end = new Date(targetReq.endDate + 'T00:00:00');
        for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
          const dStr = d.toISOString().substring(0, 10);
          const attRec = MOCK_ATTENDANCE.attendanceRecords.find(a => a.employeeId === targetReq.employeeId && a.workDate === dStr);
          if (attRec) {
            attRec.status = 'EXCUSED';
            attRec.updatedAt = now;
          } else {
            MOCK_ATTENDANCE.attendanceRecords.unshift({
              id: 'att-excused-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
              tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
              employeeId: targetReq.employeeId,
              employeeFullName: targetReq.employeeFullName,
              employeeCode: 'EMP-00104',
              workDate: dStr,
              checkInTime: null,
              checkOutTime: null,
              workedHours: 8.0,
              overtimeHours: 0,
              lateMinutes: 0,
              earlyDepartureMinutes: 0,
              status: 'EXCUSED',
              source: 'MANUAL',
              createdAt: now,
              updatedAt: now
            });
          }
        }
      }
    }

    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: approved ? 'Solicitud aprobada y sincronizada con asistencia y saldos' : 'Solicitud rechazada',
        data: targetReq,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  if (url.includes('/leaves/requests') && method === 'GET') {
    const urlObj = new URL(req.urlWithParams, 'http://localhost');
    const empId = urlObj.searchParams.get('employeeId');
    const status = urlObj.searchParams.get('status');

    let filtered = [...MOCK_LEAVES.leaveRequests];
    if (empId) {
      filtered = filtered.filter(r => r.employeeId === empId);
    }
    if (status && status !== 'ALL') {
      filtered = filtered.filter(r => r.status === status);
    }

    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Solicitudes de ausencia obtenidas',
        data: {
          content: filtered,
          page: 0,
          size: 20,
          totalElements: filtered.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(250));
  }

  if (url.includes('/leaves/requests') && method === 'POST') {
    let empId = '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d';
    let leaveTypeId = '44444444-4444-4444-4444-444444444401';
    let startDate = '2026-11-02';
    let endDate = '2026-11-04';
    let reason = 'Solicitud de permiso';
    let proofFile: File | null = null;

    if (req.body instanceof FormData) {
      empId = (req.body.get('employeeId') as string) || empId;
      leaveTypeId = (req.body.get('leaveTypeId') as string) || leaveTypeId;
      startDate = (req.body.get('startDate') as string) || startDate;
      endDate = (req.body.get('endDate') as string) || endDate;
      reason = (req.body.get('reason') as string) || reason;
      proofFile = (req.body.get('proofDocument') as File) || null;
    } else if (typeof req.body === 'object' && req.body !== null) {
      const b = req.body as any;
      empId = b.employeeId || empId;
      leaveTypeId = b.leaveTypeId || leaveTypeId;
      startDate = b.startDate || startDate;
      endDate = b.endDate || endDate;
      reason = b.reason || reason;
    }

    const typeObj = MOCK_LEAVES.leaveTypes.find(t => t.id === leaveTypeId);
    if (typeObj?.requiresAttachment && !proofFile) {
      return of(new HttpResponse({
        status: 400,
        body: {
          success: false,
          message: 'Este tipo de ausencia requiere adjuntar un documento probatorio obligatorio.',
          timestamp: now
        }
      })).pipe(delay(200));
    }

    // Calcular días hábiles
    let businessDays = 0;
    const cur = new Date(startDate + 'T00:00:00');
    const end = new Date(endDate + 'T00:00:00');
    while (cur <= end) {
      const day = cur.getDay();
      if (day !== 0 && day !== 6) {
        businessDays++;
      }
      cur.setDate(cur.getDate() + 1);
    }
    businessDays = Math.max(1, businessDays);

    // Integración HU15: Si hay archivo, guardarlo en expedientes como categoría HEALTH o LEGAL
    let docUrl: string | null = null;
    if (proofFile) {
      docUrl = 'https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf';
      MOCK_DOSSIERS.employeeDocuments.unshift({
        id: 'doc-leave-' + Date.now(),
        tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
        employeeId: empId,
        documentTypeId: '11111111-1111-1111-1111-111111111103',
        documentTypeName: 'Comprobante de Baja Médica',
        documentCategory: 'HEALTH',
        fileName: proofFile.name || 'comprobante_baja_medica.pdf',
        filePath: `tenants/uploads/${proofFile.name || 'comprobante_baja.pdf'}`,
        fileSizeBytes: proofFile.size || 524288,
        mimeType: 'application/pdf',
        versionNumber: 1,
        status: 'ACTIVE',
        createdAt: now,
        updatedAt: now,
        downloadUrl: docUrl
      } as any);
    }

    const empCatalog: Record<string, string> = {
      '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d': 'Carlos Mamani Quispe',
      '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c': 'Valeria Rios Salazar',
      '7b3dfc4d-5c7d-5dcf-9bdd-3b1d8c4dcb7e': 'Rodrigo Flores Condori'
    };

    const newReq = {
      id: 'req-' + Date.now(),
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: empId,
      employeeFullName: empCatalog[empId] || 'Carlos Mamani Quispe',
      leaveTypeId: leaveTypeId,
      leaveTypeName: typeObj?.name || 'Permiso Laboral',
      startDate: startDate,
      endDate: endDate,
      totalDays: businessDays,
      isHourly: false,
      startTime: null,
      endTime: null,
      totalHours: null,
      reason: reason,
      attachmentUrl: docUrl,
      status: 'PENDING' as const,
      reviewedBy: null,
      reviewedAt: null,
      rejectionReason: null,
      createdAt: now
    };

    MOCK_LEAVES.leaveRequests.unshift(newReq);

    // Si es vacación, reflejar en días pendientes
    if (leaveTypeId === '44444444-4444-4444-4444-444444444401') {
      const bal = MOCK_LEAVES.leaveBalances.find(b => b.employeeId === empId);
      if (bal) {
        bal.pendingDays += businessDays;
        bal.updatedAt = now;
      }
    }

    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Solicitud de ausencia enviada para revisión',
        data: newReq,
        timestamp: now
      }
    })).pipe(delay(300));
  }

  if (url.includes('/holidays') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Feriados oficiales obtenidos',
        data: MOCK_LEAVES.holidays,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  // Dejar pasar al backend si ninguna ruta coincide
  return next(req);
};
