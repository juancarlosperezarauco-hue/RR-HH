-- Identidad persistente del solicitante de horas extras.
ALTER TABLE attendance_overtime
    ADD COLUMN IF NOT EXISTS requested_by UUID;

ALTER TABLE attendance_overtime
    ADD CONSTRAINT fk_attendance_overtime_requested_by
    FOREIGN KEY (requested_by) REFERENCES tenant_users(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_attendance_overtime_requested_by
    ON attendance_overtime(requested_by);
