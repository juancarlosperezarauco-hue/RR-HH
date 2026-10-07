package com.hrcoresystem.hrcore_api.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailDeliveryService implements MailDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpMailDeliveryService.class);

    private final JavaMailSender javaMailSender;
    private final AppMailProperties appMailProperties;

    public SmtpMailDeliveryService(
            JavaMailSender javaMailSender,
            AppMailProperties appMailProperties
    ) {
        this.javaMailSender = javaMailSender;
        this.appMailProperties = appMailProperties;
    }

    @Override
    public void send(MailMessage message) {
        logger.info("Sending email notification to '{}'.", message.to());
        if (message.html()) {
            sendHtml(message);
        } else {
            sendText(message);
        }

        logger.info("Email notification sent successfully to '{}'.", message.to());
    }

    private void sendHtml(MailMessage message) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(appMailProperties.getFrom());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body(), true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Unable to build HTML mail message", exception);
        }
    }

    private void sendText(MailMessage message) {
        org.springframework.mail.SimpleMailMessage simpleMailMessage = new org.springframework.mail.SimpleMailMessage();
        simpleMailMessage.setFrom(appMailProperties.getFrom());
        simpleMailMessage.setTo(message.to());
        simpleMailMessage.setSubject(message.subject());
        simpleMailMessage.setText(message.body());
        javaMailSender.send(simpleMailMessage);
    }
}
