package com.medicalbilling.service;

import com.medicalbilling.entity.Customer;
import com.medicalbilling.entity.Prescription;
import com.medicalbilling.entity.User;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.PrescriptionRepository;
import com.medicalbilling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public List<Prescription> getByCustomer(Long customerId) {
        return prescriptionRepository.findByCustomerIdOrderByUploadedAtDesc(customerId);
    }

    @Transactional
    public Prescription upload(Long customerId, MultipartFile file, String notes, String username) throws IOException {
        Customer customer = customerService.findCustomer(customerId);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Path uploadPath = Paths.get(uploadDir, "prescriptions");
        Files.createDirectories(uploadPath);

        String originalName = file.getOriginalFilename();
        String fileName = UUID.randomUUID() + "_" + (originalName != null ? originalName : "prescription");
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        Prescription prescription = Prescription.builder()
                .customer(customer)
                .fileName(originalName)
                .filePath(filePath.toString())
                .notes(notes)
                .uploadedBy(user)
                .build();

        Prescription saved = prescriptionRepository.save(Objects.requireNonNull(prescription));
        auditService.log("UPLOAD", "Prescription", saved.getId(), username, "Uploaded prescription for customer: " + customer.getCustomerName());
        return saved;
    }
}
