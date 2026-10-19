import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeFilesPageComponent } from './employee-files-page.component';
import { EmployeeFileListUseCase } from '../../features/employee-file-management';
import { EmployeesService, EmployeeSummary } from '../../entities/employees';

describe('EmployeeFilesPageComponent (HU15 UI)', () => {
  let component: EmployeeFilesPageComponent;
  let fixture: ComponentFixture<EmployeeFilesPageComponent>;
  let fileListUseCaseSpy: jasmine.SpyObj<EmployeeFileListUseCase>;
  let employeesServiceSpy: jasmine.SpyObj<EmployeesService>;

  const mockEmployees: EmployeeSummary[] = [
    {
      id: 'emp-001',
      employeeNumber: 'EMP-00104',
      firstName: 'Carlos',
      lastName: 'Mamani',
      employmentStatus: 'ACTIVE'
    },
    {
      id: 'emp-002',
      employeeNumber: 'EMP-00105',
      firstName: 'Valeria',
      lastName: 'Rios',
      employmentStatus: 'ACTIVE'
    }
  ];

  beforeEach(async () => {
    fileListUseCaseSpy = jasmine.createSpyObj('EmployeeFileListUseCase', [
      'loadDocumentTypes',
      'loadDocuments',
      'setEmployeeId',
      'createFileForEmployee',
      'verifyDocument',
      'archiveDocument'
    ], {
      loading: jasmine.createSpy('loading').and.returnValue(false),
      activeFile: jasmine.createSpy('activeFile').and.returnValue(null),
      documents: jasmine.createSpy('documents').and.returnValue([]),
      documentTypes: jasmine.createSpy('documentTypes').and.returnValue([]),
      kpis: jasmine.createSpy('kpis').and.returnValue({ total: 0, sensitive: 0, expiringSoon: 0 }),
      filteredDocuments: jasmine.createSpy('filteredDocuments').and.returnValue([]),
      selectedCategory: jasmine.createSpy('selectedCategory').and.returnValue('ALL'),
      searchQuery: jasmine.createSpy('searchQuery').and.returnValue(''),
      currentPage: jasmine.createSpy('currentPage').and.returnValue(0),
      totalPages: jasmine.createSpy('totalPages').and.returnValue(1),
      totalElements: jasmine.createSpy('totalElements').and.returnValue(0)
    });

    employeesServiceSpy = jasmine.createSpyObj('EmployeesService', ['list']);
    employeesServiceSpy.list.and.returnValue(of({
      success: true,
      message: 'OK',
      data: {
        content: mockEmployees,
        pageable: { pageNumber: 0, pageSize: 50 },
        totalPages: 1,
        totalElements: 2,
        last: true,
        size: 50,
        number: 0,
        numberOfElements: 2,
        first: true,
        empty: false
      },
      timestamp: ''
    }));

    await TestBed.configureTestingModule({
      imports: [EmployeeFilesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: EmployeeFileListUseCase, useValue: fileListUseCaseSpy },
        { provide: EmployeesService, useValue: employeesServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (key: string) => (key === 'employeeId' ? 'emp-002' : null)
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeFilesPageComponent);
    component = fixture.componentInstance;
  });

  it('debe crear el componente y autoseleccionar el empleado del queryParam', async () => {
    await component.ngOnInit();
    expect(component).toBeTruthy();
    expect(component.selectedEmployeeId).toBe('emp-002');
    expect(fileListUseCaseSpy.setEmployeeId).toHaveBeenCalledWith('emp-002');
  });

  it('debe abrir el modal de aperturar expediente con folio sugerido', () => {
    component.employees = mockEmployees;
    component.selectedEmployeeId = 'emp-001';

    component.openCreateModal();

    expect(component.showCreateFileDialog).toBeTrue();
    expect(component.newFileNumber).toBe('EXP-EMP-00104');
  });

  it('debe aperturar expediente llamando al caso de uso', async () => {
    component.employees = mockEmployees;
    component.selectedEmployeeId = 'emp-001';
    component.newFileNumber = 'EXP-EMP-00104';
    component.newFileNotes = 'Notas de prueba';
    fileListUseCaseSpy.createFileForEmployee.and.returnValue(Promise.resolve(true));

    await component.submitCreateFile();

    expect(fileListUseCaseSpy.createFileForEmployee).toHaveBeenCalledWith('emp-001', 'EXP-EMP-00104', 'Notas de prueba');
    expect(component.showCreateFileDialog).toBeFalse();
  });

  it('debe generar la trazabilidad de auditoría real a partir del expediente activo y documentos', () => {
    (fileListUseCaseSpy.activeFile as any).and.returnValue({
      id: 'f-1',
      employeeId: 'emp-001',
      fileNumber: 'EXP-EMP-00104',
      status: 'ACTIVE',
      createdAt: '2026-01-01T10:00:00Z',
      updatedAt: '2026-01-01T10:00:00Z'
    });

    (fileListUseCaseSpy.documents as any).and.returnValue([
      {
        id: 'd-1',
        title: 'Contrato 2026',
        documentTypeName: 'Contrato Laboral',
        createdAt: '2026-01-02T10:00:00Z',
        verifiedAt: '2026-01-03T10:00:00Z',
        verificationStatus: 'VERIFIED'
      }
    ]);

    const audits = component.getAudits();
    expect(audits.length).toBe(3); // Apertura de expediente + Registro doc + Verificación doc
    expect(audits[0].action).toBe('VIEW'); // La verificación fue el 3 de enero (más reciente)
    expect(audits[1].action).toBe('UPLOAD'); // Registro doc el 2 de enero
    expect(audits[2].action).toBe('UPLOAD'); // Apertura expediente el 1 de enero
  });
});
