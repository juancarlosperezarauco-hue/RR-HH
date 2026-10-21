package com.hrcoresystem.hrcore_api.modules.platform.enterprise.application.service;

import com.hrcoresystem.hrcore_api.common.mail.AppMailProperties;
import com.hrcoresystem.hrcore_api.common.mail.MailDeliveryService;
import com.hrcoresystem.hrcore_api.common.mail.MailMessage;
import com.hrcoresystem.hrcore_api.modules.platform.enterprise.application.dto.EnterpriseInquiryRequest;
import org.springframework.stereotype.Service;

@Service
public class EnterpriseInquiryService {

  private final MailDeliveryService mailDeliveryService;
  private final AppMailProperties mailProperties;

  public EnterpriseInquiryService(
      MailDeliveryService mailDeliveryService,
      AppMailProperties mailProperties) {
    this.mailDeliveryService = mailDeliveryService;
    this.mailProperties = mailProperties;
  }

  public void send(EnterpriseInquiryRequest request) {
    String fullName = escapeHtml(request.fullName());
    String email = escapeHtml(request.email());
    String company = escapeHtml(request.company());
    String teamSize = escapeHtml(request.teamSize());
    String message = escapeHtml(request.message());

    String body = """
        <!doctype html>
        <html lang="es">
          <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
          <body style="margin:0;background:#f5fbff;font-family:Arial,Helvetica,sans-serif;color:#0b294d;">
            <div style="max-width:640px;margin:0 auto;padding:28px 16px;">
              <div style="overflow:hidden;background:#fff;border:1px solid #c9e7f7;border-radius:20px;box-shadow:0 12px 36px rgba(11,59,130,.12);">
                <div style="padding:28px 32px;background:linear-gradient(135deg,#0b3b82,#1268c4);color:#fff;">
                  <p style="margin:0 0 8px;font-size:12px;font-weight:700;letter-spacing:.14em;text-transform:uppercase;color:#d9f4ff;">HR Core · Recursos Humanos</p>
                  <h1 style="margin:0;font-size:25px;line-height:1.25;">Nueva solicitud Enterprise</h1>
                  <p style="margin:10px 0 0;font-size:14px;line-height:1.5;color:#e7f7fd;">Una organización ha solicitado información sobre una propuesta personalizada.</p>
                </div>
                <div style="padding:30px 32px;">
                  <h2 style="margin:0 0 18px;font-size:17px;color:#0b3b82;">Datos de contacto</h2>
                  <table role="presentation" style="width:100%%;border-collapse:collapse;font-size:14px;">
                    <tr><td style="padding:10px 0;border-bottom:1px solid #e5f1f7;color:#6d8498;width:42%%;">Nombre</td><td style="padding:10px 0;border-bottom:1px solid #e5f1f7;font-weight:700;">%s</td></tr>
                    <tr><td style="padding:10px 0;border-bottom:1px solid #e5f1f7;color:#6d8498;">Correo</td><td style="padding:10px 0;border-bottom:1px solid #e5f1f7;font-weight:700;"><a href="mailto:%s" style="color:#1268c4;text-decoration:none;">%s</a></td></tr>
                    <tr><td style="padding:10px 0;border-bottom:1px solid #e5f1f7;color:#6d8498;">Empresa</td><td style="padding:10px 0;border-bottom:1px solid #e5f1f7;font-weight:700;">%s</td></tr>
                    <tr><td style="padding:10px 0;color:#6d8498;">Tamaño del equipo</td><td style="padding:10px 0;font-weight:700;">%s</td></tr>
                  </table>
                  <h2 style="margin:28px 0 12px;font-size:17px;color:#0b3b82;">Necesidades compartidas</h2>
                  <div style="padding:18px 20px;background:#f5fbff;border:1px solid #c9e7f7;border-radius:14px;font-size:14px;line-height:1.7;white-space:pre-wrap;">%s</div>
                  <p style="margin:24px 0 0;padding-top:18px;border-top:1px solid #d9edf8;font-size:13px;line-height:1.6;color:#6d8498;">Para continuar la conversación, utiliza el correo indicado en los datos de contacto.</p>
                </div>
                <div style="padding:18px 32px;background:#f5fbff;text-align:center;font-size:12px;color:#6d8498;">Solicitud recibida desde HR Core</div>
              </div>
            </div>
          </body>
        </html>
        """.formatted(fullName, email, email, company, teamSize, message);

    mailDeliveryService.send(new MailMessage(
        mailProperties.getFrom(),
        "Solicitud de propuesta Enterprise — " + request.company().trim(),
        body,
        true));
  }

  private String escapeHtml(String value) {
    return value == null ? "" : value.trim()
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
