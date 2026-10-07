package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationDeliveryResponse;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationTemplateResponse;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.mapper.NotificationMapper;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.service.NotificationApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "bearerAuth")
// @PreAuthorize("hasAnyRole('OWNER_ADMIN', 'ADMIN')")
public class NotificationAdminController {

    private final NotificationApplicationService notificationApplicationService;
    private final NotificationMapper notificationMapper;

    public NotificationAdminController(
            NotificationApplicationService notificationApplicationService,
            NotificationMapper notificationMapper
    ) {
        this.notificationApplicationService = notificationApplicationService;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('notifications.templates.read')")
    public ApiResponse<Page<NotificationTemplateResponse>> listTemplates(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(
                "Notification templates retrieved successfully",
                PaginationSupport.page(
                        notificationApplicationService.listTemplates().stream()
                        .map(notificationMapper::toResponse)
                        .toList(),
                        pageable
                )
        );
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('notifications.templates.detail')")
    public ApiResponse<NotificationTemplateResponse> getTemplateById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Notification template retrieved successfully",
                notificationMapper.toResponse(notificationApplicationService.getTemplateById(id))
        );
    }

    @GetMapping("/{notificationId}/deliveries")
    @PreAuthorize("hasAuthority('notifications.deliveries.read')")
    public ApiResponse<Page<NotificationDeliveryResponse>> listDeliveriesByNotification(
            @PathVariable UUID notificationId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable
    ) {
        return ApiResponse.success(
                "Notification deliveries retrieved successfully",
                PaginationSupport.page(
                        notificationApplicationService.listDeliveriesByNotification(notificationId).stream()
                        .map(notificationMapper::toResponse)
                        .toList(),
                        pageable
                )
        );
    }
}
