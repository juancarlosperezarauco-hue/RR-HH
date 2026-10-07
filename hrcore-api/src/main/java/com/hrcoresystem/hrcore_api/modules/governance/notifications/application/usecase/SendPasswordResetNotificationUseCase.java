package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.usecase;

import com.hrcoresystem.hrcore_api.common.mail.MailDeliveryService;
import com.hrcoresystem.hrcore_api.common.mail.AppMailProperties;
import com.hrcoresystem.hrcore_api.common.mail.MailMessage;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.PasswordResetNotificationProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.PasswordResetNotification;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class SendPasswordResetNotificationUseCase {

    private final MailDeliveryService mailDeliveryService;
    private final PasswordResetNotificationProperties properties;
    private final AppMailProperties appMailProperties;

    public SendPasswordResetNotificationUseCase(
            MailDeliveryService mailDeliveryService,
            PasswordResetNotificationProperties properties,
            AppMailProperties appMailProperties
    ) {
        this.mailDeliveryService = mailDeliveryService;
        this.properties = properties;
        this.appMailProperties = appMailProperties;
    }

    public void execute(PasswordResetNotification notification) {
        String resetLink = buildResetLink(notification);
        String expiresAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(notification.expiresAt().atOffset(ZoneOffset.UTC));

        String subject = "Recupera tu acceso a HR Core";
        String body = buildHtmlBody(
                notification.tenantSlug(),
                notification.resetToken(),
                resetLink,
                expiresAt,
                "Restablecer contraseña",
                "Recibimos una solicitud para restablecer la contraseña de tu acceso a la organización.",
                "Si no solicitaste este cambio, puedes ignorarlo."
        );

        mailDeliveryService.send(new MailMessage(
                notification.email(),
                subject,
                body,
                true
        ));
    }

    private String buildResetLink(PasswordResetNotification notification) {
        String token = URLEncoder.encode(notification.resetToken(), StandardCharsets.UTF_8);
        String tenant = URLEncoder.encode(notification.tenantSlug(), StandardCharsets.UTF_8);

        return joinBaseUrl(properties.getFrontendUrlBase(), properties.getResetUrlPath())
                + "?token=" + token + "&tenant=" + tenant;
    }

    private String joinBaseUrl(String baseUrl, String path) {
        String normalizedBase = trimTrailingSlash(baseUrl);
        String normalizedPath = path == null ? "" : path.strip();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return normalizedBase + normalizedPath;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.strip();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String buildHtmlBody(
            String tenantSlug,
            String token,
            String link,
            String expiresAt,
            String ctaText,
            String intro,
            String footerText
    ) {
        String logoUrl = joinBaseUrl(appMailProperties.getPublicBaseUrl(), "/assets/logo.png");
        return """
                <!doctype html>
                <html lang="es">
                  <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  </head>
                  <body style="margin:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#123;">
                    <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                      <div style="background:#fff;border-radius:20px;overflow:hidden;box-shadow:0 12px 40px rgba(16,24,40,.12);border:1px solid #e5e7eb;">
                        <div style="padding:32px 32px 24px;text-align:center;background:linear-gradient(135deg,#e8f5e9,#f8fff8);">
                          <img src="%s" alt="HR Core" style="width:96px;height:96px;object-fit:contain;margin:0 auto 16px;display:block;" />
                          <div style="display:inline-block;background:#dcfce7;color:#166534;padding:6px 12px;border-radius:999px;font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;">
                            HR Core · Recursos Humanos
                          </div>
                        </div>
                        <div style="padding:32px;">
                          <p style="margin:0 0 16px;font-size:16px;line-height:1.6;">Hola,</p>
                          <p style="margin:0 0 20px;font-size:16px;line-height:1.6;">%s</p>
                          <div style="background:#f8fafc;border:1px solid #e5e7eb;border-radius:16px;padding:20px;margin:24px 0;">
                            <p style="margin:0 0 10px;font-size:14px;color:#475569;"><strong>Tenant:</strong> %s</p>
                            <p style="margin:0 0 10px;font-size:14px;color:#475569;"><strong>Token:</strong> %s</p>
                            <p style="margin:0;font-size:14px;color:#475569;"><strong>Expira:</strong> %s UTC</p>
                          </div>
                          <div style="text-align:center;margin:30px 0;">
                            <a href="%s" style="display:inline-block;background:#16a34a;color:#fff;text-decoration:none;font-weight:700;padding:14px 24px;border-radius:12px;">
                              %s
                            </a>
                          </div>
                          <p style="margin:0 0 10px;font-size:14px;line-height:1.6;color:#475569;">Si el botón no funciona, copia y pega este enlace:</p>
                          <p style="margin:0 0 24px;font-size:13px;line-height:1.6;word-break:break-all;"><a href="%s" style="color:#2563eb;">%s</a></p>
                          <p style="margin:0;font-size:14px;line-height:1.6;color:#64748b;">%s</p>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                logoUrl,
                intro,
                tenantSlug,
                token,
                expiresAt,
                link,
                ctaText,
                link,
                link,
                footerText
        );
    }
}
