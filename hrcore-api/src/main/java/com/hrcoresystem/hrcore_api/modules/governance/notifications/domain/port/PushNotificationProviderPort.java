package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.PushNotificationMessage;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.PushNotificationResult;

public interface PushNotificationProviderPort {

    PushNotificationResult send(PushNotificationMessage message);
}
