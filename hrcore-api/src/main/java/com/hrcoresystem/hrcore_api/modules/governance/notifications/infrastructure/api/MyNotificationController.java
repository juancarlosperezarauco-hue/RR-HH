package com.hrcoresystem.hrcore_api.modules.governance.notifications.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.mapper.NotificationMapper;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.service.NotificationApplicationService;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase.GetCurrentAuthenticatedTenantUserUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class MyNotificationController {

    private final NotificationApplicationService notificationApplicationService;
    private final NotificationMapper notificationMapper;
    private final GetCurrentAuthenticatedTenantUserUseCase getCurrentAuthenticatedTenantUserUseCase;

    public MyNotificationController(
            NotificationApplicationService notificationApplicationService,
            NotificationMapper notificationMapper,
            GetCurrentAuthenticatedTenantUserUseCase getCurrentAuthenticatedTenantUserUseCase
    ) {
        this.notificationApplicationService = notificationApplicationService;
        this.notificationMapper = notificationMapper;
        this.getCurrentAuthenticatedTenantUserUseCase = getCurrentAuthenticatedTenantUserUseCase;
    }

    @PostMapping("/notification-devices")
    public ApiResponse<NotificationDeviceResponse> registerNotificationDevice(
            @Valid @RequestBody RegisterNotificationDeviceRequest request
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification device registered successfully",
                notificationMapper.toResponse(notificationApplicationService.registerDevice(currentUserId, request))
        );
    }

    @GetMapping("/notification-devices")
    public ApiResponse<Page<NotificationDeviceResponse>> listNotificationDevices(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification devices retrieved successfully",
                PaginationSupport.page(
                        notificationApplicationService.listDevices(currentUserId).stream()
                                .map(notificationMapper::toResponse)
                                .toList(),
                        pageable
                )
        );
    }

    @PatchMapping("/notification-devices/{id}/deactivate")
    public ApiResponse<NotificationDeviceResponse> deactivateNotificationDevice(
            @PathVariable UUID id
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification device deactivated successfully",
                notificationMapper.toResponse(notificationApplicationService.deactivateDevice(currentUserId, id))
        );
    }

    @DeleteMapping("/notification-devices/{id}")
    public ApiResponse<NotificationDeviceResponse> revokeNotificationDevice(
            @PathVariable UUID id
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification device revoked successfully",
                notificationMapper.toResponse(notificationApplicationService.revokeDevice(currentUserId, id))
        );
    }

    @GetMapping("/notifications")
    public ApiResponse<Page<NotificationResponse>> listNotifications(
            @ParameterObject @PageableDefault(size = 50) Pageable pageable
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notifications retrieved successfully",
                PaginationSupport.page(
                        notificationApplicationService.listNotifications(currentUserId).stream()
                                .map(notificationMapper::toResponse)
                                .toList(),
                        pageable
                )
        );
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> unreadCount() {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Unread notifications count retrieved successfully",
                new UnreadNotificationCountResponse(notificationApplicationService.getUnreadCount(currentUserId))
        );
    }

    @GetMapping("/notifications/{id}")
    public ApiResponse<NotificationResponse> getNotificationById(
            @PathVariable UUID id
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification retrieved successfully",
                notificationMapper.toResponse(notificationApplicationService.getNotification(currentUserId, id))
        );
    }

    @PatchMapping("/notifications/{id}/read")
    public ApiResponse<NotificationResponse> markNotificationAsRead(
            @PathVariable UUID id
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification marked as read successfully",
                notificationMapper.toResponse(notificationApplicationService.markAsRead(currentUserId, id))
        );
    }

    @PatchMapping("/notifications/{id}/open")
    public ApiResponse<NotificationResponse> openNotification(
            @PathVariable UUID id
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification opened successfully",
                notificationMapper.toResponse(notificationApplicationService.openNotification(currentUserId, id))
        );
    }

    @PatchMapping("/notifications/read-all")
    public ApiResponse<List<NotificationResponse>> markAllNotificationsAsRead() {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notifications marked as read successfully",
                notificationApplicationService.markAllAsRead(currentUserId).stream()
                        .map(notificationMapper::toResponse)
                        .toList()
        );
    }

    @PatchMapping("/notifications/{id}/archive")
    public ApiResponse<NotificationResponse> archiveNotification(
            @PathVariable UUID id
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification archived successfully",
                notificationMapper.toResponse(notificationApplicationService.archive(currentUserId, id))
        );
    }

    @GetMapping("/notification-preferences")
    public ApiResponse<Page<NotificationPreferenceResponse>> listNotificationPreferences(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification preferences retrieved successfully",
                PaginationSupport.page(
                        notificationApplicationService.listPreferences(currentUserId).stream()
                                .map(notificationMapper::toResponse)
                                .toList(),
                        pageable
                )
        );
    }

    @PutMapping("/notification-preferences")
    public ApiResponse<NotificationPreferenceResponse> upsertNotificationPreference(
            @Valid @RequestBody UpsertNotificationPreferenceRequest request
    ) {
        UUID currentUserId = getCurrentAuthenticatedTenantUserUseCase.execute().id();
        return ApiResponse.success(
                "Notification preference updated successfully",
                notificationMapper.toResponse(notificationApplicationService.upsertPreference(currentUserId, request))
        );
    }
}
