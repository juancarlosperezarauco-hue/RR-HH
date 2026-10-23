package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.service;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.UUID;

@Service
public class TenantNotificationDispatcher {
    private final JdbcTemplate jdbc;
    private final NotificationPublisherPort notifications;
    private final TransactionTemplate notificationTransaction;

    public TenantNotificationDispatcher(@Qualifier("targetDataSource") DataSource dataSource,
                                        NotificationPublisherPort notifications,
                                        PlatformTransactionManager transactionManager) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.notifications = notifications;
        this.notificationTransaction = new TransactionTemplate(transactionManager);
        this.notificationTransaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    public void toOwner(PlatformTenant tenant, NotificationType type, String title, String body,
                        NotificationCategory category, NotificationPriority priority, String actionUrl) {
        if (tenant == null || tenant.schemaName() == null || !tenant.schemaName().matches("^[A-Za-z0-9_]+$")) return;
        TenantContext previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(new TenantContext(tenant.slug(), tenant.schemaName(), false));
            UUID owner = jdbc.query("SELECT owner_user_id FROM " + tenant.schemaName() + ".tenant_authorization_state WHERE id = 1",
                    rs -> rs.next() ? (UUID) rs.getObject(1) : null);
            if (owner == null) return;
            notificationTransaction.executeWithoutResult(status -> notifications.publish(
                    new NotificationPublishRequest(owner, type, category, priority, title, body,
                            null, null, actionUrl, null)
            ));
        } catch (Exception ignored) {
            // Platform lifecycle events must remain independent from tenant notification delivery.
        } finally {
            if (previous == null) TenantContextHolder.clear(); else TenantContextHolder.set(previous);
        }
    }
}
