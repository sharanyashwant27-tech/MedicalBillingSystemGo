package com.medicalbilling.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BackupServiceTest {

    @Autowired
    private BackupService backupService;

    @Test
    void createBackupGeneratesFile() throws IOException {
        String path = backupService.createBackup();
        assertNotNull(path);
        assertTrue(Files.exists(Path.of(path)));
    }

    @Test
    void listBackupsReturnsList() throws IOException {
        backupService.createBackup();
        var backups = backupService.listBackups();
        assertNotNull(backups);
    }
}
