package vn.fs.backend.presentation.api.v1;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.web.util.UriComponentsBuilder;

import vn.fs.backend.application.dto.CartSnapshot;
import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.presentation.api.v1.dto.CartItemResponse;
import vn.fs.backend.presentation.api.v1.dto.CartResponse;

final class CartApiMapper {

    private CartApiMapper() {
    }

    static CartResponse toResponse(CartSnapshot snapshot) {
        return new CartResponse(
                snapshot.items().stream()
                        .map(CartApiMapper::toResponse)
                        .toList(),
                snapshot.distinctItems(),
                snapshot.totalQuantity(),
                money(snapshot.totalAmount()));
    }

    private static CartItemResponse toResponse(CartItem item) {
        String imageName = item.getProduct() == null
                ? null
                : item.getProduct().getProductImage();
        int availableStock = item.getProduct() == null
                ? 0
                : Math.max(0, item.getProduct().getQuantity());
        return new CartItemResponse(
                item.getId(),
                item.getDisplayName(),
                imageUrl(imageName),
                money(item.getUnitPrice()),
                item.getQuantity(),
                money(item.getUnitPrice() * item.getQuantity()),
                availableStock);
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String imageUrl(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return null;
        }
        return UriComponentsBuilder.fromPath("/loadImage")
                .queryParam("imageName", imageName)
                .build()
                .encode()
                .toUriString();
    }
}
