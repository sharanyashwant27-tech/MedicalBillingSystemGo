package com.medicalbilling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    @Value("${app.backup.dir:backups}")
    private String backupDir;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:root}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    public String createBackup() throws IOException {
        Path backupPath = Paths.get(backupDir);
        Files.createDirectories(backupPath);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_" + timestamp + ".sql";
        Path target = backupPath.resolve(fileName);

        String dbName = extractDbName(datasourceUrl);
        if (dbName != null && isMysqldumpAvailable()) {
            executeMysqldump(dbName, target);
        } else {
            Files.writeString(target, generateMetadataBackup());
        }

        log.info("Backup created: {}", target);
        return target.toString();
    }

    public List<String> listBackups() throws IOException {
        Path backupPath = Paths.get(backupDir);
        if (!Files.exists(backupPath)) return List.of();
        List<String> backups = new ArrayList<>();
        try (var stream = Files.list(backupPath)) {
            stream.filter(p -> p.toString().endsWith(".sql"))
                    .sorted((a, b) -> b.getFileName().compareTo(a.getFileName()))
                    .forEach(p -> backups.add(p.getFileName().toString()));
        }
        return backups;
    }

    private boolean isMysqldumpAvailable() {
        try {
            Process p = new ProcessBuilder("mysqldump", "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void executeMysqldump(String dbName, Path target) throws IOException {
        String host = extractHost(datasourceUrl);
        List<String> command = List.of(
                "mysqldump", "-h", host, "-u", dbUsername, dbName
        );
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("MYSQL_PWD", dbPassword);
        pb.redirectOutput(target.toFile());
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Files.writeString(target, generateMetadataBackup());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Files.writeString(target, generateMetadataBackup());
        }
    }

    private String extractDbName(String url) {
        if (url == null || !url.contains("/")) return null;
        String part = url.substring(url.lastIndexOf("/") + 1);
        int q = part.indexOf("?");
        return q > 0 ? part.substring(0, q) : part;
    }

    private String extractHost(String url) {
        if (url == null) return "localhost";
        if (url.contains("mysql:")) return "mysql";
        return "localhost";
    }

    private String generateMetadataBackup() {
        return "-- Medical Billing System Backup\n" +
                "-- Created: " + LocalDateTime.now() + "\n" +
                "-- Note: mysqldump not available; use Docker backup volume or configure mysqldump\n";
    }
}
