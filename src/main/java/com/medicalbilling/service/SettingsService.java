package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.ShopSettings;
import com.medicalbilling.repository.ShopSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final ShopSettingsRepository shopSettingsRepository;
    private final AuditService auditService;
    private final BackupService backupService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.backup.dir}")
    private String backupDir;

    public ShopSettings getSettings() {
        return shopSettingsRepository.findAll().stream().findFirst()
                .orElse(ShopSettings.builder().build());
    }

    @Transactional
    public ShopSettings updateSettings(DtoModels.ShopSettingsRequest request, String username) {
        ShopSettings settings = getSettings();
        if (settings.getId() == null) {
            settings = ShopSettings.builder().build();
        }
        if (request.getShopName() != null) settings.setShopName(request.getShopName());
        if (request.getGstNumber() != null) settings.setGstNumber(request.getGstNumber());
        if (request.getAddress() != null) settings.setAddress(request.getAddress());
        if (request.getPhone() != null) settings.setPhone(request.getPhone());
        if (request.getEmail() != null) settings.setEmail(request.getEmail());
        if (request.getInvoiceFooter() != null) settings.setInvoiceFooter(request.getInvoiceFooter());
        if (request.getDefaultGstPercent() != null) settings.setDefaultGstPercent(request.getDefaultGstPercent());

        ShopSettings saved = shopSettingsRepository.save(Objects.requireNonNull(settings));
        auditService.log("UPDATE", "ShopSettings", saved.getId(), username, "Updated shop settings");
        return saved;
    }

    @Transactional
    public ShopSettings uploadLogo(MultipartFile file, String username) throws IOException {
        ShopSettings settings = getSettings();
        Path logoPath = Paths.get(uploadDir, "logos");
        Files.createDirectories(logoPath);
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = logoPath.resolve(fileName);
        Files.copy(file.getInputStream(), target);
        settings.setLogoPath(target.toString());
        ShopSettings saved = shopSettingsRepository.save(Objects.requireNonNull(settings));
        auditService.log("UPLOAD", "ShopSettings", saved.getId(), username, "Uploaded shop logo");
        return saved;
    }

    public String backupDatabase() throws IOException {
        return backupService.createBackup();
    }

    public java.util.List<String> listBackups() throws IOException {
        return backupService.listBackups();
    }
}
