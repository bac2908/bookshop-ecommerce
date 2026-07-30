package vn.fs.backend.presentation.api.v1;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.service.ShoppingCartService;
import vn.fs.backend.presentation.api.v1.dto.AddCartItemRequest;
import vn.fs.backend.presentation.api.v1.dto.ApiErrorResponse;
import vn.fs.backend.presentation.api.v1.dto.CartResponse;
import vn.fs.backend.presentation.api.v1.dto.UpdateCartItemRequest;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Giỏ hàng của người dùng hiện tại")
@SecurityRequirement(name = "sessionCookie")
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "CSRF token không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy sản phẩm hoặc mục giỏ hàng",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Số lượng vượt quá tồn kho",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class CartApiController {

    private final ShoppingCartService shoppingCartService;

    @GetMapping
    @Operation(summary = "Lấy giỏ hàng hiện tại")
    public CartResponse getCart() {
        return snapshot();
    }

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm hoặc tăng số lượng đang có")
    @SecurityRequirement(name = "csrfToken")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        shoppingCartService.addOrIncrease(request.productId(), request.quantity());
        return snapshot();
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Đặt số lượng chính xác cho một sản phẩm")
    @SecurityRequirement(name = "csrfToken")
    public CartResponse updateItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        shoppingCartService.setQuantity(productId, request.quantity());
        return snapshot();
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Xóa một sản phẩm khỏi giỏ")
    @SecurityRequirement(name = "csrfToken")
    public CartResponse removeItem(@PathVariable Long productId) {
        shoppingCartService.removeByProductId(productId);
        return snapshot();
    }

    @DeleteMapping("/items")
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponse(responseCode = "204", description = "Đã xóa toàn bộ giỏ hàng")
    public ResponseEntity<Void> clearCart() {
        shoppingCartService.clear();
        return ResponseEntity.noContent().build();
    }

    private CartResponse snapshot() {
        return CartApiMapper.toResponse(shoppingCartService.getSnapshot());
    }
}
