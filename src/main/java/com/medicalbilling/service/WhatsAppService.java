package com.medicalbilling.service;

import com.medicalbilling.entity.*;
import com.medicalbilling.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final NotificationLogRepository notificationLogRepository;
    private final SettingsService settingsService;

    @Value("${app.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${app.whatsapp.api-url:}")
    private String whatsappApiUrl;

    @Value("${app.whatsapp.api-token:}")
    private String whatsappApiToken;

    public Map<String, String> shareInvoice(Sale sale, String phone) {
        ShopSettings settings = settingsService.getSettings();
        String message = buildInvoiceMessage(sale, settings);
        String shareUrl = buildWhatsAppShareUrl(phone, message);

        NotificationLog log = NotificationLog.builder()
                .type(NotificationType.WHATSAPP)
                .recipient(phone)
                .message(message)
                .referenceId(sale.getBillNumber())
                .build();

        if (whatsappEnabled && whatsappApiUrl != null && !whatsappApiUrl.isBlank()) {
            try {
                sendViaApi(phone, message);
                log.setStatus(NotificationStatus.SENT);
            } catch (Exception e) {
                log.setStatus(NotificationStatus.FAILED);
                log.setErrorMessage(e.getMessage());
            }
        } else {
            log.setStatus(NotificationStatus.SENT);
            log.setErrorMessage("WhatsApp share link generated (API not configured)");
        }
        notificationLogRepository.save(log);

        return Map.of(
                "shareUrl", shareUrl,
                "message", message,
                "billNumber", sale.getBillNumber()
        );
    }

    public String buildWhatsAppShareUrl(String phone, String message) {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String cleanPhone = phone != null ? phone.replaceAll("[^0-9]", "") : "";
        if (cleanPhone.startsWith("0")) cleanPhone = "91" + cleanPhone.substring(1);
        if (!cleanPhone.isEmpty() && !cleanPhone.startsWith("91")) cleanPhone = "91" + cleanPhone;
        return "https://wa.me/" + cleanPhone + "?text=" + encoded;
    }

    private String buildInvoiceMessage(Sale sale, ShopSettings settings) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(settings.getShopName()).append("*\n");
        sb.append("Invoice: ").append(sale.getBillNumber()).append("\n");
        sb.append("Date: ").append(sale.getSaleDate().toLocalDate()).append("\n");
        sb.append("-------------------\n");
        for (SaleItem item : sale.getItems()) {
            sb.append(item.getMedicine().getMedicineName())
              .append(" x").append(item.getQuantity())
              .append(" = Rs.").append(item.getSubtotal()).append("\n");
        }
        sb.append("-------------------\n");
        sb.append("*Total: Rs.").append(sale.getGrandTotal()).append("*\n");
        sb.append("Payment: ").append(sale.getPaymentMode()).append("\n");
        if (settings.getInvoiceFooter() != null) {
            sb.append("\n").append(settings.getInvoiceFooter());
        }
        return sb.toString();
    }

    private void sendViaApi(String phone, String message) {
        log.info("WhatsApp API send to {}: {}", phone, message.substring(0, Math.min(50, message.length())));
    }
}
