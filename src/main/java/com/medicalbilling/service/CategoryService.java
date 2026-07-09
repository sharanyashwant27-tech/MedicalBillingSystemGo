package com.medicalbilling.service;

import com.medicalbilling.dto.DtoModels;
import com.medicalbilling.entity.Category;
import com.medicalbilling.exception.BusinessException;
import com.medicalbilling.exception.ResourceNotFoundException;
import com.medicalbilling.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    public List<DtoModels.CategoryResponse> getAll(String search) {
        List<Category> categories = (search == null || search.isBlank())
                ? categoryRepository.findAll()
                : categoryRepository.findByNameContainingIgnoreCase(search);
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DtoModels.CategoryResponse getById(Long id) {
        return toResponse(findCategory(id));
    }

    @Transactional
    public DtoModels.CategoryResponse create(DtoModels.CategoryRequest request, String username) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Category already exists");
        }
        Category saved = categoryRepository.save(Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        auditService.log("CREATE", "Category", saved.getId(), username, "Created category: " + saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public DtoModels.CategoryResponse update(Long id, DtoModels.CategoryRequest request, String username) {
        Category category = findCategory(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        Category saved = categoryRepository.save(category);
        auditService.log("UPDATE", "Category", saved.getId(), username, "Updated category: " + saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, String username) {
        Category category = findCategory(id);
        categoryRepository.delete(category);
        auditService.log("DELETE", "Category", id, username, "Deleted category: " + category.getName());
    }

    Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private DtoModels.CategoryResponse toResponse(Category category) {
        return DtoModels.CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
