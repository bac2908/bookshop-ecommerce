package vn.fs.backend.application.dto;

import java.util.List;

import vn.fs.backend.domain.model.CartItem;

public record CartSnapshot(
        List<CartItem> items,
        int distinctItems,
        int totalQuantity,
        double totalAmount) {

    public CartSnapshot {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static CartSnapshot empty() {
        return new CartSnapshot(List.of(), 0, 0, 0.0);
    }
}
