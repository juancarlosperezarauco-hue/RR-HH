package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.AccountActivationNotificationProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.usecase.SendAccountActivationNotificationUseCase;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.AccountActivationNotification;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.AccountActivationToken;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.AccountActivationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Issues a fresh account activation token for the given email and sends the
 * activation email. Assumes the caller has already verified that the email
 * belongs to a tenant user that still needs to confirm their address.
 */
@Service
public class IssueAccountActivationUseCase {

    private static final Logger log = LoggerFactory.getLogger(IssueAccountActivationUseCase.class);

    private final AccountActivationTokenRepository accountActivationTokenRepository;
    private final AccountActivationNotificationProperties properties;
    private final SendAccountActivationNotificationUseCase sendAccountActivationNotificationUseCase;
    private final SecureRandom secureRandom = new SecureRandom();

    public IssueAccountActivationUseCase(
            AccountActivationTokenRepository accountActivationTokenRepository,
            AccountActivationNotificationProperties properties,
            SendAccountActivationNotificationUseCase sendAccountActivationNotificationUseCase
    ) {
        this.accountActivationTokenRepository = accountActivationTokenRepository;
        this.properties = properties;
        this.sendAccountActivationNotificationUseCase = sendAccountActivationNotificationUseCase;
    }

    @Transactional
    public void execute(String email) {
        TenantContext tenantContext = TenantContextHolder.getRequired();
        String normalizedEmail = email.trim().toLowerCase();

        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getExpirationMinutes(), ChronoUnit.MINUTES);

        accountActivationTokenRepository.invalidateAllByEmail(normalizedEmail, now);

        String token = generateSecureToken();

        accountActivationTokenRepository.save(new AccountActivationToken(
                null,
                normalizedEmail,
                token,
                expiresAt,
                false,
                null,
                null
        ));

        try {
            sendAccountActivationNotificationUseCase.execute(
                    new AccountActivationNotification(
                            normalizedEmail,
                            tenantContext.tenantSlug(),
                            token,
                            expiresAt
                    )
            );
        } catch (Exception exception) {
            log.warn("Unable to send account activation email to '{}': {}", normalizedEmail, exception.getMessage());
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
