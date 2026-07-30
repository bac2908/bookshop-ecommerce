package vn.fs.backend.presentation.api.v1;

import java.net.URI;
import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.dto.ProductDTO;
import vn.fs.backend.application.service.CategoryService;
import vn.fs.backend.application.service.NxbService;
import vn.fs.backend.application.service.ProductService;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.presentation.api.v1.dto.PageResponse;
import vn.fs.backend.presentation.api.v1.dto.ApiErrorResponse;
import vn.fs.backend.presentation.api.v1.dto.ProductResponse;
import vn.fs.backend.presentation.api.v1.dto.ProductUpsertRequest;
import vn.fs.backend.presentation.api.v1.error.ApiConflictException;
import vn.fs.backend.presentation.api.v1.error.ApiResourceNotFoundException;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Tra cứu và quản trị sản phẩm")
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Yêu cầu hoặc dữ liệu không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Thiếu quyền hoặc CSRF token không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy tài nguyên",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Dữ liệu bị trùng hoặc xung đột",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class ProductApiController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductService productService;
    private final CategoryService categoryService;
    private final NxbService publisherService;

    @GetMapping
    @Operation(summary = "Danh sách sản phẩm đang bán", description = "Hỗ trợ phân trang, tìm kiếm và lọc.")
    public PageResponse<ProductResponse> findAll(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page phải lớn hơn hoặc bằng 0")
            int page,
            @RequestParam(defaultValue = "12")
            @Min(value = 1, message = "size phải lớn hơn hoặc bằng 1")
            @Max(value = MAX_PAGE_SIZE, message = "size tối đa là 100")
            int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(defaultValue = "enteredDate")
            @Pattern(
                    regexp = "productId|productName|price|enteredDate|discount",
                    message = "sortBy không được hỗ trợ")
            String sortBy,
            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "(?i)asc|desc", message = "direction chỉ nhận asc hoặc desc")
            String direction) {

        Sort sort = Sort.by(
                Sort.Direction.fromString(direction.toUpperCase(Locale.ROOT)),
                sortBy);
        Page<Product> products = productService.findActive(
                keyword,
                categoryId,
                publisherId,
                PageRequest.of(page, size, sort));
        return PageResponse.from(products, ProductApiMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một sản phẩm đang bán")
    public ProductResponse findById(
            @Parameter(description = "ID sản phẩm", example = "1")
            @PathVariable Long id) {
        return ProductApiMapper.toResponse(productService.findActiveById(id)
                .orElseThrow(() -> new ApiResourceNotFoundException(
                        "PRODUCT_NOT_FOUND",
                        "Không tìm thấy sản phẩm đang bán có id " + id)));
    }

    @PostMapping
    @Operation(summary = "Tạo sản phẩm", description = "Yêu cầu vai trò ADMIN hoặc STAFF.")
    @ApiResponse(responseCode = "201", description = "Tạo sản phẩm thành công")
    @SecurityRequirement(name = "sessionCookie")
    @SecurityRequirement(name = "csrfToken")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductUpsertRequest request) {
        validateReferences(request);
        if (productService.nameExists(request.normalizedName())) {
            throw new ApiConflictException(
                    "PRODUCT_NAME_EXISTS",
                    "Tên sản phẩm đã tồn tại");
        }

        Product created = productService.create(ProductApiMapper.toApplicationDto(request), null);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getProductId())
                .toUri();
        return ResponseEntity.created(location).body(ProductApiMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sản phẩm", description = "Yêu cầu vai trò ADMIN hoặc STAFF.")
    @SecurityRequirement(name = "sessionCookie")
    @SecurityRequirement(name = "csrfToken")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpsertRequest request) {

        Product existing = productService.findById(id)
                .orElseThrow(() -> new ApiResourceNotFoundException(
                        "PRODUCT_NOT_FOUND",
                        "Không tìm thấy sản phẩm có id " + id));
        validateReferences(request);
        if (productService.nameExistsOther(request.normalizedName(), id)) {
            throw new ApiConflictException(
                    "PRODUCT_NAME_EXISTS",
                    "Tên sản phẩm đã tồn tại");
        }

        ProductDTO dto = ProductApiMapper.toApplicationDto(request);
        String existingImage = existing.getProductImage();
        if (request.imageName() == null || request.imageName().isBlank()) {
            dto.setProductImage(existingImage);
        }
        return ProductApiMapper.toResponse(
                productService.update(id, dto, null, existingImage));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xóa hoặc ẩn sản phẩm",
            description = "Sản phẩm đã có đơn hàng sẽ được ẩn; sản phẩm chưa được tham chiếu sẽ bị xóa.")
    @SecurityRequirement(name = "sessionCookie")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponse(responseCode = "204", description = "Xóa hoặc ẩn sản phẩm thành công")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.findById(id)
                .orElseThrow(() -> new ApiResourceNotFoundException(
                        "PRODUCT_NOT_FOUND",
                        "Không tìm thấy sản phẩm có id " + id));
        productService.softOrHardDelete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateReferences(ProductUpsertRequest request) {
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
    }
}
