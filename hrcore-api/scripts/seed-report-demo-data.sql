-- Datos ficticios e idempotentes para validar Informe de RR. HH.
-- Ejecutar únicamente en una base local de desarrollo.
BEGIN;
SET LOCAL search_path TO tenant_casacolor, public;

INSERT INTO departments (id,code,name,description,status) VALUES
 ('d0000000-0000-0000-0000-000000000001','DEMO-ADM','Administración Demo','Datos ficticios para reportes','ACTIVE'),
 ('d0000000-0000-0000-0000-000000000002','DEMO-VEN','Ventas Demo','Datos ficticios para reportes','ACTIVE'),
 ('d0000000-0000-0000-0000-000000000003','DEMO-TEC','Tecnología Demo','Datos ficticios para reportes','ACTIVE'),
 ('d0000000-0000-0000-0000-000000000004','DEMO-OPE','Operaciones Demo','Datos ficticios para reportes','ACTIVE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO positions (id,code,name,description,organizational_level,salary_min,salary_max,currency,status) VALUES
 ('c0000000-0000-0000-0000-000000000001','DEMO-GER','Gerente Demo','Cargo ficticio','MANAGEMENT',8000,12000,'BOB','ACTIVE'),
 ('c0000000-0000-0000-0000-000000000002','DEMO-ANA','Analista Demo','Cargo ficticio','PROFESSIONAL',4500,7000,'BOB','ACTIVE'),
 ('c0000000-0000-0000-0000-000000000003','DEMO-DEV','Desarrollador Demo','Cargo ficticio','TECHNICAL',5500,9000,'BOB','ACTIVE'),
 ('c0000000-0000-0000-0000-000000000004','DEMO-AUX','Auxiliar Demo','Cargo ficticio','OPERATIONAL',2800,4200,'BOB','ACTIVE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO employees (id,employee_number,first_name,last_name,document_type,document_number,birth_date,gender,nationality,personal_email,work_email,mobile_phone,address,hire_date,termination_date,employment_status) VALUES
 ('e0000000-0000-0000-0000-000000000001','DEMO-EMP-001','Ana','Rojas','CI','DEMO-CI-001','1991-02-14','FEMALE','Boliviana','ana.demo@example.test','ana.rojas@demo.test','70000001','Dirección ficticia 1','2024-01-15',NULL,'ACTIVE'),
 ('e0000000-0000-0000-0000-000000000002','DEMO-EMP-002','Luis','Mendoza','CI','DEMO-CI-002','1988-07-22','MALE','Boliviana','luis.demo@example.test','luis.mendoza@demo.test','70000002','Dirección ficticia 2','2024-06-03',NULL,'ACTIVE'),
 ('e0000000-0000-0000-0000-000000000003','DEMO-EMP-003','Carla','Flores','CI','DEMO-CI-003','1995-11-05','FEMALE','Boliviana','carla.demo@example.test','carla.flores@demo.test','70000003','Dirección ficticia 3','2025-02-10',NULL,'ACTIVE'),
 ('e0000000-0000-0000-0000-000000000004','DEMO-EMP-004','Diego','Vargas','CI','DEMO-CI-004','1993-04-18','MALE','Boliviana','diego.demo@example.test','diego.vargas@demo.test','70000004','Dirección ficticia 4','2025-09-01',NULL,'ACTIVE'),
 ('e0000000-0000-0000-0000-000000000005','DEMO-EMP-005','Sofía','Quispe','CI','DEMO-CI-005','1997-09-09','FEMALE','Boliviana','sofia.demo@example.test','sofia.quispe@demo.test','70000005','Dirección ficticia 5','2026-01-12',NULL,'ACTIVE'),
 ('e0000000-0000-0000-0000-000000000006','DEMO-EMP-006','Marco','Suárez','CI','DEMO-CI-006','1990-12-30','MALE','Boliviana','marco.demo@example.test','marco.suarez@demo.test','70000006','Dirección ficticia 6','2026-04-06',NULL,'ACTIVE'),
 ('e0000000-0000-0000-0000-000000000007','DEMO-EMP-007','Elena','Castro','CI','DEMO-CI-007','1998-03-25','FEMALE','Boliviana','elena.demo@example.test','elena.castro@demo.test','70000007','Dirección ficticia 7','2026-09-07',NULL,'ACTIVE'),
 ('e0000000-0000-0000-0000-000000000008','DEMO-EMP-008','Pablo','López','CI','DEMO-CI-008','1989-06-16','MALE','Boliviana','pablo.demo@example.test','pablo.lopez@demo.test','70000008','Dirección ficticia 8','2025-03-17','2026-10-05','TERMINATED')
ON CONFLICT (employee_number) DO NOTHING;

INSERT INTO employee_terminations (id,employee_id,employment_period_id,effective_date,reason)
SELECT 'e1000000-0000-0000-0000-000000000008',e.id,p.id,e.termination_date,'Finalización ficticia para reportes'
FROM employees e JOIN employment_periods p ON p.employee_id=e.id
WHERE e.employee_number='DEMO-EMP-008'
ON CONFLICT (employment_period_id) DO NOTHING;

INSERT INTO employee_assignments (id,employee_id,employment_period_id,department_id,position_id,start_date,end_date,assignment_type,assignment_status,reason)
SELECT v.assignment_id,v.employee_id,p.id,v.department_id,v.position_id,e.hire_date,e.termination_date,'PRIMARY','ACTIVE','Asignación ficticia para reportes'
FROM (VALUES
 ('a0000000-0000-0000-0000-000000000001'::uuid,'e0000000-0000-0000-0000-000000000001'::uuid,'d0000000-0000-0000-0000-000000000001'::uuid,'c0000000-0000-0000-0000-000000000001'::uuid),
 ('a0000000-0000-0000-0000-000000000002','e0000000-0000-0000-0000-000000000002','d0000000-0000-0000-0000-000000000002','c0000000-0000-0000-0000-000000000002'),
 ('a0000000-0000-0000-0000-000000000003','e0000000-0000-0000-0000-000000000003','d0000000-0000-0000-0000-000000000003','c0000000-0000-0000-0000-000000000003'),
 ('a0000000-0000-0000-0000-000000000004','e0000000-0000-0000-0000-000000000004','d0000000-0000-0000-0000-000000000004','c0000000-0000-0000-0000-000000000004'),
 ('a0000000-0000-0000-0000-000000000005','e0000000-0000-0000-0000-000000000005','d0000000-0000-0000-0000-000000000002','c0000000-0000-0000-0000-000000000002'),
 ('a0000000-0000-0000-0000-000000000006','e0000000-0000-0000-0000-000000000006','d0000000-0000-0000-0000-000000000003','c0000000-0000-0000-0000-000000000003'),
 ('a0000000-0000-0000-0000-000000000007','e0000000-0000-0000-0000-000000000007','d0000000-0000-0000-0000-000000000004','c0000000-0000-0000-0000-000000000004'),
 ('a0000000-0000-0000-0000-000000000008','e0000000-0000-0000-0000-000000000008','d0000000-0000-0000-0000-000000000001','c0000000-0000-0000-0000-000000000004')
) AS v(assignment_id,employee_id,department_id,position_id)
JOIN employees e ON e.id=v.employee_id
JOIN employment_periods p ON p.employee_id=e.id
ON CONFLICT (id) DO NOTHING;

INSERT INTO employment_contracts (id,employee_id,employment_period_id,employee_assignment_id,contract_number,contract_type,status,start_date,end_date,signed_at,base_salary,currency,payment_frequency,weekly_hours,contract_form,work_schedule,work_modality,termination_date,termination_reason,terms)
SELECT v.contract_id,v.employee_id,p.id,v.assignment_id,v.contract_number,v.contract_type,v.status,e.hire_date,v.end_date,e.hire_date::timestamp+interval '8 hours',v.salary,'BOB','MONTHLY',40,'WRITTEN','FULL_TIME',v.modality,v.termination_date,v.termination_reason,'Contrato ficticio para validación de informes'
FROM (VALUES
 ('b0000000-0000-0000-0000-000000000001'::uuid,'e0000000-0000-0000-0000-000000000001'::uuid,'a0000000-0000-0000-0000-000000000001'::uuid,'DEMO-CON-001','INDEFINITE','ACTIVE',NULL::date,9500::numeric,'ONSITE',NULL::date,NULL::text),
 ('b0000000-0000-0000-0000-000000000002','e0000000-0000-0000-0000-000000000002','a0000000-0000-0000-0000-000000000002','DEMO-CON-002','FIXED_TERM','ACTIVE','2026-12-31',6200,'HYBRID',NULL,NULL),
 ('b0000000-0000-0000-0000-000000000003','e0000000-0000-0000-0000-000000000003','a0000000-0000-0000-0000-000000000003','DEMO-CON-003','INDEFINITE','ACTIVE',NULL,7800,'REMOTE',NULL,NULL),
 ('b0000000-0000-0000-0000-000000000004','e0000000-0000-0000-0000-000000000004','a0000000-0000-0000-0000-000000000004','DEMO-CON-004','FIXED_TERM','ACTIVE','2026-11-10',3600,'ONSITE',NULL,NULL),
 ('b0000000-0000-0000-0000-000000000005','e0000000-0000-0000-0000-000000000005','a0000000-0000-0000-0000-000000000005','DEMO-CON-005','FIXED_TERM','ACTIVE','2027-01-11',5900,'HYBRID',NULL,NULL),
 ('b0000000-0000-0000-0000-000000000006','e0000000-0000-0000-0000-000000000006','a0000000-0000-0000-0000-000000000006','DEMO-CON-006','INDEFINITE','ACTIVE',NULL,7200,'REMOTE',NULL,NULL),
 ('b0000000-0000-0000-0000-000000000007','e0000000-0000-0000-0000-000000000007','a0000000-0000-0000-0000-000000000007','DEMO-CON-007','INTERNSHIP','ACTIVE','2026-12-06',3200,'ONSITE',NULL,NULL),
 ('b0000000-0000-0000-0000-000000000008','e0000000-0000-0000-0000-000000000008','a0000000-0000-0000-0000-000000000008','DEMO-CON-008','FIXED_TERM','TERMINATED','2026-12-31',3500,'ONSITE','2026-10-05','Finalización ficticia')
) AS v(contract_id,employee_id,assignment_id,contract_number,contract_type,status,end_date,salary,modality,termination_date,termination_reason)
JOIN employees e ON e.id=v.employee_id
JOIN employment_periods p ON p.employee_id=e.id
ON CONFLICT (contract_number) DO NOTHING;

INSERT INTO employee_files (id,employee_id,file_number,status,notes)
SELECT ('f1000000-0000-0000-0000-'||right(e.employee_number,3)||'000000000')::uuid,e.id,'DEMO-EXP-'||right(e.employee_number,3),'ACTIVE','Expediente ficticio para reportes'
FROM employees e WHERE e.employee_number LIKE 'DEMO-EMP-%' AND e.employee_number<>'DEMO-EMP-007'
ON CONFLICT (employee_id) DO NOTHING;

INSERT INTO employee_documents (id,employee_file_id,contract_id,document_type,title,document_number,issued_at,expires_at,confidential,verification_status,status)
SELECT gen_random_uuid(),ef.id,c.id,'IDENTITY','Documento de identidad ficticio','ID-'||ef.file_number,'2024-01-01',
       CASE WHEN right(ef.file_number,3)='006' THEN '2026-10-10'::date ELSE '2030-12-31'::date END,FALSE,
       CASE WHEN right(ef.file_number,3) IN ('004','005') THEN 'PENDING' ELSE 'VERIFIED' END,'ACTIVE'
FROM employee_files ef JOIN employees e ON e.id=ef.employee_id LEFT JOIN employment_contracts c ON c.employee_id=e.id
WHERE ef.file_number LIKE 'DEMO-EXP-%' AND NOT EXISTS (SELECT 1 FROM employee_documents d WHERE d.employee_file_id=ef.id AND d.document_type='IDENTITY');

INSERT INTO employee_documents (id,employee_file_id,contract_id,document_type,title,document_number,issued_at,confidential,verification_status,status)
SELECT gen_random_uuid(),ef.id,c.id,'CONTRACT','Contrato laboral ficticio',c.contract_number,c.start_date,TRUE,
       CASE WHEN right(ef.file_number,3)='005' THEN 'PENDING' ELSE 'VERIFIED' END,'ACTIVE'
FROM employee_files ef JOIN employees e ON e.id=ef.employee_id JOIN employment_contracts c ON c.employee_id=e.id
WHERE ef.file_number LIKE 'DEMO-EXP-%' AND right(ef.file_number,3) NOT IN ('004','006')
AND NOT EXISTS (SELECT 1 FROM employee_documents d WHERE d.employee_file_id=ef.id AND d.document_type='CONTRACT');

INSERT INTO work_shifts (id,code,name,description,start_time,end_time,crosses_midnight,late_tolerance_minutes,early_departure_tolerance_minutes,status) VALUES
 ('f2000000-0000-0000-0000-000000000001','DEMO-DIURNO','Turno diurno demo','Turno ficticio','08:00','16:00',FALSE,10,5,'ACTIVE'),
 ('f2000000-0000-0000-0000-000000000002','DEMO-MIXTO','Turno tarde demo','Turno ficticio','14:00','22:00',FALSE,10,5,'ACTIVE'),
 ('f2000000-0000-0000-0000-000000000003','DEMO-NOCTURNO','Turno nocturno demo','Turno ficticio','22:00','06:00',TRUE,10,5,'ACTIVE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO employee_shift_assignments (id,employee_id,employment_period_id,shift_id,start_date,end_date,weekdays,status)
SELECT ('f3000000-0000-0000-0000-'||right(e.employee_number,3)||'000000000')::uuid,e.id,p.id,
       CASE (right(e.employee_number,3)::int % 3) WHEN 1 THEN 'f2000000-0000-0000-0000-000000000001'::uuid WHEN 2 THEN 'f2000000-0000-0000-0000-000000000002'::uuid ELSE 'f2000000-0000-0000-0000-000000000003'::uuid END,
       e.hire_date,e.termination_date,ARRAY[1,2,3,4,5]::smallint[],'ACTIVE'
FROM employees e JOIN employment_periods p ON p.employee_id=e.id WHERE e.employee_number LIKE 'DEMO-EMP-%'
ON CONFLICT (id) DO NOTHING;

INSERT INTO attendance_records (employee_id,employment_period_id,shift_assignment_id,work_date,expected_start_at,expected_end_at,actual_first_in_at,actual_last_out_at,worked_minutes,late_minutes,early_departure_minutes,status,calculated_at)
SELECT e.id,p.id,sa.id,d::date,
       (d::date+time '08:00') AT TIME ZONE 'America/La_Paz',(d::date+time '16:00') AT TIME ZONE 'America/La_Paz',
       CASE WHEN (extract(day from d)::int+right(e.employee_number,3)::int)%11=0 THEN NULL ELSE (d::date+time '08:00'+CASE WHEN (extract(day from d)::int+right(e.employee_number,3)::int)%5=0 THEN interval '20 minutes' ELSE interval '0 minutes' END) AT TIME ZONE 'America/La_Paz' END,
       CASE WHEN (extract(day from d)::int+right(e.employee_number,3)::int)%11=0 THEN NULL ELSE (d::date+time '16:00') AT TIME ZONE 'America/La_Paz' END,
       CASE WHEN (extract(day from d)::int+right(e.employee_number,3)::int)%11=0 THEN 0 ELSE 480 END,
       CASE WHEN (extract(day from d)::int+right(e.employee_number,3)::int)%5=0 THEN 20 ELSE 0 END,0,
       CASE WHEN (extract(day from d)::int+right(e.employee_number,3)::int)%11=0 THEN 'ABSENT' WHEN (extract(day from d)::int+right(e.employee_number,3)::int)%5=0 THEN 'LATE' ELSE 'PRESENT' END,NOW()
FROM employees e JOIN employment_periods p ON p.employee_id=e.id JOIN employee_shift_assignments sa ON sa.employee_id=e.id
CROSS JOIN generate_series('2026-09-21'::date,'2026-10-16'::date,interval '1 day') d
WHERE e.employee_number LIKE 'DEMO-EMP-%' AND extract(isodow from d) between 1 and 5 AND d::date between p.start_date and coalesce(p.end_date,'infinity'::date)
ON CONFLICT (employee_id,work_date) DO NOTHING;

INSERT INTO leave_requests (id,employee_id,employment_period_id,leave_type_id,start_date,end_date,requested_days,reason,status,submitted_at,reviewed_at,review_comment)
SELECT v.id,v.employee_id,p.id,t.id,v.start_date,v.end_date,v.days,'Solicitud ficticia para reportes',v.status,NOW(),CASE WHEN v.status IN ('APPROVED','REJECTED') THEN NOW() END,'Revisión ficticia'
FROM (VALUES
 ('aa000000-0000-0000-0000-000000000001'::uuid,'e0000000-0000-0000-0000-000000000002'::uuid,'LEAVE_VACATION','2026-09-28'::date,'2026-09-30'::date,3::numeric,'APPROVED'),
 ('aa000000-0000-0000-0000-000000000003','e0000000-0000-0000-0000-000000000004','MEDICAL_LEAVE','2026-10-06','2026-10-07',2,'APPROVED'),
 ('aa000000-0000-0000-0000-000000000004','e0000000-0000-0000-0000-000000000005','PERSONAL_PERMISSION','2026-10-20','2026-10-20',1,'PENDING'),
 ('aa000000-0000-0000-0000-000000000005','e0000000-0000-0000-0000-000000000006','UNPAID_LEAVE','2026-10-12','2026-10-14',3,'REJECTED')
) AS v(id,employee_id,type_code,start_date,end_date,days,status)
JOIN employment_periods p ON p.employee_id=v.employee_id JOIN leave_types t ON t.code=v.type_code
ON CONFLICT (id) DO NOTHING;

INSERT INTO employee_incidents (id,employee_id,employment_period_id,incident_number,incident_type,severity,title,description,occurred_at,location,status)
SELECT v.id,v.employee_id,p.id,v.number,v.type,v.severity,v.title,'Incidencia ficticia para reportes',v.occurred_at,'Oficina Demo',v.status
FROM (VALUES
 ('ab000000-0000-0000-0000-000000000001'::uuid,'e0000000-0000-0000-0000-000000000002'::uuid,'DEMO-INC-001','WARNING','LOW','Tardanza reiterada','2026-09-25 09:00-04'::timestamptz,'RESOLVED'),
 ('ab000000-0000-0000-0000-000000000002','e0000000-0000-0000-0000-000000000003','DEMO-INC-002','RECOGNITION','LOW','Reconocimiento de desempeño','2026-10-02 10:00-04','CLOSED'),
 ('ab000000-0000-0000-0000-000000000003','e0000000-0000-0000-0000-000000000004','DEMO-INC-003','SAFETY','HIGH','Incidente de seguridad','2026-10-08 15:00-04','UNDER_REVIEW'),
 ('ab000000-0000-0000-0000-000000000004','e0000000-0000-0000-0000-000000000006','DEMO-INC-004','PERFORMANCE','MEDIUM','Seguimiento de desempeño','2026-10-15 11:00-04','OPEN')
) AS v(id,employee_id,number,type,severity,title,occurred_at,status)
JOIN employment_periods p ON p.employee_id=v.employee_id
ON CONFLICT (incident_number) DO NOTHING;

COMMIT;
