package com.medicalbilling.config;

import com.medicalbilling.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class BackupScheduler {

    private final BackupService backupService;

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledBackup() {
        try {
            String path = backupService.createBackup();
            log.info("Scheduled database backup completed: {}", path);
        } catch (Exception e) {
            log.error("Scheduled backup failed: {}", e.getMessage());
        }
    }
}
