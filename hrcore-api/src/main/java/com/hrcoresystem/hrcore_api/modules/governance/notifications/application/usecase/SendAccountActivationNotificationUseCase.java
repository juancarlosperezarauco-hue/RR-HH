package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.usecase;

import com.hrcoresystem.hrcore_api.common.mail.MailDeliveryService;
import com.hrcoresystem.hrcore_api.common.mail.AppMailProperties;
import com.hrcoresystem.hrcore_api.common.mail.MailMessage;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.config.AccountActivationNotificationProperties;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.AccountActivationNotification;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class SendAccountActivationNotificationUseCase {

    private final MailDeliveryService mailDeliveryService;
    private final AccountActivationNotificationProperties properties;
    private final AppMailProperties appMailProperties;
    public SendAccountActivationNotificationUseCase(
            MailDeliveryService mailDeliveryService,
            AccountActivationNotificationProperties properties,
            AppMailProperties appMailProperties
    ) {
        this.mailDeliveryService = mailDeliveryService;
        this.properties = properties;
        this.appMailProperties = appMailProperties;
    }

    public void execute(AccountActivationNotification notification) {
        String activationLink = buildActivationLink(notification);
        String expiresAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(notification.expiresAt().atOffset(ZoneOffset.UTC));

        String subject = "Activa tu acceso a HR Core";
        String body = buildHtmlBody(
                notification.tenantSlug(),
                notification.activationToken(),
                activationLink,
                expiresAt,
                "Activar cuenta",
                "Tu acceso a la organización fue creado y solo falta confirmar tu correo electrónico.",
                "Si no esperabas este correo, puedes ignorarlo."
        );

        mailDeliveryService.send(new MailMessage(
                notification.email(),
                subject,
                body,
                true
        ));
    }

    private String buildActivationLink(AccountActivationNotification notification) {
        String token = URLEncoder.encode(notification.activationToken(), StandardCharsets.UTF_8);
        String tenant = URLEncoder.encode(notification.tenantSlug(), StandardCharsets.UTF_8);

        return joinBaseUrl(properties.getFrontendUrlBase(), properties.getActivationUrlPath())
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
        String logoUrl = joinBaseUrl(appMailProperties.getPublicBaseUrl(), "/assets/con-fondo/logo.png");
        String logoMarkUrl = joinBaseUrl(appMailProperties.getPublicBaseUrl(), "/assets/logo.png");
        return """
                <!doctype html>
                <html lang="es">
                  <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  </head>
                  <body style="margin:0;background:#f5fbff;font-family:Arial,Helvetica,sans-serif;color:#0b294d;">
                    <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                      <div style="background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 12px 40px rgba(11,59,130,.14);border:1px solid #c9e7f7;">
                        <div style="padding:32px 32px 24px;text-align:center;background:linear-gradient(135deg,#0b3b82,#1268c4);">
                          <img src="%s" alt="HR Core" style="width:104px;height:104px;object-fit:contain;border-radius:18px;margin:0 auto 16px;display:block;" />
                          <div style="display:inline-block;background:#e7f7fd;color:#0b3b82;padding:6px 12px;border-radius:999px;font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;">
                            HR Core · Recursos Humanos
                          </div>
                        </div>
                        <div style="padding:32px;">
                          <p style="margin:0 0 16px;font-size:16px;line-height:1.6;">Hola,</p>
                          <p style="margin:0 0 20px;font-size:16px;line-height:1.6;">%s</p>
                          <div style="background:#f5fbff;border:1px solid #c9e7f7;border-radius:16px;padding:20px;margin:24px 0;">
                            <p style="margin:0 0 10px;font-size:14px;color:#294967;"><strong>Tenant:</strong> %s</p>
                            <p style="margin:0 0 10px;font-size:14px;color:#294967;"><strong>Token:</strong> %s</p>
                            <p style="margin:0;font-size:14px;color:#294967;"><strong>Expira:</strong> %s UTC</p>
                          </div>
                          <div style="text-align:center;margin:30px 0;">
                            <a href="%s" style="display:inline-block;background:#1268c4;color:#ffffff;text-decoration:none;font-weight:700;padding:14px 24px;border-radius:12px;">
                              %s
                            </a>
                          </div>
                          <p style="margin:0 0 10px;font-size:14px;line-height:1.6;color:#294967;">Si el botón no funciona, copia y pega este enlace:</p>
                          <p style="margin:0 0 24px;font-size:13px;line-height:1.6;word-break:break-all;"><a href="%s" style="color:#0d56a6;">%s</a></p>
                          <p style="margin:0;font-size:14px;line-height:1.6;color:#6d8498;">%s</p>
                          <div style="margin-top:28px;padding-top:20px;border-top:1px solid #d9edf8;text-align:center;">
                            <img src="%s" alt="HR Core" style="width:42px;height:42px;object-fit:contain;vertical-align:middle;margin-right:8px;" />
                            <span style="font-size:13px;font-weight:700;color:#0b3b82;vertical-align:middle;">HR Core · Recursos Humanos</span>
                          </div>
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
                footerText,
                logoMarkUrl
        );
    }
}
