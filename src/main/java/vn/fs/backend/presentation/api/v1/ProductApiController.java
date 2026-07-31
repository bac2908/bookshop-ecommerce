package vn.fs.backend.presentation.api.v1;

import java.util.Locale;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.service.ProductService;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.presentation.api.v1.dto.PageResponse;
import vn.fs.backend.presentation.api.v1.dto.ApiErrorResponse;
import vn.fs.backend.presentation.api.v1.dto.ProductResponse;
import vn.fs.backend.presentation.api.v1.error.ApiResourceNotFoundException;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Tra cứu sản phẩm công khai")
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

}
