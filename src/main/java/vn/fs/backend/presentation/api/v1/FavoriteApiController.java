package vn.fs.backend.presentation.api.v1;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.service.CurrentUserService;
import vn.fs.backend.application.service.FavoriteService;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.presentation.api.v1.dto.ApiErrorResponse;
import vn.fs.backend.presentation.api.v1.dto.FavoriteCollectionResponse;
import vn.fs.backend.presentation.api.v1.dto.FavoriteStatusResponse;
import vn.fs.backend.presentation.api.v1.error.ApiUnauthorizedException;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Sản phẩm yêu thích của người dùng hiện tại")
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
                description = "Không tìm thấy sản phẩm đang bán",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class FavoriteApiController {

    private final FavoriteService favoriteService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Lấy danh sách yêu thích")
    public FavoriteCollectionResponse getFavorites() {
        User user = requireCurrentUser();
        var products = favoriteService.listActiveProducts(user.getUserId());
        return new FavoriteCollectionResponse(
                products.stream()
                        .map(ProductApiMapper::toResponse)
                        .toList(),
                products.size());
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Thêm sản phẩm vào yêu thích theo cách idempotent")
    @SecurityRequirement(name = "csrfToken")
    public FavoriteStatusResponse addFavorite(@PathVariable Long productId) {
        User user = requireCurrentUser();
        favoriteService.add(user.getUserId(), productId);
        return status(user.getUserId(), productId, true);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Xóa sản phẩm khỏi yêu thích theo cách idempotent")
    @SecurityRequirement(name = "csrfToken")
    public FavoriteStatusResponse removeFavorite(@PathVariable Long productId) {
        User user = requireCurrentUser();
        favoriteService.remove(user.getUserId(), productId);
        return status(user.getUserId(), productId, false);
    }

    private FavoriteStatusResponse status(
            Long userId,
            Long productId,
            boolean favorite) {
        return new FavoriteStatusResponse(
                productId,
                favorite,
                favoriteService.countByUser(userId));
    }

    private User requireCurrentUser() {
        User user = currentUserService.get();
        if (user == null) {
            throw new ApiUnauthorizedException(
                    "SESSION_USER_NOT_FOUND",
                    "Phiên đăng nhập không còn hợp lệ");
        }
        return user;
    }
}
