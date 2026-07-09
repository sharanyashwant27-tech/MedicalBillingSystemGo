package com.medicalbilling.service;

import com.medicalbilling.entity.Branch;
import com.medicalbilling.exception.BusinessException;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Branch> getAll() {
        return branchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Branch> getActive() {
        return branchRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Branch getById(Long id) {
        return branchRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
    }

    @Transactional
    public Branch create(Branch branch, String username) {
        if (branchRepository.existsByBranchCode(branch.getBranchCode())) {
            throw new BusinessException("Branch code already exists");
        }
        Branch saved = branchRepository.save(branch);
        auditService.log("CREATE", "Branch", saved.getId(), username, "Created branch: " + saved.getBranchName());
        return saved;
    }

    @Transactional
    public Branch update(Long id, Branch updated, String username) {
        Branch branch = getById(id);
        branch.setBranchName(updated.getBranchName());
        branch.setAddress(updated.getAddress());
        branch.setPhone(updated.getPhone());
        branch.setEmail(updated.getEmail());
        branch.setCity(updated.getCity());
        branch.setState(updated.getState());
        branch.setPinCode(updated.getPinCode());
        branch.setActive(updated.isActive());
        Branch saved = branchRepository.save(branch);
        auditService.log("UPDATE", "Branch", saved.getId(), username, "Updated branch: " + saved.getBranchName());
        return saved;
    }

    @Transactional
    public void delete(Long id, String username) {
        Branch branch = getById(id);
        branch.setActive(false);
        branchRepository.save(branch);
        auditService.log("DELETE", "Branch", id, username, "Deactivated branch: " + branch.getBranchName());
    }
}
