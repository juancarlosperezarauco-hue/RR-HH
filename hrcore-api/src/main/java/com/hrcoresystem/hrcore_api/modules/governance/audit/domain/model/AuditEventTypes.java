package com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model;

public final class AuditEventTypes {

    public static final String TENANT_CREATED = "TENANT_CREATED";
    public static final String TENANT_ACTIVATED = "TENANT_ACTIVATED";
    public static final String TENANT_DEACTIVATED = "TENANT_DEACTIVATED";
    public static final String TENANT_SETTINGS_UPDATED = "TENANT_SETTINGS_UPDATED";

    public static final String SUBSCRIPTION_ASSIGNED = "SUBSCRIPTION_ASSIGNED";
    public static final String SUBSCRIPTION_EXPIRED = "SUBSCRIPTION_EXPIRED";
    public static final String PUBLIC_SIGNUP_COMPLETED = "PUBLIC_SIGNUP_COMPLETED";
    public static final String PUBLIC_SIGNUP_CHECKOUT_CREATED = "PUBLIC_SIGNUP_CHECKOUT_CREATED";
    public static final String PLATFORM_PLAN_CREATED = "PLATFORM_PLAN_CREATED";
    public static final String PLATFORM_PLAN_ACTIVATED = "PLATFORM_PLAN_ACTIVATED";
    public static final String PLATFORM_PLAN_DEACTIVATED = "PLATFORM_PLAN_DEACTIVATED";

    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_ACTIVATED = "USER_ACTIVATED";
    public static final String USER_DEACTIVATED = "USER_DEACTIVATED";

    public static final String ROLE_CREATED = "ROLE_CREATED";
    public static final String ROLE_UPDATED = "ROLE_UPDATED";
    public static final String ROLE_ACTIVATED = "ROLE_ACTIVATED";
    public static final String ROLE_DEACTIVATED = "ROLE_DEACTIVATED";
    public static final String USER_ROLES_ASSIGNED = "USER_ROLES_ASSIGNED";

    public static final String LOGIN = "LOGIN";
    public static final String TOKEN_REFRESHED = "TOKEN_REFRESHED";
    public static final String LOGOUT = "LOGOUT";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String PASSWORD_RESET_COMPLETED = "PASSWORD_RESET_COMPLETED";

    //NEW EVENTS
    public static final String ACCOUNT_CREATED = "ACCOUNT_CREATED";
    public static final String ACCOUNT_UPDATED = "ACCOUNT_UPDATED";
    public static final String ACCOUNT_ACTIVATED = "ACCOUNT_ACTIVATED";
    public static final String ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED";
    public static final String ACCOUNT_FROZEN = "ACCOUNT_FROZEN";
    public static final String ACCOUNT_CLOSED = "ACCOUNT_CLOSED";
    public static final String ACCOUNT_ALIAS_UPDATED = "ACCOUNT_ALIAS_UPDATED";
    public static final String ACCOUNT_APPROVAL_REQUESTED = "ACCOUNT_APPROVAL_REQUESTED";
    public static final String ACCOUNTING_PERIOD_CREATED = "ACCOUNTING_PERIOD_CREATED";
    public static final String ACCOUNTING_PERIOD_CLOSED = "ACCOUNTING_PERIOD_CLOSED";
    public static final String LIMIT_RULE_CREATED = "LIMIT_RULE_CREATED";
    public static final String LIMIT_RULE_UPDATED = "LIMIT_RULE_UPDATED";
    public static final String LIMIT_RULE_DEACTIVATED = "LIMIT_RULE_DEACTIVATED";

    public static final String LOAN_REQUESTED = "LOAN_REQUESTED";
    public static final String LOAN_APPROVED = "LOAN_APPROVED";
    public static final String LOAN_REJECTED = "LOAN_REJECTED";
    public static final String LOAN_DISBURSED = "LOAN_DISBURSED";
    public static final String LOAN_PAYMENT_RECORDED = "LOAN_PAYMENT_RECORDED";
    public static final String LOAN_PAID_OFF = "LOAN_PAID_OFF";

    public static final String TRANSACTION_CREATED = "TRANSACTION_CREATED";
    public static final String TRANSACTION_COMPLETED = "TRANSACTION_COMPLETED";
    public static final String TRANSACTION_FAILED = "TRANSACTION_FAILED";
    public static final String TRANSACTION_UPDATED = "TRANSACTION_UPDATED";

    public static final String SERVICE_PROVIDER_CREATED = "SERVICE_PROVIDER_CREATED";
    public static final String SERVICE_PROVIDER_UPDATED = "SERVICE_PROVIDER_UPDATED";
    public static final String SERVICE_PROVIDER_STATUS_CHANGED = "SERVICE_PROVIDER_STATUS_CHANGED";
    public static final String SERVICE_CUSTOMER_CREATED = "SERVICE_CUSTOMER_CREATED";
    public static final String SERVICE_CUSTOMER_UPDATED = "SERVICE_CUSTOMER_UPDATED";
    public static final String SERVICE_BILL_CREATED = "SERVICE_BILL_CREATED";
    public static final String SERVICE_BILL_CANCELLED = "SERVICE_BILL_CANCELLED";
    public static final String SERVICE_ENROLLMENT_CREATED = "SERVICE_ENROLLMENT_CREATED";
    public static final String SERVICE_ENROLLMENT_DELETED = "SERVICE_ENROLLMENT_DELETED";
    public static final String SERVICE_BILL_QUERIED = "SERVICE_BILL_QUERIED";
    public static final String SERVICE_PAYMENT_CREATED = "SERVICE_PAYMENT_CREATED";
    public static final String SERVICE_PAYMENT_ASSISTED_CREATED = "SERVICE_PAYMENT_ASSISTED_CREATED";
    public static final String SERVICE_PAYMENT_COMPLETED = "SERVICE_PAYMENT_COMPLETED";
    public static final String SERVICE_PAYMENT_FAILED = "SERVICE_PAYMENT_FAILED";

    public static final String SUBSCRIPTION_CHECKOUT_COMPLETED = "SUBSCRIPTION_CHECKOUT_COMPLETED";
    public static final String SUBSCRIPTION_PAYMENT_SUCCEEDED = "SUBSCRIPTION_PAYMENT_SUCCEEDED";
    public static final String SUBSCRIPTION_PAYMENT_FAILED = "SUBSCRIPTION_PAYMENT_FAILED";
    public static final String SUBSCRIPTION_UPGRADED = "SUBSCRIPTION_UPGRADED";
    public static final String SUBSCRIPTION_RENEWED = "SUBSCRIPTION_RENEWED";
    public static final String SUBSCRIPTION_CANCELLED = "SUBSCRIPTION_CANCELLED";
    public static final String STRIPE_WEBHOOK_RECEIVED = "STRIPE_WEBHOOK_RECEIVED";
    public static final String STRIPE_WEBHOOK_PROCESSED = "STRIPE_WEBHOOK_PROCESSED";

    private AuditEventTypes() {
    }
}
