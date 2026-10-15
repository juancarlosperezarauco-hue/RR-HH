UPDATE public.platform_plans
SET public_visible = TRUE,
    active = TRUE,
    updated_at = NOW()
WHERE code = 'DEMO';
