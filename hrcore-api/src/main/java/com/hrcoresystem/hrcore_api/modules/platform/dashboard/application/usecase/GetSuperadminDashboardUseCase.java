package com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.PlatformAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.PlatformAuditEventRepository;
import com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.dto.SuperadminDashboardResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GetSuperadminDashboardUseCase {

    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("America/La_Paz");
    private static final int MAX_RECENT_ITEMS = 5;
    private static final int MAX_ALERT_ITEMS = 5;

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSuperadminRepository platformSuperadminRepository;
    private final PlatformAuditEventRepository platformAuditEventRepository;

    public GetSuperadminDashboardUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformPlanRepository platformPlanRepository,
            PlatformSuperadminRepository platformSuperadminRepository,
            PlatformAuditEventRepository platformAuditEventRepository
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.platformSuperadminRepository = platformSuperadminRepository;
        this.platformAuditEventRepository = platformAuditEventRepository;
    }

    @Transactional(readOnly = true)
    public SuperadminDashboardResponse execute() {
        OffsetDateTime generatedAt = OffsetDateTime.now(DASHBOARD_ZONE);
        PeriodWindow currentMonth = currentMonthWindow(generatedAt);
        PeriodWindow previousMonth = previousMonthWindow(generatedAt);

        List<PlatformTenant> tenants = platformTenantRepository.findAll();
        List<PlatformSubscription> subscriptions = platformSubscriptionRepository.findAll();
        List<PlatformPlan> plans = platformPlanRepository.findAll();
        List<com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin> platformUsers = platformSuperadminRepository.findAll();
        List<PlatformAuditEvent> auditEvents = platformAuditEventRepository.findAll();
        List<PlatformAuditEvent> recentAudit = platformAuditEventRepository.findRecent(MAX_RECENT_ITEMS);
        long platformAdmins = platformUsers.size();

        List<PlatformTenant> recentTenants = tenants.stream()
                .sorted(Comparator.comparing(PlatformTenant::createdAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(MAX_RECENT_ITEMS)
                .toList();

        List<PlatformTenant> inactiveTenants = tenants.stream()
                .filter(tenant -> tenant.status() == PlatformTenantStatus.INACTIVE)
                .sorted(Comparator.comparing(PlatformTenant::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(MAX_RECENT_ITEMS)
                .toList();

        long totalTenants = tenants.size();
        long activeTenants = tenants.stream().filter(tenant -> tenant.status() == PlatformTenantStatus.ACTIVE).count();
        long inactiveTenantsCount = totalTenants - activeTenants;
        long newTenantsThisPeriod = countCreatedInWindow(tenants, currentMonth);
        long previousNewTenants = countCreatedInWindow(tenants, previousMonth);

        long activeSubscriptions = subscriptions.stream()
                .filter(subscription -> subscription.status() == PlatformSubscriptionStatus.ACTIVE)
                .count();
        long trialSubscriptions = subscriptions.stream()
                .filter(subscription -> subscription.status() == PlatformSubscriptionStatus.TRIAL)
                .count();
        long expiredSubscriptions = subscriptions.stream()
                .filter(subscription -> subscription.status() == PlatformSubscriptionStatus.EXPIRED)
                .count();
        long cancelledSubscriptions = subscriptions.stream()
                .filter(subscription -> subscription.status() == PlatformSubscriptionStatus.CANCELLED)
                .count();
        java.time.Instant nowInstant = generatedAt.toInstant();
        List<PlatformSubscription> expiringSoonSubscriptions = subscriptions.stream()
                .filter(subscription -> subscription.expiresAt() != null)
                .filter(subscription -> !subscription.expiresAt().isBefore(nowInstant))
                .filter(subscription -> !subscription.expiresAt().isAfter(nowInstant.plus(14, ChronoUnit.DAYS)))
                .sorted(Comparator.comparing(PlatformSubscription::expiresAt))
                .limit(MAX_RECENT_ITEMS)
                .toList();
        List<PlatformSubscription> expiredSoonSubscriptions = subscriptions.stream()
                .filter(subscription -> subscription.status() == PlatformSubscriptionStatus.EXPIRED)
                .sorted(Comparator.comparing(PlatformSubscription::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(MAX_RECENT_ITEMS)
                .toList();

        long totalPlans = plans.size();
        long activePlans = plans.stream().filter(PlatformPlan::active).count();
        Map<String, Long> tenantsByPlan = countTenantsByPlan(tenants, plans);

        long totalUsers = platformUsers.size();
        long activeUsers = platformUsers.stream().filter(com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin::active).count();
        long inactiveUsers = totalUsers - activeUsers;

        long auditCount = platformAuditEventRepository.count();
        long failedAuditCount = auditEvents.stream()
                .filter(event -> event.outcome() != null && event.outcome().equalsIgnoreCase("FAILED"))
                .count();

        PeriodRangeSummary comparison = new PeriodRangeSummary(
                countCreatedInWindow(tenants, previousMonth),
                countActiveSubscriptionsInWindow(subscriptions, previousMonth),
                countCreatedInWindow(tenants, previousMonth),
                countExpiredSubscriptionsInWindow(subscriptions, previousMonth)
        );

        List<SuperadminDashboardResponse.AlertItem> alerts = buildAlerts(
                inactiveTenantsCount,
                expiringSoonSubscriptions.size(),
                expiredSubscriptions,
                failedAuditCount,
                activeTenants,
                totalTenants,
                generatedAt
        );

        List<SuperadminDashboardResponse.InsightItem> insights = buildInsights(
                activeTenants,
                inactiveTenantsCount,
                activeSubscriptions,
                trialSubscriptions,
                activePlans,
                totalPlans
        );

        return new SuperadminDashboardResponse(
                new SuperadminDashboardResponse.Metadata(
                        generatedAt,
                        DASHBOARD_ZONE.getId(),
                        "COMPLETE"
                ),
                buildFilters(currentMonth),
                new SuperadminDashboardResponse.Comparisons(
                        new SuperadminDashboardResponse.PeriodRange(previousMonth.from(), previousMonth.to()),
                        new SuperadminDashboardResponse.ComparisonSummary(
                                percentChange(totalTenants, tenants.size()),
                                percentChange(activeSubscriptions, countActiveSubscriptionsInWindow(subscriptions, previousMonth)),
                                percentChange(newTenantsThisPeriod, previousNewTenants),
                                percentChange(expiredSubscriptions, countExpiredSubscriptionsInWindow(subscriptions, previousMonth))
                        )
                ),
                new SuperadminDashboardResponse.Summary(
                        new SuperadminDashboardResponse.TenantsSummary(
                                totalTenants,
                                activeTenants,
                                inactiveTenantsCount,
                                newTenantsThisPeriod
                        ),
                        new SuperadminDashboardResponse.SubscriptionsSummary(
                                activeSubscriptions,
                                trialSubscriptions,
                                expiredSubscriptions,
                                cancelledSubscriptions,
                                expiringSoonSubscriptions.size()
                        ),
                        new SuperadminDashboardResponse.PlansSummary(
                                totalPlans,
                                activePlans
                        ),
                        new SuperadminDashboardResponse.UsersSummary(
                                totalUsers,
                                activeUsers,
                                inactiveUsers,
                                platformAdmins
                        ),
                        new SuperadminDashboardResponse.AuditSummary(
                                auditCount,
                                failedAuditCount
                        ),
                        new SuperadminDashboardResponse.SystemSummary(
                                totalTenants,
                                activeTenants,
                                inactiveTenantsCount
                        )
                ),
                new SuperadminDashboardResponse.TenantsSection(
                        List.of(
                                new SuperadminDashboardResponse.TenantStatusItem("ACTIVE", "Activos", activeTenants),
                                new SuperadminDashboardResponse.TenantStatusItem("INACTIVE", "Inactivos", inactiveTenantsCount)
                        ),
                        recentTenants.stream().map(this::toTenantItem).toList(),
                        inactiveTenants.stream().map(this::toTenantItem).toList()
                ),
                new SuperadminDashboardResponse.SubscriptionsSection(
                        List.of(
                                new SuperadminDashboardResponse.SubscriptionStatusItem("ACTIVE", activeSubscriptions),
                                new SuperadminDashboardResponse.SubscriptionStatusItem("TRIAL", trialSubscriptions),
                                new SuperadminDashboardResponse.SubscriptionStatusItem("EXPIRED", expiredSubscriptions),
                                new SuperadminDashboardResponse.SubscriptionStatusItem("CANCELLED", cancelledSubscriptions)
                        ),
                        plans.stream()
                                .sorted(Comparator.comparing(PlatformPlan::code))
                                .map(plan -> new SuperadminDashboardResponse.SubscriptionPlanItem(
                                        plan.code(),
                                        tenantsByPlan.getOrDefault(plan.code(), 0L)
                                ))
                                .toList(),
                        new SuperadminDashboardResponse.SectionBucket(
                                expiringSoonSubscriptions.size(),
                                expiringSoonSubscriptions.stream().map(this::toSubscriptionItem).toList()
                        ),
                        new SuperadminDashboardResponse.SectionBucket(
                                expiredSoonSubscriptions.size(),
                                expiredSoonSubscriptions.stream().map(this::toSubscriptionItem).toList()
                        )
                ),
                new SuperadminDashboardResponse.PlansSection(
                        plans.stream()
                                .sorted(Comparator.comparing(PlatformPlan::code))
                                .map(plan -> new SuperadminDashboardResponse.PlanItem(
                                        plan.code(),
                                        plan.name(),
                                        plan.planType(),
                                        plan.active(),
                                        plan.maxUsers(),
                                        plan.maxRoles(),
                                        plan.trialDays(),
                                        tenantsByPlan.getOrDefault(plan.code(), 0L)
                                ))
                                .toList()
                ),
                new SuperadminDashboardResponse.UsersSection(
                        totalUsers,
                        activeUsers,
                        inactiveUsers,
                        platformAdmins
                ),
                new SuperadminDashboardResponse.AuditSection(
                        buildAuditEventTypes(auditEvents),
                        new SuperadminDashboardResponse.SectionBucketRecent(
                                recentAudit.size(),
                                recentAudit.stream().map(this::toAuditItem).toList()
                        )
                ),
                alerts,
                insights,
                recentAudit.stream().map(this::toActivityItem).toList()
        );
    }

    private SuperadminDashboardResponse.Filters buildFilters(PeriodWindow currentMonth) {
        return new SuperadminDashboardResponse.Filters(
                "MONTH",
                currentMonth.from().getYear(),
                currentMonth.from().getMonthValue(),
                null,
                currentMonth.from(),
                currentMonth.to()
        );
    }

    private long countCreatedInWindow(List<PlatformTenant> tenants, PeriodWindow window) {
        return tenants.stream()
                .filter(tenant -> isInWindow(tenant.createdAt(), window))
                .count();
    }

    private long countActiveSubscriptionsInWindow(List<PlatformSubscription> subscriptions, PeriodWindow window) {
        return subscriptions.stream()
                .filter(subscription -> subscription.status() == PlatformSubscriptionStatus.ACTIVE)
                .filter(subscription -> isInWindow(subscription.startedAt(), window))
                .count();
    }

    private long countExpiredSubscriptionsInWindow(List<PlatformSubscription> subscriptions, PeriodWindow window) {
        return subscriptions.stream()
                .filter(subscription -> subscription.status() == PlatformSubscriptionStatus.EXPIRED)
                .filter(subscription -> isInWindow(subscription.updatedAt(), window))
                .count();
    }

    private Map<String, Long> countTenantsByPlan(List<PlatformTenant> tenants, List<PlatformPlan> plans) {
        Map<String, String> planCodesById = plans.stream()
                .filter(Objects::nonNull)
                .filter(plan -> plan.id() != null && plan.code() != null)
                .collect(LinkedHashMap::new, (map, plan) -> map.put(plan.id().toString(), plan.code()), Map::putAll);
        Map<String, Long> totals = new LinkedHashMap<>();
        for (PlatformTenant tenant : tenants) {
            String planCode = planCodesById.get(tenant.planId() != null ? tenant.planId().toString() : null);
            if (planCode == null) {
                planCode = "UNKNOWN";
            }
            totals.merge(planCode, 1L, Long::sum);
        }
        return totals;
    }

    private List<SuperadminDashboardResponse.AuditEventItem> buildAuditEventTypes(List<PlatformAuditEvent> auditEvents) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (PlatformAuditEvent event : auditEvents) {
            String eventType = safeText(event.eventType());
            if (eventType == null) {
                eventType = "UNKNOWN";
            }
            totals.merge(eventType, 1L, Long::sum);
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> new SuperadminDashboardResponse.AuditEventItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<SuperadminDashboardResponse.AlertItem> buildAlerts(
            long inactiveTenants,
            long expiringSoonSubscriptions,
            long expiredSubscriptions,
            long failedAuditEvents,
            long activeTenants,
            long totalTenants,
            OffsetDateTime generatedAt
    ) {
        List<SuperadminDashboardResponse.AlertItem> alerts = new java.util.ArrayList<>();
        if (inactiveTenants > 0) {
            alerts.add(new SuperadminDashboardResponse.AlertItem(
                    "INACTIVE_TENANTS",
                    "WARN",
                    "Tenants inactivos",
                    "Hay " + inactiveTenants + " tenants inactivos que requieren revisión.",
                    "Revisar tenants",
                    generatedAt
            ));
        }
        if (expiringSoonSubscriptions > 0) {
            alerts.add(new SuperadminDashboardResponse.AlertItem(
                    "SUBSCRIPTIONS_EXPIRING_SOON",
                    "MEDIUM",
                    "Suscripciones próximas a vencer",
                    "Hay " + expiringSoonSubscriptions + " suscripciones que vencen en los próximos días.",
                    "Revisar suscripciones",
                    generatedAt
            ));
        }
        if (expiredSubscriptions > 0) {
            alerts.add(new SuperadminDashboardResponse.AlertItem(
                    "EXPIRED_SUBSCRIPTIONS",
                    "HIGH",
                    "Suscripciones vencidas",
                    "Se detectaron " + expiredSubscriptions + " suscripciones vencidas.",
                    "Revisar suscripciones vencidas",
                    generatedAt
            ));
        }
        if (failedAuditEvents > 0) {
            alerts.add(new SuperadminDashboardResponse.AlertItem(
                    "FAILED_AUDIT_EVENTS",
                    "HIGH",
                    "Eventos de auditoría fallidos",
                    "Hay " + failedAuditEvents + " eventos fallidos en auditoría.",
                    "Revisar auditoría",
                    generatedAt
            ));
        }
        if (activeTenants < totalTenants) {
            alerts.add(new SuperadminDashboardResponse.AlertItem(
                    "TENANT_ACTIVATION_GAP",
                    "INFO",
                    "Tenants no activos",
                    "No todos los tenants están activos actualmente.",
                    "Ver estado de tenants",
                    generatedAt
            ));
        }
        return alerts.stream().limit(MAX_ALERT_ITEMS).toList();
    }

    private List<SuperadminDashboardResponse.InsightItem> buildInsights(
            long activeTenants,
            long inactiveTenants,
            long activeSubscriptions,
            long trialSubscriptions,
            long activePlans,
            long totalPlans
    ) {
        List<SuperadminDashboardResponse.InsightItem> insights = new java.util.ArrayList<>();
        if (activeTenants > 0) {
            insights.add(new SuperadminDashboardResponse.InsightItem(
                    "ACTIVE_TENANT_BASE",
                    "INFO",
                    "Base activa",
                    "El sistema mantiene " + activeTenants + " tenants activos."
            ));
        }
        if (inactiveTenants > 0) {
            insights.add(new SuperadminDashboardResponse.InsightItem(
                    "TENANT_INACTIVITY",
                    "WARN",
                    "Tenants inactivos",
                    "Hay " + inactiveTenants + " tenants inactivos que podrían requerir acción."
            ));
        }
        if (trialSubscriptions > 0) {
            insights.add(new SuperadminDashboardResponse.InsightItem(
                    "TRIAL_BASE",
                    "INFO",
                    "Suscripciones de prueba",
                    "Existen " + trialSubscriptions + " suscripciones en trial."
            ));
        }
        if (activePlans == totalPlans) {
            insights.add(new SuperadminDashboardResponse.InsightItem(
                    "PLANS_HEALTHY",
                    "INFO",
                    "Catálogo estable",
                    "Todos los planes registrados están activos."
            ));
        }
        if (activeSubscriptions > 0) {
            insights.add(new SuperadminDashboardResponse.InsightItem(
                    "ACTIVE_SUBSCRIPTION_BASE",
                    "INFO",
                    "Base suscrita",
                    "Hay " + activeSubscriptions + " suscripciones activas en el sistema."
            ));
        }
        return insights;
    }

    private SuperadminDashboardResponse.TenantItem toTenantItem(PlatformTenant tenant) {
        PlatformPlan plan = tenant.planId() != null ? platformPlanRepository.findById(tenant.planId()).orElse(null) : null;
        return new SuperadminDashboardResponse.TenantItem(
                tenant.id(),
                tenant.name(),
                tenant.slug(),
                tenant.schemaName(),
                safeEnumName(tenant.status()),
                plan != null ? plan.code() : null,
                toDashboardTime(tenant.createdAt()),
                toDashboardTime(tenant.updatedAt())
        );
    }

    private SuperadminDashboardResponse.SubscriptionItem toSubscriptionItem(PlatformSubscription subscription) {
        PlatformTenant tenant = platformTenantRepository.findById(subscription.tenantId()).orElse(null);
        PlatformPlan plan = subscription.planId() != null ? platformPlanRepository.findById(subscription.planId()).orElse(null) : null;
        return new SuperadminDashboardResponse.SubscriptionItem(
                subscription.id(),
                subscription.tenantId(),
                tenant != null ? tenant.name() : null,
                tenant != null ? tenant.slug() : null,
                plan != null ? plan.code() : null,
                safeEnumName(subscription.status()),
                toDashboardTime(subscription.expiresAt()),
                subscription.status() == PlatformSubscriptionStatus.EXPIRED ? toDashboardTime(subscription.updatedAt()) : null
        );
    }

    private SuperadminDashboardResponse.AuditItem toAuditItem(PlatformAuditEvent event) {
        return new SuperadminDashboardResponse.AuditItem(
                event.id(),
                safeText(event.actorEmail()),
                safeText(event.eventType()),
                safeText(event.resourceType()),
                safeText(event.resourceId()),
                safeText(event.outcome()),
                toDashboardTime(event.createdAt())
        );
    }

    private SuperadminDashboardResponse.ActivityItem toActivityItem(PlatformAuditEvent event) {
        String actorName = safeText(event.actorEmail());
        if (actorName == null) {
            actorName = safeText(event.actorSubject());
        }
        return new SuperadminDashboardResponse.ActivityItem(
                event.id().toString(),
                safeText(event.resourceType()),
                safeText(event.eventType()),
                safeText(event.eventDetails()),
                actorName,
                toDashboardTime(event.createdAt())
        );
    }

    private boolean isInWindow(OffsetDateTime value, PeriodWindow window) {
        return value != null && !value.isBefore(window.from()) && !value.isAfter(window.to());
    }

    private boolean isInWindow(java.time.Instant instant, PeriodWindow window) {
        return instant != null && isInWindow(instant.atZone(DASHBOARD_ZONE).toOffsetDateTime(), window);
    }

    private PeriodWindow currentMonthWindow(OffsetDateTime now) {
        OffsetDateTime from = now.toLocalDate()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay()
                .atZone(DASHBOARD_ZONE)
                .toOffsetDateTime();
        return new PeriodWindow(from, now);
    }

    private PeriodWindow previousMonthWindow(OffsetDateTime now) {
        LocalDate previousMonth = now.toLocalDate().with(TemporalAdjusters.firstDayOfMonth()).minusMonths(1);
        OffsetDateTime from = previousMonth.atStartOfDay().atZone(DASHBOARD_ZONE).toOffsetDateTime();
        OffsetDateTime to = previousMonth.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59).atZone(DASHBOARD_ZONE).toOffsetDateTime();
        return new PeriodWindow(from, to);
    }

    private OffsetDateTime toDashboardTime(java.time.Instant instant) {
        return instant != null ? instant.atZone(DASHBOARD_ZONE).toOffsetDateTime() : null;
    }

    private BigDecimal percentChange(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return BigDecimal.valueOf((current - previous) * 100.0 / previous).setScale(2, RoundingMode.HALF_UP);
    }

    private String safeEnumName(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record PeriodWindow(OffsetDateTime from, OffsetDateTime to) {
    }

    private record PeriodRangeSummary(
            long previousTenants,
            long previousActiveSubscriptions,
            long previousNewTenants,
            long previousExpiredSubscriptions
    ) {
    }
}
