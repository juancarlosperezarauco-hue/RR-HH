ALTER TABLE public.platform_plans
    ADD COLUMN IF NOT EXISTS plan_type VARCHAR(20) NOT NULL DEFAULT 'PAID';

ALTER TABLE public.platform_plans
    ADD COLUMN IF NOT EXISTS trial_days INTEGER NULL;

ALTER TABLE public.platform_plans
    ADD COLUMN IF NOT EXISTS monthly_amount NUMERIC(19, 4) NULL;

ALTER TABLE public.platform_plans
    ADD COLUMN IF NOT EXISTS yearly_amount NUMERIC(19, 4) NULL;

ALTER TABLE public.platform_plans
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'USD';

ALTER TABLE public.platform_plans
    ADD COLUMN IF NOT EXISTS public_visible BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE public.platform_plans
    ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_platform_plans_public_visible
    ON public.platform_plans(public_visible);

CREATE INDEX IF NOT EXISTS idx_platform_plans_sort_order
    ON public.platform_plans(sort_order);


ALTER TABLE public.platform_tenants
    ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(120) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_tenants_stripe_customer_id
    ON public.platform_tenants(stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;


CREATE TABLE IF NOT EXISTS public.platform_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    tenant_id UUID NOT NULL REFERENCES public.platform_tenants(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES public.platform_plans(id),

    status VARCHAR(30) NOT NULL,

    is_trial BOOLEAN NOT NULL DEFAULT FALSE,
    current_subscription BOOLEAN NOT NULL DEFAULT TRUE,

    stripe_subscription_id VARCHAR(120) NULL,
    stripe_price_id VARCHAR(120) NULL,

    billing_interval VARCHAR(20) NULL,

    started_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NULL,

    current_period_start TIMESTAMPTZ NULL,
    current_period_end TIMESTAMPTZ NULL,

    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_platform_subscriptions_status CHECK (
        status IN (
            'TRIAL',
            'ACTIVE',
            'PAST_DUE',
            'SUSPENDED',
            'INCOMPLETE',
            'EXPIRED',
            'CANCELLED'
        )
    ),

    CONSTRAINT chk_platform_subscriptions_billing_interval CHECK (
        billing_interval IS NULL OR billing_interval IN ('MONTHLY', 'YEARLY')
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_subscriptions_current_per_tenant
    ON public.platform_subscriptions(tenant_id)
    WHERE current_subscription = true;

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_subscriptions_stripe_subscription_id
    ON public.platform_subscriptions(stripe_subscription_id)
    WHERE stripe_subscription_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_platform_subscriptions_tenant_id
    ON public.platform_subscriptions(tenant_id);

CREATE INDEX IF NOT EXISTS idx_platform_subscriptions_plan_id
    ON public.platform_subscriptions(plan_id);

CREATE INDEX IF NOT EXISTS idx_platform_subscriptions_status
    ON public.platform_subscriptions(status);

CREATE INDEX IF NOT EXISTS idx_platform_subscriptions_stripe_price_id
    ON public.platform_subscriptions(stripe_price_id);

CREATE INDEX IF NOT EXISTS idx_platform_subscriptions_current_period_end
    ON public.platform_subscriptions(current_period_end);


CREATE TABLE IF NOT EXISTS public.subscription_checkout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    tenant_id UUID NOT NULL REFERENCES public.platform_tenants(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES public.platform_plans(id),

    requested_by_user_id UUID NULL,
    requested_by_email VARCHAR(255) NULL,

    billing_interval VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    stripe_customer_id VARCHAR(120) NULL,
    stripe_session_id VARCHAR(120) NOT NULL,
    stripe_subscription_id VARCHAR(120) NULL,
    stripe_price_id VARCHAR(120) NULL,

    checkout_url TEXT NULL,
    success_url TEXT NULL,
    cancel_url TEXT NULL,

    amount NUMERIC(19, 4) NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',

    completed_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_subscription_checkout_sessions_billing_interval CHECK (
        billing_interval IN ('MONTHLY', 'YEARLY')
    ),

    CONSTRAINT chk_subscription_checkout_sessions_status CHECK (
        status IN ('PENDING', 'OPEN', 'COMPLETED', 'EXPIRED', 'CANCELLED', 'FAILED')
    ),

    CONSTRAINT chk_subscription_checkout_sessions_amount CHECK (
        amount IS NULL OR amount >= 0
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_checkout_sessions_stripe_session_id
    ON public.subscription_checkout_sessions(stripe_session_id);

CREATE INDEX IF NOT EXISTS idx_subscription_checkout_sessions_tenant_id
    ON public.subscription_checkout_sessions(tenant_id);

CREATE INDEX IF NOT EXISTS idx_subscription_checkout_sessions_plan_id
    ON public.subscription_checkout_sessions(plan_id);

CREATE INDEX IF NOT EXISTS idx_subscription_checkout_sessions_status
    ON public.subscription_checkout_sessions(status);

CREATE INDEX IF NOT EXISTS idx_subscription_checkout_sessions_stripe_subscription_id
    ON public.subscription_checkout_sessions(stripe_subscription_id);

CREATE INDEX IF NOT EXISTS idx_subscription_checkout_sessions_requested_by_user_id
    ON public.subscription_checkout_sessions(requested_by_user_id);

CREATE INDEX IF NOT EXISTS idx_subscription_checkout_sessions_created_at
    ON public.subscription_checkout_sessions(created_at DESC);


CREATE TABLE IF NOT EXISTS public.subscription_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    tenant_id UUID NOT NULL REFERENCES public.platform_tenants(id) ON DELETE CASCADE,
    platform_subscription_id UUID NULL REFERENCES public.platform_subscriptions(id) ON DELETE SET NULL,
    plan_id UUID NULL REFERENCES public.platform_plans(id) ON DELETE SET NULL,

    stripe_customer_id VARCHAR(120) NULL,
    stripe_subscription_id VARCHAR(120) NULL,
    stripe_invoice_id VARCHAR(120) NULL,
    stripe_payment_intent_id VARCHAR(120) NULL,
    stripe_charge_id VARCHAR(120) NULL,

    invoice_number VARCHAR(120) NULL,
    hosted_invoice_url TEXT NULL,
    invoice_pdf_url TEXT NULL,

    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL,

    billing_reason VARCHAR(80) NULL,

    paid_at TIMESTAMPTZ NULL,
    failed_at TIMESTAMPTZ NULL,
    failure_reason TEXT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_subscription_payments_status CHECK (
        status IN ('PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'VOID')
    ),

    CONSTRAINT chk_subscription_payments_amount CHECK (
        amount >= 0
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_payments_stripe_invoice_id
    ON public.subscription_payments(stripe_invoice_id)
    WHERE stripe_invoice_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_subscription_payments_tenant_id
    ON public.subscription_payments(tenant_id);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_platform_subscription_id
    ON public.subscription_payments(platform_subscription_id);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_plan_id
    ON public.subscription_payments(plan_id);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_status
    ON public.subscription_payments(status);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_paid_at
    ON public.subscription_payments(paid_at DESC);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_created_at
    ON public.subscription_payments(created_at DESC);


CREATE TABLE IF NOT EXISTS public.stripe_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    stripe_event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,

    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMPTZ NULL,

    processing_attempts INTEGER NOT NULL DEFAULT 0,

    last_error TEXT NULL,
    payload JSONB NULL,

    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_stripe_webhook_events_processing_attempts CHECK (
        processing_attempts >= 0
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stripe_webhook_events_stripe_event_id
    ON public.stripe_webhook_events(stripe_event_id);

CREATE INDEX IF NOT EXISTS idx_stripe_webhook_events_event_type
    ON public.stripe_webhook_events(event_type);

CREATE INDEX IF NOT EXISTS idx_stripe_webhook_events_processed
    ON public.stripe_webhook_events(processed);

CREATE INDEX IF NOT EXISTS idx_stripe_webhook_events_received_at
    ON public.stripe_webhook_events(received_at DESC);