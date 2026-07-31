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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.dto.ProductDTO;
import vn.fs.backend.application.service.CategoryService;
import vn.fs.backend.application.service.ImageStorageService;
import vn.fs.backend.application.service.NxbService;
import vn.fs.backend.application.service.ProductService;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.presentation.api.v1.dto.CatalogVisibilityRequest;
import vn.fs.backend.presentation.api.v1.dto.ProductResponse;
import vn.fs.backend.presentation.api.v1.dto.ProductUpsertRequest;
import vn.fs.backend.presentation.api.v1.dto.StockAdjustmentRequest;
import vn.fs.backend.presentation.api.v1.error.ApiConflictException;
import vn.fs.backend.presentation.api.v1.error.ApiInvalidRequestException;
import vn.fs.backend.presentation.api.v1.error.ApiResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Admin products", description = "Quản trị sản phẩm, tồn kho và trạng thái bán")
@SecurityRequirement(name = "sessionCookie")
public class AdminProductApiController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final NxbService publisherService;
    private final ImageStorageService imageStorageService;

    @GetMapping
    @Operation(summary = "Danh sách toàn bộ sản phẩm cho quản trị")
    public List<ProductResponse> findAll() {
        return productService.findAll().stream()
                .map(ProductApiMapper::toResponse)
                .toList();
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Chi tiết sản phẩm cho quản trị")
    public ProductResponse findById(@PathVariable Long productId) {
        return ProductApiMapper.toResponse(requireProduct(productId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo sản phẩm")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponse(responseCode = "201", description = "Tạo sản phẩm thành công")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestPart("data") ProductUpsertRequest request,
            @RequestPart("file") MultipartFile file) {
        validateRequest(request, null);
        validateImage(file, true);

        ProductDTO dto = ProductApiMapper.toApplicationDto(request);
        dto.setProductImage(null);
        Product created = productService.create(dto, file);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getProductId())
                .toUri();
        return ResponseEntity.created(location).body(ProductApiMapper.toResponse(created));
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật sản phẩm")
    @SecurityRequirement(name = "csrfToken")
    public ProductResponse update(
            @PathVariable Long productId,
            @Valid @RequestPart("data") ProductUpsertRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Product existing = requireProduct(productId);
        validateRequest(request, productId);
        validateImage(file, false);

        ProductDTO dto = ProductApiMapper.toApplicationDto(request);
        dto.setProductImage(existing.getProductImage());
        return ProductApiMapper.toResponse(productService.update(
                productId,
                dto,
                file,
                existing.getProductImage()));
    }

    @PutMapping("/{productId}/stock")
    @Operation(summary = "Nhập thêm tồn kho")
    @SecurityRequirement(name = "csrfToken")
    public ProductResponse increaseStock(
            @PathVariable Long productId,
            @Valid @org.springframework.web.bind.annotation.RequestBody StockAdjustmentRequest request) {
        requireProduct(productId);
        if (!productService.increaseStock(productId, request.quantity())) {
            throw new ApiConflictException(
                    "STOCK_UPDATE_FAILED",
                    "Không thể cập nhật tồn kho sản phẩm");
        }
        return ProductApiMapper.toResponse(requireProduct(productId));
    }

    @PutMapping("/{productId}/visibility")
    @Operation(summary = "Ẩn hoặc mở bán sản phẩm")
    @SecurityRequirement(name = "csrfToken")
    public ProductResponse changeVisibility(
            @PathVariable Long productId,
            @Valid @org.springframework.web.bind.annotation.RequestBody CatalogVisibilityRequest request) {
        requireProduct(productId);
        return ProductApiMapper.toResponse(
                productService.setVisibility(productId, request.active()));
    }

    private void validateRequest(ProductUpsertRequest request, Long existingId) {
        categoryService.findById(request.categoryId())
                .filter(category -> Boolean.TRUE.equals(category.getStatus()))
                .orElseThrow(() -> new ApiResourceNotFoundException(
                        "CATEGORY_NOT_FOUND",
                        "Không tìm thấy thể loại đang hoạt động có id " + request.categoryId()));
        if (request.publisherId() != null) {
            publisherService.findById(request.publisherId())
                    .filter(publisher -> Boolean.TRUE.equals(publisher.getStatus()))
                    .orElseThrow(() -> new ApiResourceNotFoundException(
                            "PUBLISHER_NOT_FOUND",
                            "Không tìm thấy nhà xuất bản đang hoạt động có id " + request.publisherId()));
        }

        boolean duplicated = existingId == null
                ? productService.nameExists(request.normalizedName())
                : productService.nameExistsOther(request.normalizedName(), existingId);
        if (duplicated) {
            throw new ApiConflictException(
                    "PRODUCT_NAME_EXISTS",
                    "Tên sản phẩm đã tồn tại");
        }
    }

    private void validateImage(MultipartFile file, boolean required) {
        if (required && (file == null || file.isEmpty())) {
            throw new ApiInvalidRequestException(
                    "PRODUCT_IMAGE_REQUIRED",
                    "Vui lòng chọn ảnh sản phẩm");
        }
        if (!imageStorageService.isValidUpload(file)) {
            throw new ApiInvalidRequestException(
                    "INVALID_PRODUCT_IMAGE",
                    "Ảnh không hợp lệ; chỉ chấp nhận jpg, jpeg, png hoặc webp");
        }
    }

    private Product requireProduct(Long productId) {
        return productService.findById(productId)
                .orElseThrow(() -> new ApiResourceNotFoundException(
                        "PRODUCT_NOT_FOUND",
                        "Không tìm thấy sản phẩm có id " + productId));
    }
}
