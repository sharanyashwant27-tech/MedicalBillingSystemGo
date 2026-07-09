package com.medicalbilling.service;

import com.medicalbilling.entity.NotificationLog;
import com.medicalbilling.entity.NotificationStatus;
import com.medicalbilling.entity.NotificationType;
import com.medicalbilling.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.api-url:}")
    private String smsApiUrl;

    @Value("${app.sms.api-key:}")
    private String smsApiKey;

    @Value("${app.sms.sender-id:MEDBILL}")
    private String senderId;

    public NotificationLog sendSms(String phone, String message) {
        return sendSms(phone, message, null);
    }

    public NotificationLog sendSms(String phone, String message, String referenceId) {
        NotificationLog logEntry = NotificationLog.builder()
                .type(NotificationType.SMS)
                .recipient(phone)
                .message(message)
                .referenceId(referenceId)
                .build();

        if (!smsEnabled || phone == null || phone.isBlank()) {
            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setErrorMessage("SMS simulated (provider not configured)");
            log.info("SMS to {}: {}", phone, message);
        } else {
            try {
                sendViaProvider(phone, message);
                logEntry.setStatus(NotificationStatus.SENT);
            } catch (Exception e) {
                logEntry.setStatus(NotificationStatus.FAILED);
                logEntry.setErrorMessage(e.getMessage());
                log.error("SMS failed to {}: {}", phone, e.getMessage());
            }
        }
        return notificationLogRepository.save(logEntry);
    }

    public NotificationLog sendLowStockAlert(String phone, String message, String referenceId) {
        return sendSms(phone, message, referenceId);
    }

    public void sendBillNotification(String phone, String billNumber, String amount) {
        sendSms(phone, "Thank you for your purchase! Bill: " + billNumber + ", Amount: Rs." + amount);
    }

    private void sendViaProvider(String phone, String message) {
        log.info("Sending SMS via {} to {}: {}", smsApiUrl, phone, message);
    }
}
