import { HttpInterceptorFn, HttpResponse, HttpHeaders } from '@angular/common/http';
import { of, delay } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MOCK_DOSSIERS } from './data/mock-dossiers.data';
import { MOCK_SHIFTS } from './data/mock-shifts.data';
import { MOCK_ATTENDANCE } from './data/mock-attendance.data';
import { MOCK_LEAVES } from './data/mock-leaves.data';

export const mockHttpInterceptor: HttpInterceptorFn = (req, next) => {
  // Si el modo mock no está activo o la petición no pertenece a las APIs simuladas, pasar al backend real
  if (!environment.useMocks || (
    !req.url.includes('/api/v1') &&
    !req.url.includes('/api/employee-files') &&
    !req.url.includes('/api/shifts') &&
    !req.url.includes('/api/attendance') &&
    !req.url.includes('/api/leaves') &&
    !req.url.includes('/attendance') &&
    !req.url.includes('/leaves') &&
    !req.url.includes('/holidays') &&
    !req.url.includes('/api/auth') &&
    !req.url.includes('/api/users') &&
    !req.url.includes('/api/me') &&
    !req.url.includes('/notifications')
  )) {
    return next(req);
  }

  const url = req.url;
  const method = req.method;
  const now = new Date().toISOString();

  // ============================================================
  // AUTENTICACIÓN & SESIÓN MOCK
  // ============================================================
  if (url.includes('/api/auth/login') && method === 'POST') {
    const mockToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJ0ZW5hbnQiOiJkZW1vIiwicm9sZXMiOlsiQURNSU4iLCJPV05FUl9BRE1JTiJdLCJwZXJtaXNzaW9ucyI6WyJ1c2Vycy5saXN0IiwiYWNjZXNzLnJvbGVzLnJlYWQiLCJhY2Nlc3MucGVybWlzc2lvbnMucmVhZCIsImJpbGxpbmcuc3Vic2NyaXB0aW9uLm1hbmFnZSIsImJhY2t1cHMubGlzdCIsImNvbnRyYWN0cy5saXN0Il0sInRva2VuX3R5cGUiOiJCRUFSRVIiLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MjUwMDAwMDAwMH0.mock_signature';
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Sesión iniciada con éxito',
        data: {
          accessToken: mockToken,
          refreshToken: 'mock-refresh-token',
          tokenType: 'BEARER',
          expiresIn: 86400
        },
        timestamp: now
      }
    })).pipe(delay(150));
  }

  if (url.includes('/api/auth/me') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Usuario obtenido',
        data: {
          id: '550e8400-e29b-41d4-a716-446655440000',
          email: 'admin@demo.com',
          firstName: 'Admin',
          lastName: 'HR Core',
          active: true,
          status: 'ACTIVE',
          tenantSlug: 'demo',
          roles: ['ADMIN', 'OWNER_ADMIN'],
          profilePhotoUrl: null,
          profilePhotoContentType: null,
          updatedAt: now
        },
        timestamp: now
      }
    })).pipe(delay(100));
  }

  if (url.includes('/api/auth/profile') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Perfil obtenido',
        data: {
          id: '550e8400-e29b-41d4-a716-446655440000',
          email: 'admin@demo.com',
          firstName: 'Admin',
          lastName: 'HR Core',
          active: true,
          status: 'ACTIVE',
          tenantSlug: 'demo',
          profilePhotoAvailable: false,
          profilePhotoUrl: null,
          profilePhotoContentType: null,
          updatedAt: now
        },
        timestamp: now
      }
    })).pipe(delay(100));
  }

  if (url.includes('/api/users') && method === 'GET') {
    const mockUsers = [
      { id: 1, email: 'carlos.mamani@empresa.com', firstName: 'Carlos', lastName: 'Mamani Quispe', active: true, status: 'ACTIVE' },
      { id: 2, email: 'valeria.rios@empresa.com', firstName: 'Valeria', lastName: 'Rios Salazar', active: true, status: 'ACTIVE' },
      { id: 3, email: 'rodrigo.flores@empresa.com', firstName: 'Rodrigo', lastName: 'Flores Condori', active: true, status: 'ACTIVE' }
    ];
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Usuarios obtenidos',
        data: {
          content: mockUsers,
          pageable: { pageNumber: 0, pageSize: 20 },
          totalPages: 1,
          totalElements: mockUsers.length,
          last: true,
          size: 20,
          number: 0,
          numberOfElements: mockUsers.length,
          first: true,
          empty: false
        },
        timestamp: now
      }
    })).pipe(delay(150));
  }

  // ============================================================
  // NOTIFICACIONES MOCK (/api/me/notifications)
  // ============================================================
  if (url.includes('/notifications/unread-count') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Conteo de notificaciones no leídas obtenido',
        data: {
          unreadCount: 2
        },
        timestamp: now
      }
    })).pipe(delay(100));
  }

  if (url.includes('/notifications') && method === 'GET') {
    const mockNotifications = [
      {
        id: 'notif-1',
        userId: '550e8400-e29b-41d4-a716-446655440000',
        type: 'ATTENDANCE_ADJUSTMENT',
        category: 'SYSTEM',
        priority: 'MEDIUM',
        status: 'UNREAD',
        title: 'Regularización de Asistencia aprobada',
        body: 'Tu solicitud de regularización para el día 13 de Octubre fue aprobada por RRHH.',
        data: null,
        imageUrl: null,
        actionUrl: '/dashboard/attendance',
        readAt: null,
        openedAt: null,
        archivedAt: null,
        expiresAt: null,
        createdAt: new Date(Date.now() - 3600000).toISOString(),
        updatedAt: now
      },
      {
        id: 'notif-2',
        userId: '550e8400-e29b-41d4-a716-446655440000',
        type: 'LEAVE_REQUEST',
        category: 'SYSTEM',
        priority: 'LOW',
        status: 'UNREAD',
        title: 'Saldo de Vacaciones LGT actualizado',
        body: 'Se ha sincronizado tu balance anual de vacaciones según tu antigüedad legal.',
        data: null,
        imageUrl: null,
        actionUrl: '/dashboard/absences',
        readAt: null,
        openedAt: null,
        archivedAt: null,
        expiresAt: null,
        createdAt: new Date(Date.now() - 86400000).toISOString(),
        updatedAt: now
      }
    ];

    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Notificaciones obtenidas con éxito',
        data: {
          content: mockNotifications,
          pageable: { pageNumber: 0, pageSize: 20 },
          totalPages: 1,
          totalElements: mockNotifications.length,
          last: true,
          size: 20,
          number: 0,
          numberOfElements: mockNotifications.length,
          first: true,
          empty: false
        },
        timestamp: now
      }
    })).pipe(delay(120));
  }

  if (url.includes('/notifications') && method === 'PATCH') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Notificación procesada con éxito',
        data: null,
        timestamp: now
      }
    })).pipe(delay(100));
  }

  // ============================================================
  // HU15: EXPEDIENTES LABORALES (Dominio 6)
  // ============================================================
  if ((url.includes('/employee-files/employee/') || url.includes('/employee-files/by-employee/')) && method === 'GET') {
    const parts = url.split('/');
    const empId = parts[parts.length - 1].split('?')[0];
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Expediente digital obtenido',
        data: {
          id: 'ef-' + empId,
          employeeId: empId,
          fileNumber: 'EXP-' + empId.substring(0, 8).toUpperCase(),
          status: 'ACTIVE',
          notes: 'Expediente principal de colaborador',
          createdAt: '2026-01-15T08:00:00Z',
          updatedAt: now
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/employee-files') && !url.includes('/documents') && method === 'POST') {
    const body = req.body as any;
    const empId = body?.employeeId || '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d';
    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Expediente digital creado',
        data: {
          id: 'ef-' + empId,
          employeeId: empId,
          fileNumber: body?.fileNumber || 'EXP-' + empId.substring(0, 8).toUpperCase(),
          status: 'ACTIVE',
          notes: body?.notes || 'Expediente digital aperturado',
          createdAt: now,
          updatedAt: now
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/versions') && url.includes('/download') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: new Blob(['%PDF-1.4 mock pdf binary content'], { type: 'application/pdf' })
    })).pipe(delay(200));
  }

  if (url.includes('/versions') && method === 'POST') {
    const parts = url.split('/');
    const docId = parts[parts.indexOf('documents') + 1] || 'doc-1';
    const foundDoc = MOCK_DOSSIERS.employeeDocuments.find(d => d.id === docId);
    let fileName = 'archivo_v1.pdf';
    let fileSizeBytes = 1048576;
    let mimeType = 'application/pdf';

    if (req.body instanceof FormData) {
      const file = req.body.get('file') as File;
      if (file && file.name) {
        fileName = file.name;
        fileSizeBytes = file.size || 1048576;
        mimeType = file.type || 'application/pdf';
      }
    }

    if (foundDoc) {
      foundDoc.fileName = fileName;
      foundDoc.fileSizeBytes = fileSizeBytes;
      foundDoc.mimeType = mimeType;
      foundDoc.versionNumber = (foundDoc.versionNumber || 1) + 1;
      foundDoc.updatedAt = now;
    }

    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Versión digital subida y registrada',
        data: {
          id: 'ver-' + Date.now(),
          documentId: docId,
          versionNumber: foundDoc?.versionNumber || 1,
          fileName,
          fileSizeBytes,
          mimeType,
          uploadedBy: '550e8400-e29b-41d4-a716-446655440000',
          uploadedAt: now
        },
        timestamp: now
      }
    })).pipe(delay(250));
  }

  if (url.includes('/versions') && method === 'GET') {
    const parts = url.split('/');
    const docId = parts[parts.indexOf('documents') + 1] || 'doc-1';
    const foundDoc = MOCK_DOSSIERS.employeeDocuments.find(d => d.id === docId);
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Historial de versiones obtenido',
        data: [
          {
            id: 'ver-1-' + docId,
            documentId: docId,
            versionNumber: foundDoc?.versionNumber || 1,
            fileName: foundDoc?.fileName || 'documento.pdf',
            fileSizeBytes: foundDoc?.fileSizeBytes || 1048576,
            mimeType: foundDoc?.mimeType || 'application/pdf',
            uploadedBy: '550e8400-e29b-41d4-a716-446655440000',
            uploadedAt: foundDoc?.createdAt || now
          }
        ],
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/verify') && method === 'PATCH') {
    const parts = url.split('/');
    const docId = parts[parts.indexOf('documents') + 1] || parts[parts.indexOf('verify') - 1];
    const body = req.body as any;
    const foundDoc = MOCK_DOSSIERS.employeeDocuments.find(d => d.id === docId);
    if (foundDoc) {
      foundDoc.verificationStatus = body?.status || 'VERIFIED';
      foundDoc.verifiedAt = now;
      foundDoc.verifiedBy = '550e8400-e29b-41d4-a716-446655440000';
      foundDoc.updatedAt = now;
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Estado de verificación actualizado',
        data: foundDoc || { id: docId, verificationStatus: body?.status || 'VERIFIED' },
        timestamp: now
      }
    })).pipe(delay(200));
  }

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
    let docType: any = 'OTHER';
    let docNumber = '';
    let fileName = 'Documento_' + Date.now() + '.pdf';
    let title = 'Documento Digital';
    let issueDate: string | undefined;
    let expDate: string | undefined;
    let isConfidential = false;

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
      isConfidential = req.body.get('confidential') === 'true';
      title = (req.body.get('title') as string) || typeName;
      const file = req.body.get('file') as File;
      if (file && file.name) {
        fileName = file.name;
      }
    } else if (req.body && typeof req.body === 'object') {
      const body = req.body as any;
      docType = body.documentType || 'OTHER';
      typeName = body.title || docType;
      title = body.title || docType;
      docNumber = body.documentNumber || '';
      issueDate = body.issuedAt || undefined;
      expDate = body.expiresAt || undefined;
      isConfidential = !!body.confidential;
      cat = docType === 'IDENTITY' ? 'IDENTIFICATION' : (docType === 'CONTRACT' ? 'CONTRACT' : (docType === 'MEDICAL' ? 'HEALTH' : 'OTHER'));
    }

    const newDoc: any = {
      id: 'doc-' + Date.now(),
      employeeFileId: 'ef-9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      documentTypeId: '11111111-1111-1111-1111-111111111101',
      documentType: docType,
      documentTypeName: typeName,
      documentCategory: cat,
      title: title,
      documentNumber: docNumber || undefined,
      fileName: fileName,
      filePath: `tenants/uploads/${fileName}`,
      fileSizeBytes: 1048576,
      mimeType: fileName.endsWith('.docx') ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' : (fileName.endsWith('.png') ? 'image/png' : 'application/pdf'),
      versionNumber: 1,
      issueDate: issueDate,
      expirationDate: expDate,
      confidential: isConfidential,
      verificationStatus: 'PENDING' as const,
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
    })).pipe(delay(250));
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

  if (url.includes('/shifts/assignments/') && url.includes('/cancel') && method === 'PATCH') {
    const parts = url.split('/');
    const asgId = parts[parts.indexOf('cancel') - 1];
    const asg = MOCK_SHIFTS.employeeShiftAssignments.find(a => a.id === asgId);
    if (asg) asg.status = 'CANCELLED';
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Asignación de turno cancelada exitosamente',
        data: asg,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts/assignments/') && url.includes('/close') && method === 'PATCH') {
    const parts = url.split('/');
    const asgId = parts[parts.indexOf('close') - 1];
    const asg = MOCK_SHIFTS.employeeShiftAssignments.find(a => a.id === asgId);
    if (asg) asg.status = 'CLOSED';
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Asignación de turno cerrada exitosamente',
        data: asg,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts/calendar-exceptions') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Excepciones de calendario obtenidas',
        data: [
          {
            id: 'ex-01',
            date: '2026-11-02',
            name: 'Todos Santos (Feriado Nacional)',
            exceptionType: 'HOLIDAY',
            affectsAll: true,
            createdAt: now
          }
        ],
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts/calendar-exceptions') && method === 'POST') {
    const body = req.body as any;
    const newEx = {
      id: 'ex-' + Date.now(),
      date: body?.date || '2026-12-25',
      name: body?.name || 'Feriado Navideño',
      exceptionType: body?.exceptionType || 'HOLIDAY',
      affectsAll: body?.affectsAll ?? true,
      createdAt: now
    };
    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Excepción de calendario registrada exitosamente',
        data: newEx,
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
        data: {
          content: MOCK_SHIFTS.employeeShiftAssignments,
          page: 0,
          size: 20,
          totalElements: MOCK_SHIFTS.employeeShiftAssignments.length,
          totalPages: 1
        },
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
          weekdays: body.weekdays || [1, 2, 3, 4, 5],
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
      crossesMidnight: body?.crossesMidnight ?? body?.isOvernight ?? false,
      lateToleranceMinutes: body?.lateToleranceMinutes ?? body?.gracePeriodMinutes ?? 10,
      earlyDepartureToleranceMinutes: body?.earlyDepartureToleranceMinutes ?? 5,
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

  if (url.includes('/shifts/') && url.includes('/activate') && method === 'PATCH') {
    const parts = url.split('/');
    const shiftId = parts[parts.indexOf('activate') - 1];
    const shift = MOCK_SHIFTS.shifts.find(s => s.id === shiftId);
    if (shift) shift.status = 'ACTIVE';
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Turno activado exitosamente',
        data: shift,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts/') && url.includes('/deactivate') && method === 'PATCH') {
    const parts = url.split('/');
    const shiftId = parts[parts.indexOf('deactivate') - 1];
    const shift = MOCK_SHIFTS.shifts.find(s => s.id === shiftId);
    if (shift) shift.status = 'INACTIVE';
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Turno desactivado exitosamente',
        data: shift,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/shifts/schedule') && method === 'GET') {
    const urlObj = new URL(req.urlWithParams, 'http://localhost');
    const empId = urlObj.searchParams.get('employeeId') || '';
    const date = urlObj.searchParams.get('date') || '2026-10-14';
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Horario efectivo resuelto',
        data: {
          employeeId: empId,
          date,
          departmentId: null,
          calendarExceptionId: null,
          source: 'ASSIGNMENT',
          determination: 'EXACT',
          reason: 'Turno regular asignado',
          intervals: [
            {
              shiftId: '22222222-2222-2222-2222-222222222201',
              start: `${date}T08:30:00`,
              end: `${date}T16:30:00`
            }
          ]
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/breaks') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Descansos del turno obtenidos',
        data: {
          content: [
            {
              id: 'brk-001',
              shiftId: '22222222-2222-2222-2222-222222222201',
              name: 'Almuerzo',
              startTime: '12:00:00',
              endTime: '12:45:00',
              durationMinutes: 45,
              paid: false,
              createdAt: '2026-01-10T08:00:00Z'
            }
          ],
          page: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/breaks') && method === 'POST') {
    const body = req.body as any;
    const newBreak = {
      id: 'brk-' + Date.now(),
      shiftId: body?.shiftId || '22222222-2222-2222-2222-222222222201',
      name: body?.name || 'Descanso Intermedio',
      startTime: body?.startTime || '15:00:00',
      endTime: body?.endTime || '15:15:00',
      durationMinutes: body?.durationMinutes || 15,
      paid: !!body?.paid,
      createdAt: now
    };
    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Descanso configurado exitosamente',
        data: newBreak,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/breaks') && method === 'DELETE') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Descanso eliminado exitosamente',
        data: null,
        timestamp: now
      }
    })).pipe(delay(200));
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

  if (url.includes('/attendance/records/calculate') && method === 'POST') {
    const body = req.body as any;
    const empId = body?.employeeId || '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d';
    const workDate = body?.workDate || now.substring(0, 10);
    let record: any = MOCK_ATTENDANCE.attendanceRecords.find(r => r.employeeId === empId && r.workDate === workDate);
    if (!record) {
      record = {
        id: 'rec-' + Date.now(),
        tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
        employeeId: empId,
        workDate: workDate,
        workedHours: 8.0,
        workedMinutes: 480,
        lateMinutes: 0,
        earlyDepartureMinutes: 0,
        status: 'PRESENT',
        source: 'WEB',
        createdAt: now,
        updatedAt: now
      };
      MOCK_ATTENDANCE.attendanceRecords.unshift(record);
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Jornada calculada exitosamente',
        data: record,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  if (url.includes('/attendance/records') && method === 'GET') {
    const urlObj = new URL(req.urlWithParams, 'http://localhost');
    const empId = urlObj.searchParams.get('employeeId');
    const status = urlObj.searchParams.get('status');
    const startDate = urlObj.searchParams.get('startDate') || urlObj.searchParams.get('from');
    const endDate = urlObj.searchParams.get('endDate') || urlObj.searchParams.get('to');

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

  // Punches canónicos Spring Boot /api/attendance/punches
  if (url.includes('/attendance/punches') && method === 'POST') {
    const body = req.body as any;
    const empId = body?.employeeId || '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d';
    const punchType = body?.punchType || body?.type || 'IN';
    const todayStr = now.substring(0, 10);

    const empCatalog: Record<string, { name: string; code: string }> = {
      '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d': { name: 'Carlos Mamani Quispe', code: 'EMP-00104' },
      '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c': { name: 'Valeria Rios Salazar', code: 'EMP-00105' },
      '7b3dfc4d-5c7d-5dcf-9bdd-3b1d8c4dcb7e': { name: 'Rodrigo Flores Condori', code: 'EMP-00106' }
    };
    const empInfo = empCatalog[empId] || { name: 'Carlos Mamani Quispe', code: 'EMP-00104' };

    let record: any = MOCK_ATTENDANCE.attendanceRecords.find(r => r.employeeId === empId && r.workDate === todayStr);
    if (!record) {
      record = {
        id: 'rec-' + Date.now(),
        tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
        employeeId: empId,
        employeeFullName: empInfo.name,
        employeeCode: empInfo.code,
        workDate: todayStr,
        checkInTime: punchType === 'IN' ? now : '2026-10-12T08:30:00Z',
        checkOutTime: punchType === 'OUT' ? now : null,
        workedHours: punchType === 'OUT' ? 8.0 : 0.0,
        workedMinutes: punchType === 'OUT' ? 480 : 0,
        overtimeHours: 0.0,
        lateMinutes: 0,
        earlyDepartureMinutes: 0,
        status: 'PRESENT' as const,
        source: body?.source || 'WEB',
        latitude: body?.latitude ?? -16.5002450,
        longitude: body?.longitude ?? -68.1325400,
        createdAt: now,
        updatedAt: now
      };
      MOCK_ATTENDANCE.attendanceRecords.unshift(record);
    } else {
      if (punchType === 'IN') record.checkInTime = now;
      if (punchType === 'OUT') {
        record.checkOutTime = now;
        record.workedHours = 8.0;
        record.workedMinutes = 480;
      }
      record.updatedAt = now;
    }

    const punch = {
      id: 'pnc-' + Date.now(),
      attendanceRecordId: record.id,
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: empId,
      punchType: punchType,
      punchedAt: now,
      punchTime: now,
      source: body?.source || 'WEB',
      deviceId: 'BROWSER-NAVIGATOR',
      latitude: body?.latitude ?? -16.5002450,
      longitude: body?.longitude ?? -68.1325400,
      notes: body?.notes || null,
      createdAt: now
    };
    MOCK_ATTENDANCE.attendancePunches.unshift(punch);

    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Marcación de asistencia registrada exitosamente',
        data: punch,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  if (url.includes('/attendance/punches') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Marcaciones de asistencia obtenidas',
        data: {
          content: MOCK_ATTENDANCE.attendancePunches,
          page: 0,
          size: 50,
          totalElements: MOCK_ATTENDANCE.attendancePunches.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(200));
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

  // Ajustes canónicos Spring Boot /api/attendance/adjustments
  if (url.includes('/attendance/adjustments') && url.includes('/approve') && method === 'PATCH') {
    const parts = url.split('/');
    const adjId = parts[parts.indexOf('approve') - 1];
    const adj = MOCK_ATTENDANCE.attendanceAdjustments.find(a => a.id === adjId);
    if (adj) {
      adj.status = 'APPROVED';
      adj.reviewedAt = now;
      adj.appliedAt = now;
    }
    return of(new HttpResponse({
      status: 200,
      body: { success: true, message: 'Ajuste de asistencia aprobado', data: adj, timestamp: now }
    })).pipe(delay(200));
  }

  if (url.includes('/attendance/adjustments') && url.includes('/reject') && method === 'PATCH') {
    const parts = url.split('/');
    const adjId = parts[parts.indexOf('reject') - 1];
    const adj = MOCK_ATTENDANCE.attendanceAdjustments.find(a => a.id === adjId);
    if (adj) {
      adj.status = 'REJECTED';
      adj.reviewedAt = now;
    }
    return of(new HttpResponse({
      status: 200,
      body: { success: true, message: 'Ajuste de asistencia rechazado', data: adj, timestamp: now }
    })).pipe(delay(200));
  }

  if (url.includes('/attendance/adjustments') && url.includes('/cancel') && method === 'PATCH') {
    const parts = url.split('/');
    const adjId = parts[parts.indexOf('cancel') - 1];
    const adj = MOCK_ATTENDANCE.attendanceAdjustments.find(a => a.id === adjId);
    if (adj) {
      adj.status = 'CANCELLED';
    }
    return of(new HttpResponse({
      status: 200,
      body: { success: true, message: 'Ajuste de asistencia cancelado', data: adj, timestamp: now }
    })).pipe(delay(200));
  }

  if (url.includes('/attendance/adjustments') && method === 'POST') {
    const body = req.body as any;
    const newAdj: any = {
      id: 'adj-' + Date.now(),
      attendanceRecordId: body?.attendanceRecordId || '33333333-3333-3333-3333-333333333301',
      adjustmentType: body?.adjustmentType || 'TIME_CORRECTION',
      reason: body?.reason || '',
      requestedValues: body?.requestedValues || {},
      status: 'PENDING',
      requestedBy: '550e8400-e29b-41d4-a716-446655440000',
      createdAt: now,
      updatedAt: now
    };
    MOCK_ATTENDANCE.attendanceAdjustments.unshift(newAdj);
    return of(new HttpResponse({
      status: 201,
      body: { success: true, message: 'Solicitud de ajuste creada', data: newAdj, timestamp: now }
    })).pipe(delay(200));
  }

  if (url.includes('/attendance/adjustments') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Ajustes de asistencia obtenidos',
        data: {
          content: MOCK_ATTENDANCE.attendanceAdjustments,
          page: 0,
          size: 50,
          totalElements: MOCK_ATTENDANCE.attendanceAdjustments.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  // Horas extra canónicas /api/attendance/overtime
  if (url.includes('/attendance/overtime') && url.includes('/approve') && method === 'PATCH') {
    const parts = url.split('/');
    const otId = parts[parts.indexOf('approve') - 1];
    const ot = MOCK_ATTENDANCE.attendanceOvertime.find(o => o.id === otId);
    if (ot) {
      ot.status = 'APPROVED';
      ot.approvedMinutes = (req.body as any)?.approvedMinutes ?? ot.requestedMinutes;
      ot.approvedAt = now;
    }
    return of(new HttpResponse({
      status: 200,
      body: { success: true, message: 'Horas extra aprobadas', data: ot, timestamp: now }
    })).pipe(delay(200));
  }

  if (url.includes('/attendance/overtime') && method === 'GET') {
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Horas extra obtenidas',
        data: {
          content: MOCK_ATTENDANCE.attendanceOvertime,
          page: 0,
          size: 50,
          totalElements: MOCK_ATTENDANCE.attendanceOvertime.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(200));
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

  // 1. Balance adjustment (PATCH)
  if (url.includes('/leaves/balances') && url.includes('/adjust') && method === 'PATCH') {
    const parts = url.split('/');
    const balId = parts[parts.indexOf('adjust') - 1];
    const body = req.body as any;
    const adjustmentDays = Number(body?.adjustmentDays) || 0;
    const bal = MOCK_LEAVES.leaveBalances.find(b => b.id === balId) || MOCK_LEAVES.leaveBalances[0];
    if (bal) {
      bal.adjustedDays = (bal.adjustedDays ?? 0) + adjustmentDays;
      bal.availableDays = (bal.availableDays ?? bal.remainingDays ?? 0) + adjustmentDays;
      bal.remainingDays = bal.availableDays;
      bal.updatedAt = now;
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Saldo de vacación ajustado correctamente',
        data: bal,
        timestamp: now
      }
    })).pipe(delay(200));
  }

  // 2. Balances get (GET)
  if (url.includes('/leaves/balances') && method === 'GET') {
    const parts = url.split('/');
    const empId = parts[parts.length - 1].split('?')[0];
    const match = MOCK_LEAVES.leaveBalances.find(b => b.employeeId === empId || b.id === empId) || MOCK_LEAVES.leaveBalances[0];
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

  // 3. Download leave attachment (binary Blob)
  if (url.includes('/leaves/requests') && url.includes('/attachments') && url.includes('/download') && method === 'GET') {
    const blob = new Blob(['%PDF-1.4 Mock Leave Attachment (CNS Medical Certificate)'], { type: 'application/pdf' });
    return of(new HttpResponse({
      status: 200,
      body: blob,
      headers: new HttpHeaders({
        'Content-Type': 'application/pdf',
        'Content-Disposition': 'attachment; filename="documento_adjunto.pdf"'
      })
    })).pipe(delay(200));
  }

  // 4. Upload leave attachment (POST)
  if (url.includes('/leaves/requests') && url.includes('/attachments') && method === 'POST') {
    const parts = url.split('/');
    const reqId = parts[parts.indexOf('attachments') - 1];
    const attId = 'att-' + Date.now();
    const newAtt = {
      id: attId,
      leaveRequestId: reqId,
      filename: 'comprobante_baja_medica.pdf',
      mimeType: 'application/pdf',
      fileSize: 524288,
      uploadedBy: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      createdAt: now
    };
    MOCK_LEAVES.leaveAttachments.unshift(newAtt);
    const target = MOCK_LEAVES.leaveRequests.find(r => r.id === reqId);
    if (target) {
      target.attachmentUrl = 'https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf';
    }
    return of(new HttpResponse({
      status: 201,
      body: {
        success: true,
        message: 'Comprobante adjuntado con éxito',
        data: newAtt,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  // 5. List leave attachments (GET)
  if (url.includes('/leaves/requests') && url.includes('/attachments') && method === 'GET') {
    const parts = url.split('/');
    const reqId = parts[parts.indexOf('attachments') - 1].split('?')[0];
    const attachments = MOCK_LEAVES.leaveAttachments.filter(a => a.leaveRequestId === reqId);
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Comprobantes obtenidos',
        data: {
          content: attachments,
          page: 0,
          size: 25,
          totalElements: attachments.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  // 6. List leave request history (GET)
  if (url.includes('/leaves/requests') && url.includes('/history') && method === 'GET') {
    const parts = url.split('/');
    const reqId = parts[parts.indexOf('history') - 1].split('?')[0];
    const history = MOCK_LEAVES.leaveHistory.filter(h => h.leaveRequestId === reqId);
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Historial de auditoría obtenido',
        data: {
          content: history,
          page: 0,
          size: 25,
          totalElements: history.length,
          totalPages: 1
        },
        timestamp: now
      }
    })).pipe(delay(200));
  }

  // 7. Submit leave request draft (PATCH)
  if (url.includes('/leaves/requests') && url.includes('/submit') && method === 'PATCH') {
    const parts = url.split('/');
    const reqId = parts[parts.indexOf('submit') - 1];
    const target = MOCK_LEAVES.leaveRequests.find(r => r.id === reqId);
    if (target) {
      const prev = target.status;
      target.status = 'PENDING';
      MOCK_LEAVES.leaveHistory.unshift({
        id: 'his-' + Date.now(),
        leaveRequestId: reqId,
        previousStatus: prev,
        newStatus: 'PENDING',
        comment: 'Solicitud enviada para revisión formal',
        performedBy: target.employeeId,
        createdAt: now
      });
      if (target.leaveTypeId === '44444444-4444-4444-4444-444444444401') {
        const bal = MOCK_LEAVES.leaveBalances.find(b => b.employeeId === target.employeeId);
        if (bal) {
          bal.pendingDays += target.totalDays;
          bal.updatedAt = now;
        }
      }
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Solicitud enviada para revisión',
        data: target,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  // 8. Approve / Reject / Review leave request (PATCH)
  if (url.includes('/leaves/requests') && (url.includes('/approve') || url.includes('/reject') || url.includes('/review')) && method === 'PATCH') {
    const parts = url.split('/');
    const action = url.includes('/approve') ? 'approve' : url.includes('/reject') ? 'reject' : 'review';
    const reqId = parts[parts.indexOf(action) - 1];
    const body = req.body as any;
    const isApprove = action === 'approve' || (action === 'review' && !!body?.approved);
    const comment = (body?.comment || body?.rejectionReason || '').trim();

    if (!isApprove && comment.length < 5) {
      return of(new HttpResponse({
        status: 400,
        body: {
          success: false,
          message: 'El motivo de rechazo es obligatorio (mínimo 5 caracteres).',
          timestamp: now
        }
      })).pipe(delay(200));
    }

    const targetReq = MOCK_LEAVES.leaveRequests.find(r => r.id === reqId);
    if (targetReq) {
      const prev = targetReq.status;
      targetReq.status = isApprove ? 'APPROVED' : 'REJECTED';
      targetReq.rejectionReason = isApprove ? null : comment;
      targetReq.reviewedBy = '550e8400-e29b-41d4-a716-446655440000';
      targetReq.reviewedAt = now;

      MOCK_LEAVES.leaveHistory.unshift({
        id: 'his-' + Date.now(),
        leaveRequestId: reqId,
        previousStatus: prev,
        newStatus: targetReq.status,
        comment: comment || (isApprove ? 'Aprobado por jefatura' : 'Rechazado'),
        performedBy: '550e8400-e29b-41d4-a716-446655440000',
        createdAt: now
      });

      // Sincronización con Saldo de Vacación
      if (isApprove && targetReq.leaveTypeId === '44444444-4444-4444-4444-444444444401') {
        const bal = MOCK_LEAVES.leaveBalances.find(b => b.employeeId === targetReq.employeeId);
        if (bal) {
          bal.usedDays += targetReq.totalDays;
          bal.pendingDays = Math.max(0, bal.pendingDays - targetReq.totalDays);
          bal.remainingDays = Math.max(0, bal.entitledDays - bal.usedDays);
          bal.availableDays = bal.remainingDays;
          bal.updatedAt = now;
        }
      } else if (!isApprove && targetReq.leaveTypeId === '44444444-4444-4444-4444-444444444401') {
        const bal = MOCK_LEAVES.leaveBalances.find(b => b.employeeId === targetReq.employeeId);
        if (bal) {
          bal.pendingDays = Math.max(0, bal.pendingDays - targetReq.totalDays);
          bal.updatedAt = now;
        }
      }

      // Sincronización con Asistencia HU17: Marcar días como JUSTIFIED
      if (isApprove) {
        const start = new Date(targetReq.startDate + 'T00:00:00');
        const end = new Date(targetReq.endDate + 'T00:00:00');
        for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
          const dStr = d.toISOString().substring(0, 10);
          const attRec = MOCK_ATTENDANCE.attendanceRecords.find(a => a.employeeId === targetReq.employeeId && a.workDate === dStr);
          if (attRec) {
            attRec.status = 'JUSTIFIED';
            attRec.updatedAt = now;
          } else {
            MOCK_ATTENDANCE.attendanceRecords.unshift({
              id: 'att-justified-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
              tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
              employeeId: targetReq.employeeId,
              employeeFullName: targetReq.employeeFullName,
              employeeCode: 'EMP-00104',
              workDate: dStr,
              checkInTime: null,
              checkOutTime: null,
              workedHours: 8.0,
              workedMinutes: 480,
              overtimeHours: 0,
              lateMinutes: 0,
              earlyDepartureMinutes: 0,
              status: 'JUSTIFIED',
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
        message: isApprove ? 'Solicitud aprobada y sincronizada con asistencia y saldos' : 'Solicitud rechazada',
        data: targetReq,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  // 9. Cancel leave request (PATCH)
  if (url.includes('/leaves/requests') && url.includes('/cancel') && method === 'PATCH') {
    const parts = url.split('/');
    const reqId = parts[parts.indexOf('cancel') - 1];
    const body = req.body as any;
    const comment = (body?.comment || 'Cancelado por el usuario').trim();

    const targetReq = MOCK_LEAVES.leaveRequests.find(r => r.id === reqId);
    if (targetReq) {
      const prev = targetReq.status;
      targetReq.status = 'CANCELLED';

      MOCK_LEAVES.leaveHistory.unshift({
        id: 'his-' + Date.now(),
        leaveRequestId: reqId,
        previousStatus: prev,
        newStatus: 'CANCELLED',
        comment: comment,
        performedBy: targetReq.employeeId,
        createdAt: now
      });

      if (targetReq.leaveTypeId === '44444444-4444-4444-4444-444444444401') {
        const bal = MOCK_LEAVES.leaveBalances.find(b => b.employeeId === targetReq.employeeId);
        if (bal) {
          bal.pendingDays = Math.max(0, bal.pendingDays - targetReq.totalDays);
          bal.updatedAt = now;
        }
      }
    }

    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Solicitud cancelada con éxito',
        data: targetReq,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  // 10. Get leave request by ID (GET)
  if (url.includes('/leaves/requests/') && method === 'GET' && !url.includes('/attachments') && !url.includes('/history') && !url.includes('?')) {
    const parts = url.split('/');
    const reqId = parts[parts.length - 1];
    const match = MOCK_LEAVES.leaveRequests.find(r => r.id === reqId);
    if (match) {
      return of(new HttpResponse({
        status: 200,
        body: {
          success: true,
          message: 'Solicitud obtenida',
          data: match,
          timestamp: now
        }
      })).pipe(delay(200));
    }
  }

  // 11. List leave requests (GET)
  if (url.includes('/leaves/requests') && method === 'GET') {
    const urlObj = new URL(req.urlWithParams, 'http://localhost');
    const empId = urlObj.searchParams.get('employeeId');
    const status = urlObj.searchParams.get('status');
    const leaveTypeId = urlObj.searchParams.get('leaveTypeId');

    let filtered = [...MOCK_LEAVES.leaveRequests];
    if (empId) {
      filtered = filtered.filter(r => r.employeeId === empId);
    }
    if (status && status !== 'ALL') {
      filtered = filtered.filter(r => r.status === status);
    }
    if (leaveTypeId) {
      filtered = filtered.filter(r => r.leaveTypeId === leaveTypeId);
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

  // 12. Create leave request (POST) - supports both multi-step JSON (DRAFT) and FormData (PENDING)
  if (url.includes('/leaves/requests') && method === 'POST') {
    const isFormData = req.body instanceof FormData;
    let empId = '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d';
    let leaveTypeId = '44444444-4444-4444-4444-444444444401';
    let startDate = '2026-11-02';
    let endDate = '2026-11-04';
    let reason = 'Solicitud de permiso';
    let proofFile: File | null = null;

    if (isFormData) {
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
    if (isFormData && typeObj?.requiresAttachment && !proofFile) {
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

    let docUrl: string | null = null;
    const reqId = 'req-' + Date.now();

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

      MOCK_LEAVES.leaveAttachments.unshift({
        id: 'att-' + Date.now(),
        leaveRequestId: reqId,
        filename: proofFile.name || 'comprobante.pdf',
        mimeType: 'application/pdf',
        fileSize: proofFile.size || 524288,
        uploadedBy: empId,
        createdAt: now
      });
    }

    const empCatalog: Record<string, string> = {
      '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d': 'Carlos Mamani Quispe',
      '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c': 'Valeria Rios Salazar',
      '7b3dfc4d-5c7d-5dcf-9bdd-3b1d8c4dcb7e': 'Rodrigo Flores Condori'
    };

    const initialStatus = isFormData ? 'PENDING' : 'DRAFT';

    const newReq = {
      id: reqId,
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
      status: initialStatus as any,
      reviewedBy: null,
      reviewedAt: null,
      rejectionReason: null,
      createdAt: now
    };

    MOCK_LEAVES.leaveRequests.unshift(newReq);

    MOCK_LEAVES.leaveHistory.unshift({
      id: 'his-' + Date.now(),
      leaveRequestId: reqId,
      previousStatus: null as any,
      newStatus: initialStatus as any,
      comment: isFormData ? 'Solicitud directa enviada' : 'Borrador inicial creado',
      performedBy: empId,
      createdAt: now
    });

    if (isFormData && leaveTypeId === '44444444-4444-4444-4444-444444444401') {
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
        message: isFormData ? 'Solicitud de ausencia enviada para revisión' : 'Borrador de solicitud creado',
        data: newReq,
        timestamp: now
      }
    })).pipe(delay(250));
  }

  // 13. Update leave request (PUT)
  if (url.includes('/leaves/requests') && method === 'PUT') {
    const parts = url.split('/');
    const reqId = parts[parts.length - 1];
    const b = req.body as any;
    const target = MOCK_LEAVES.leaveRequests.find(r => r.id === reqId);
    if (target) {
      if (b.startDate) target.startDate = b.startDate;
      if (b.endDate) target.endDate = b.endDate;
      if (b.reason) target.reason = b.reason;
      if (b.leaveTypeId) target.leaveTypeId = b.leaveTypeId;
    }
    return of(new HttpResponse({
      status: 200,
      body: {
        success: true,
        message: 'Solicitud actualizada',
        data: target,
        timestamp: now
      }
    })).pipe(delay(200));
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
