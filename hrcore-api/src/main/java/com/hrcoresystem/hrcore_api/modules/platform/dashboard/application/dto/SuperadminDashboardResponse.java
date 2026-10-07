package com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SuperadminDashboardResponse(
        Metadata metadata,
        Filters filters,
        Comparisons comparisons,
        Summary summary,
        TenantsSection tenants,
        SubscriptionsSection subscriptions,
        PlansSection plans,
        UsersSection users,
        AuditSection audit,
        List<AlertItem> alerts,
        List<InsightItem> insights,
        List<ActivityItem> recentActivity
) {

    public record Metadata(
            OffsetDateTime generatedAt,
            String timezone,
            String dataCompleteness
    ) {
    }

    public record Filters(
            String period,
            Integer year,
            Integer month,
            LocalDate date,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
    }

    public record Comparisons(
            PeriodRange previousPeriod,
            ComparisonSummary summary
    ) {
    }

    public record PeriodRange(
            OffsetDateTime from,
            OffsetDateTime to
    ) {
    }

    public record ComparisonSummary(
            BigDecimal tenantsChangePercent,
            BigDecimal activeSubscriptionsChangePercent,
            BigDecimal newTenantsChangePercent,
            BigDecimal expiredSubscriptionsChangePercent
    ) {
    }

    public record Summary(
            TenantsSummary tenants,
            SubscriptionsSummary subscriptions,
            PlansSummary plans,
            UsersSummary users,
            AuditSummary audit,
            SystemSummary system
    ) {
    }

    public record TenantsSummary(
            long total,
            long active,
            long inactive,
            long newThisPeriod
    ) {
    }

    public record SubscriptionsSummary(
            long active,
            long trial,
            long expired,
            long cancelled,
            long expiringSoon
    ) {
    }

    public record PlansSummary(
            long total,
            long active
    ) {
    }

    public record UsersSummary(
            long total,
            long active,
            long inactive,
            long platformAdmins
    ) {
    }

    public record AuditSummary(
            long events,
            long failedEvents
    ) {
    }

    public record SystemSummary(
            long registeredTenantSchemas,
            long activeTenantSchemas,
            long inactiveTenantSchemas
    ) {
    }

    public record TenantsSection(
            List<TenantStatusItem> byStatus,
            List<TenantItem> recent,
            List<TenantItem> inactive
    ) {
    }

    public record TenantStatusItem(
            String status,
            String label,
            long total
    ) {
    }

    public record TenantItem(
            UUID id,
            String name,
            String slug,
            String schemaName,
            String status,
            String planCode,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record SubscriptionsSection(
            List<SubscriptionStatusItem> byStatus,
            List<SubscriptionPlanItem> byPlan,
            SectionBucket expiringSoon,
            SectionBucket expired
    ) {
    }

    public record SubscriptionStatusItem(
            String status,
            long total
    ) {
    }

    public record SubscriptionPlanItem(
            String planCode,
            long total
    ) {
    }

    public record SectionBucket(
            long total,
            List<SubscriptionItem> items
    ) {
    }

    public record SubscriptionItem(
            UUID id,
            UUID tenantId,
            String tenantName,
            String tenantSlug,
            String planCode,
            String status,
            OffsetDateTime expiresAt,
            OffsetDateTime expiredAt
    ) {
    }

    public record PlansSection(
            List<PlanItem> items
    ) {
    }

    public record PlanItem(
            String code,
            String name,
            String type,
            boolean active,
            int maxUsers,
            int maxRoles,
            Integer trialDays,
            long tenants
    ) {
    }

    public record UsersSection(
            long total,
            long active,
            long inactive,
            long platformAdmins
    ) {
    }

    public record AuditSection(
            List<AuditEventItem> byEventType,
            SectionBucketRecent recent
    ) {
    }

    public record AuditEventItem(
            String eventType,
            long total
    ) {
    }

    public record SectionBucketRecent(
            long total,
            List<AuditItem> items
    ) {
    }

    public record AuditItem(
            UUID id,
            String actorEmail,
            String eventType,
            String resourceType,
            String resourceId,
            String outcome,
            OffsetDateTime createdAt
    ) {
    }

    public record AlertItem(
            String id,
            String type,
            String severity,
            String title,
            String description,
            OffsetDateTime createdAt
    ) {
    }

    public record InsightItem(
            String type,
            String severity,
            String title,
            String description
    ) {
    }

    public record ActivityItem(
            String id,
            String type,
            String title,
            String description,
            String actorName,
            OffsetDateTime createdAt
    ) {
    }
}
