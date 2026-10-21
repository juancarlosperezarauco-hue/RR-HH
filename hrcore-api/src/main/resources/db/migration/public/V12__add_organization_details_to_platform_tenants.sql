ALTER TABLE public.platform_tenants
    ADD COLUMN IF NOT EXISTS legal_name VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS tax_identifier VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(150) NULL,
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS owner_name VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS owner_email VARCHAR(150) NULL;

CREATE INDEX IF NOT EXISTS idx_platform_tenants_tax_identifier
    ON public.platform_tenants(tax_identifier);

CREATE INDEX IF NOT EXISTS idx_platform_tenants_legal_name
    ON public.platform_tenants(legal_name);

-- Backfill from tenant schemas if they exist
DO $$
DECLARE
    r RECORD;
    v_legal_name VARCHAR(200);
    v_tax_identifier VARCHAR(50);
    v_contact_email VARCHAR(150);
    v_contact_phone VARCHAR(50);
    v_owner_name VARCHAR(200);
    v_owner_email VARCHAR(150);
BEGIN
    FOR r IN SELECT id, schema_name FROM public.platform_tenants WHERE schema_name IS NOT NULL LOOP
        IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = r.schema_name) THEN
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = r.schema_name AND table_name = 'tenant_settings') THEN
                EXECUTE format('SELECT setting_value FROM %I.tenant_settings WHERE setting_key = ''company.legal_name'' LIMIT 1', r.schema_name) INTO v_legal_name;
                EXECUTE format('SELECT setting_value FROM %I.tenant_settings WHERE setting_key = ''company.tax_identifier'' LIMIT 1', r.schema_name) INTO v_tax_identifier;
                EXECUTE format('SELECT setting_value FROM %I.tenant_settings WHERE setting_key = ''company.contact_email'' LIMIT 1', r.schema_name) INTO v_contact_email;
                EXECUTE format('SELECT setting_value FROM %I.tenant_settings WHERE setting_key = ''company.contact_phone'' LIMIT 1', r.schema_name) INTO v_contact_phone;
            END IF;

            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = r.schema_name AND table_name = 'tenant_authorization_state')
               AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = r.schema_name AND table_name = 'tenant_users') THEN
                EXECUTE format('SELECT u.first_name || '' '' || u.last_name, u.email FROM %I.tenant_authorization_state s JOIN %I.tenant_users u ON u.id = s.owner_user_id WHERE s.id = 1 LIMIT 1', r.schema_name, r.schema_name) INTO v_owner_name, v_owner_email;
            END IF;

            UPDATE public.platform_tenants
            SET legal_name = COALESCE(legal_name, v_legal_name),
                tax_identifier = COALESCE(tax_identifier, v_tax_identifier),
                contact_email = COALESCE(contact_email, v_contact_email, v_owner_email),
                contact_phone = COALESCE(contact_phone, v_contact_phone),
                owner_name = COALESCE(owner_name, v_owner_name),
                owner_email = COALESCE(owner_email, v_owner_email)
            WHERE id = r.id;
        END IF;
    END LOOP;
END $$;
