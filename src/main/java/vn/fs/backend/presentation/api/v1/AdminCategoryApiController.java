package vn.fs.backend.presentation.api.v1;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.dto.CatalogVisibilityResult;
import vn.fs.backend.application.dto.CategoryDTO;
import vn.fs.backend.application.error.CategoryNotFoundException;
import vn.fs.backend.application.service.CategoryService;
import vn.fs.backend.application.service.ImageStorageService;
import vn.fs.backend.domain.model.Category;
import vn.fs.backend.presentation.api.v1.dto.CatalogVisibilityRequest;
import vn.fs.backend.presentation.api.v1.dto.CatalogVisibilityResponse;
import vn.fs.backend.presentation.api.v1.dto.CategoryAdminUpsertRequest;
import vn.fs.backend.presentation.api.v1.dto.CategoryResponse;
import vn.fs.backend.presentation.api.v1.error.ApiConflictException;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Tag(name = "Admin categories", description = "Quản trị thể loại và ảnh")
@SecurityRequirement(name = "sessionCookie")
public class AdminCategoryApiController {

    private final CategoryService categoryService;
    private final ImageStorageService imageStorageService;

    @GetMapping
    @Operation(summary = "Danh sách tất cả thể loại, gồm cả thể loại đã ẩn")
    public List<CategoryResponse> categories() {
        return categoryService.findAll().stream()
                .map(ProductApiMapper::toResponse)
                .toList();
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Chi tiết thể loại")
    public CategoryResponse category(@PathVariable Long categoryId) {
        return ProductApiMapper.toResponse(requireCategory(categoryId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo thể loại")
    @SecurityRequirement(name = "csrfToken")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestPart("data") CategoryAdminUpsertRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        validateImage(file);
        Category created = categoryService.create(toDto(request), file);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getCategoryId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ProductApiMapper.toResponse(created));
    }

    @PutMapping(
            path = "/{categoryId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật thể loại")
    @SecurityRequirement(name = "csrfToken")
    public CategoryResponse update(
            @PathVariable Long categoryId,
            @Valid @RequestPart("data") CategoryAdminUpsertRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        validateImage(file);
        Category existing = requireCategory(categoryId);
        Category updated = categoryService.update(
                categoryId,
                toDto(request),
                file,
                existing.getCategoryImage());
        return ProductApiMapper.toResponse(updated);
    }

    @PutMapping("/{categoryId}/visibility")
    @Operation(summary = "Ẩn/hiện thể loại và đồng bộ trạng thái sản phẩm con")
    @SecurityRequirement(name = "csrfToken")
    public CatalogVisibilityResponse visibility(
            @PathVariable Long categoryId,
            @Valid @RequestBody CatalogVisibilityRequest request) {
        CatalogVisibilityResult result = categoryService.setVisibility(
                categoryId,
                request.active());
        return new CatalogVisibilityResponse(
                result.id(),
                result.active(),
                result.affectedProducts());
    }

    private Category requireCategory(Long id) {
        return categoryService.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private CategoryDTO toDto(CategoryAdminUpsertRequest request) {
        return CategoryDTO.builder()
                .categoryName(request.normalizedName())
                .status(request.active())
                .build();
    }

    private void validateImage(MultipartFile file) {
        if (!imageStorageService.isValidUpload(file)) {
            throw new ApiConflictException(
                    "INVALID_CATEGORY_IMAGE",
                    "Ảnh thể loại chỉ chấp nhận jpg, jpeg, png hoặc webp");
        }
    }
}
