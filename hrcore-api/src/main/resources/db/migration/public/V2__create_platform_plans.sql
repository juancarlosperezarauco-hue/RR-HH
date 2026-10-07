CREATE TABLE IF NOT EXISTS public.platform_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),

    max_users INTEGER NOT NULL DEFAULT 10,
    max_roles INTEGER NOT NULL DEFAULT 5,

    plan_type VARCHAR(20) NOT NULL DEFAULT 'PAID',
    trial_days INTEGER NULL,

    monthly_amount NUMERIC(19, 4) NULL,
    yearly_amount NUMERIC(19, 4) NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',

    public_visible BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_platform_plans_plan_type CHECK (
        plan_type IN ('DEMO', 'PAID', 'ENTERPRISE')
    ),

    CONSTRAINT chk_platform_plans_trial_days CHECK (
        trial_days IS NULL OR trial_days >= 0
    ),

    CONSTRAINT chk_platform_plans_monthly_amount CHECK (
        monthly_amount IS NULL OR monthly_amount >= 0
    ),

    CONSTRAINT chk_platform_plans_yearly_amount CHECK (
        yearly_amount IS NULL OR yearly_amount >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_platform_plans_code
    ON public.platform_plans(code);

CREATE INDEX IF NOT EXISTS idx_platform_plans_plan_type
    ON public.platform_plans(plan_type);

CREATE INDEX IF NOT EXISTS idx_platform_plans_active
    ON public.platform_plans(active);

CREATE INDEX IF NOT EXISTS idx_platform_plans_public_visible
    ON public.platform_plans(public_visible);

CREATE INDEX IF NOT EXISTS idx_platform_plans_sort_order
    ON public.platform_plans(sort_order);