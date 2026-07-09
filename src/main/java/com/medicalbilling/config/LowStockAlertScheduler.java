package com.medicalbilling.config;

import com.medicalbilling.service.LowStockNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class LowStockAlertScheduler {

    private final LowStockNotificationService lowStockNotificationService;

    @Scheduled(cron = "${app.low-stock.digest-cron:0 0 9,17 * * *}")
    public void sendLowStockDigest() {
        try {
            int count = lowStockNotificationService.sendDailyLowStockDigest();
            if (count > 0) {
                log.info("Scheduled low stock digest completed for {} medicines", count);
            }
        } catch (Exception e) {
            log.error("Scheduled low stock digest failed: {}", e.getMessage());
        }
    }
}
