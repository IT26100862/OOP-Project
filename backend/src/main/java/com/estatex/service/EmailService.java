package com.estatex.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service to handle transactional emails (password reset, notifications).
 * Sends rich HTML formatted emails with plain-text fallback.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public EmailService() {
    }

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendPasswordResetEmail(String toEmail, String resetToken, String resetLink) {
        return sendPasswordResetEmail(toEmail, null, resetToken, resetLink);
    }

    public boolean sendPasswordResetEmail(String toEmail, String username, String resetToken, String resetLink) {
        String subject = "EstateX - Reset Your Password";
        String userGreeting = (username != null && !username.isBlank()) ? "Hello " + username + "," : "Hello,";
        String userContext = (username != null && !username.isBlank()) 
                ? "You have requested to reset your password for EstateX username: <strong>" + username + "</strong>."
                : "You have requested to reset your password for your EstateX account.";

        String plainContent = userGreeting + "\n\n"
                + "You have requested to reset your password for your EstateX account" + (username != null ? " (" + username + ")" : "") + ".\n\n"
                + "Your Password Reset Token: " + resetToken + "\n"
                + "Reset Link: " + resetLink + "\n\n"
                + "This link and token are valid for 30 minutes.\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Regards,\nEstateX Support Team";

        String htmlContent = "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='margin:0; padding:24px; font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Helvetica,Arial,sans-serif; background-color:#f1f5f9; color:#1e293b;'>"
                + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width:540px; background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.08); margin:20px auto; border:1px solid #e2e8f0;'>"
                + "  <tr>"
                + "    <td style='background-color:#0E1627; padding:28px 32px; text-align:center;'>"
                + "      <h1 style='margin:0; font-size:24px; color:#ffffff; font-weight:700; letter-spacing:0.5px;'>EstateX</h1>"
                + "      <p style='margin:6px 0 0 0; font-size:13px; color:#94a3b8;'>Real Estate Property Management</p>"
                + "    </td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding:36px 32px;'>"
                + "      <h2 style='margin:0 0 16px 0; font-size:20px; color:#0f172a; font-weight:700;'>Reset Your Password</h2>"
                + "      <p style='margin:0 0 12px 0; font-size:15px; line-height:1.6; color:#475569;'>" + userGreeting + "</p>"
                + "      <p style='margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#475569;'>" + userContext + " Click the button below to set your new password:</p>"
                + "      <div style='text-align:center; margin:32px 0;'>"
                + "        <a href='" + resetLink + "' target='_blank' style='display:inline-block; background-color:#7c5237; color:#ffffff; padding:14px 32px; font-size:15px; font-weight:600; text-decoration:none; border-radius:8px; box-shadow:0 2px 6px rgba(124,82,55,0.3);'>Reset Password</a>"
                + "      </div>"
                + "      <div style='background-color:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:16px 20px; margin:24px 0; text-align:center;'>"
                + "        <span style='display:block; font-size:12px; font-weight:600; color:#64748b; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:4px;'>Password Reset Token</span>"
                + "        <span style='font-size:20px; font-family:monospace; font-weight:700; color:#0f172a; letter-spacing:2px;'>" + resetToken + "</span>"
                + "      </div>"
                + "      <p style='margin:24px 0 8px 0; font-size:13px; color:#64748b;'>If the button above does not work, copy and paste this link into your browser:</p>"
                + "      <p style='margin:0 0 24px 0; font-size:13px; line-height:1.4;'><a href='" + resetLink + "' target='_blank' style='color:#2563eb; word-break:break-all; text-decoration:underline;'>" + resetLink + "</a></p>"
                + "      <p style='margin:0 0 16px 0; font-size:13px; color:#64748b; line-height:1.5;'><em>This link and token are valid for 30 minutes. If you did not request a password reset, you can safely ignore this email.</em></p>"
                + "      <div style='border-top:1px solid #e2e8f0; padding-top:20px; margin-top:28px;'>"
                + "        <p style='margin:0; font-size:14px; color:#475569;'>Regards,<br><strong>EstateX Support Team</strong></p>"
                + "      </div>"
                + "    </td>"
                + "  </tr>"
                + "</table>"
                + "</body>"
                + "</html>";

        log.info("=================================================================");
        log.info("[PASSWORD RESET EMAIL DISPATCHED]");
        log.info("To: {}", toEmail);
        log.info("Token: {}", resetToken);
        log.info("Link: {}", resetLink);
        log.info("=================================================================");

        if (mailSender != null) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(plainContent, htmlContent);
                mailSender.send(mimeMessage);
                log.info("HTML password reset email successfully sent via SMTP to {}", toEmail);
                return true;
            } catch (Exception e) {
                log.warn("MIME email failed, attempting plain text fallback: {}", e.getMessage());
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(toEmail);
                    message.setSubject(subject);
                    message.setText(plainContent);
                    mailSender.send(message);
                    log.info("Plain text fallback email successfully sent via SMTP to {}", toEmail);
                    return true;
                } catch (Exception ex) {
                    log.error("SMTP send failed completely: {}", ex.getMessage());
                    return false;
                }
            }
        } else {
            log.info("No external SMTP server configured. Simulated email logged successfully.");
            return false;
        }
    }
}
