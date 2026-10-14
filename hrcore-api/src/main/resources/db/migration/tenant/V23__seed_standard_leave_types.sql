-- Catálogo inicial de HU18 para que cada tenant pueda registrar permisos y ausencias.
-- El tenant puede ampliar o desactivar estos tipos desde la API sin perder historial.
INSERT INTO leave_types (
    id, code, name, description, paid,
    requires_approval, requires_attachment, deducts_balance, status
) VALUES
    (gen_random_uuid(), 'LEAVE_VACATION', 'Vacación anual',
     'Descanso anual sujeto al saldo disponible del empleado.', TRUE, TRUE, FALSE, TRUE, 'ACTIVE'),
    (gen_random_uuid(), 'MEDICAL_LEAVE', 'Baja médica',
     'Ausencia por enfermedad o incapacidad respaldada por certificado.', TRUE, TRUE, TRUE, FALSE, 'ACTIVE'),
    (gen_random_uuid(), 'PERSONAL_PERMISSION', 'Permiso personal',
     'Permiso solicitado por motivos personales.', FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
    (gen_random_uuid(), 'MATERNITY_LEAVE', 'Licencia de maternidad',
     'Licencia remunerada por maternidad con respaldo documental.', TRUE, TRUE, TRUE, FALSE, 'ACTIVE'),
    (gen_random_uuid(), 'PATERNITY_LEAVE', 'Licencia de paternidad',
     'Licencia remunerada por paternidad con respaldo documental.', TRUE, TRUE, TRUE, FALSE, 'ACTIVE'),
    (gen_random_uuid(), 'UNPAID_LEAVE', 'Licencia sin goce de haberes',
     'Ausencia autorizada sin remuneración.', FALSE, TRUE, FALSE, FALSE, 'ACTIVE')
ON CONFLICT (code) DO NOTHING;
