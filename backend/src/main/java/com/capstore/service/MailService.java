package com.capstore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/** Sends email asynchronously and logs delivery failures. */
@Slf4j
@Service
public class MailService {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final TemplateEngine templates;

    public MailService(ObjectProvider<JavaMailSender> sender, @Value("${app.mail-from}") String from,
                       TemplateEngine templates) {
        this.sender = sender;
        this.from = from;
        this.templates = templates;
    }

    @Async
    public void send(String to, String subject, String body) {
        sendNow(to, subject, body);
    }

    public boolean sendNow(String to, String subject, String body) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.info("[EMAIL not configured] To: {} | Subject: {}\n{}", to, subject, body);
            return true;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            s.send(m);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean sendContactInquiry(String to, String replyTo, String body) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.error("Cannot send contact inquiry because email is not configured");
            return false;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setReplyTo(replyTo);
            m.setSubject("New website contact inquiry");
            m.setText(body);
            s.send(m);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send website contact inquiry: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendHtml(String to, String subject, String template, Map<String, Object> model) {
        try {
            JavaMailSender s = sender.getIfAvailable();
            Context context = new Context();
            context.setVariables(model);
            String body = templates.process(template, context);
            if (s == null) {
                log.info("[HTML EMAIL not configured] To: {} | Subject: {}\n{}", to, subject, body);
                return true;
            }
            MimeMessage message = s.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            s.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Failed to render or send HTML email to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }
}
