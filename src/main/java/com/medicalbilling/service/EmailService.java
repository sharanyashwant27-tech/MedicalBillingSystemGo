package com.medicalbilling.service;

import com.medicalbilling.entity.NotificationLog;
import com.medicalbilling.entity.NotificationStatus;
import com.medicalbilling.entity.NotificationType;
import com.medicalbilling.entity.Sale;
import com.medicalbilling.entity.ShopSettings;
import com.medicalbilling.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SettingsService settingsService;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendBillEmail(Sale sale, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("No email address provided for bill {}", sale.getBillNumber());
            return;
        }
        try {
            ShopSettings settings = settingsService.getSettings();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("Invoice " + sale.getBillNumber() + " - " + settings.getShopName());
            message.setText(buildBillEmailBody(sale, settings));
            if (settings.getEmail() != null) {
                message.setFrom(settings.getEmail());
            }
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send bill email: {}", e.getMessage());
        }
    }

    public void sendLowStockAlertEmail(String recipientEmail, String message, String referenceId) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }

        NotificationLog logEntry = NotificationLog.builder()
                .type(NotificationType.EMAIL)
                .recipient(recipientEmail)
                .message(message)
                .referenceId(referenceId)
                .build();

        try {
            ShopSettings settings = settingsService.getSettings();
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(recipientEmail);
            mailMessage.setSubject("MediBill Low Stock Alert - " + settings.getShopName());
            mailMessage.setText(message);
            if (settings.getEmail() != null && !settings.getEmail().isBlank()) {
                mailMessage.setFrom(settings.getEmail());
            } else if (mailUsername != null && !mailUsername.isBlank()) {
                mailMessage.setFrom(mailUsername);
            }
            mailSender.send(mailMessage);
            logEntry.setStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            log.warn("Low stock email simulated/failed for {}: {}", recipientEmail, e.getMessage());
        }
        notificationLogRepository.save(logEntry);
    }

    private String buildBillEmailBody(Sale sale, ShopSettings settings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear Customer,\n\n");
        sb.append("Thank you for your purchase at ").append(settings.getShopName()).append(".\n\n");
        sb.append("Bill Number: ").append(sale.getBillNumber()).append("\n");
        sb.append("Date: ").append(sale.getSaleDate()).append("\n");
        sb.append("Grand Total: Rs. ").append(sale.getGrandTotal()).append("\n");
        sb.append("Payment Mode: ").append(sale.getPaymentMode()).append("\n\n");
        sb.append(settings.getInvoiceFooter() != null ? settings.getInvoiceFooter() : "Thank you!");
        return sb.toString();
    }
}
