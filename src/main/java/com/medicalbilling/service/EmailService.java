package com.medicalbilling.service;

import com.medicalbilling.entity.Sale;
import com.medicalbilling.entity.ShopSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SettingsService settingsService;

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
