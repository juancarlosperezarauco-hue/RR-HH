ALTER TABLE work_calendar_exceptions
    ADD COLUMN IF NOT EXISTS override_shift_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_calendar_exception_override_shift'
    ) THEN
        ALTER TABLE work_calendar_exceptions
            ADD CONSTRAINT fk_calendar_exception_override_shift
            FOREIGN KEY (override_shift_id) REFERENCES work_shifts(id) ON DELETE RESTRICT;
    END IF;
END $$;

ALTER TABLE work_calendar_exceptions
    DROP CONSTRAINT IF EXISTS chk_calendar_exception_override;

ALTER TABLE work_calendar_exceptions
    ADD CONSTRAINT chk_calendar_exception_override CHECK (
        (exception_type IN ('HOLIDAY', 'NON_WORKING_DAY', 'OTHER') AND override_shift_id IS NULL)
        OR (exception_type = 'SCHEDULE_CHANGE' AND work_required = TRUE AND override_shift_id IS NOT NULL)
        OR (exception_type = 'SPECIAL_WORKING_DAY' AND work_required = TRUE)
    ) NOT VALID;

CREATE INDEX IF NOT EXISTS idx_calendar_exceptions_override_shift
    ON work_calendar_exceptions(override_shift_id);
