package com.hrcoresystem.hrcore_api.common.mail;

public record MailMessage(
        String to,
        String subject,
        String body,
        boolean html
) {
}
