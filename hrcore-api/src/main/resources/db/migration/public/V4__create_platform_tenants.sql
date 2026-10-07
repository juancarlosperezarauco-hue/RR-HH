CREATE TABLE IF NOT EXISTS public.platform_tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    schema_name VARCHAR(128) NOT NULL UNIQUE,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    plan_id UUID NULL REFERENCES public.platform_plans(id),

    stripe_customer_id VARCHAR(120) NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_platform_tenants_slug
    ON public.platform_tenants(slug);

CREATE INDEX IF NOT EXISTS idx_platform_tenants_schema_name
    ON public.platform_tenants(schema_name);

CREATE INDEX IF NOT EXISTS idx_platform_tenants_status
    ON public.platform_tenants(status);

CREATE INDEX IF NOT EXISTS idx_platform_tenants_plan_id
    ON public.platform_tenants(plan_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_tenants_stripe_customer_id
    ON public.platform_tenants(stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;
