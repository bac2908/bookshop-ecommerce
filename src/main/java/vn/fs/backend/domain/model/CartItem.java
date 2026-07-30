package vn.fs.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {

    private Long id;
    private String name;
    private double unitPrice;
    private int quantity;
    private Product product;

    public double getUnitPriceAfterDiscount() {
        return unitPrice;
    }

    public double getLineTotal() {
        return getUnitPriceAfterDiscount() * Math.max(1, quantity);
    }

    public String getDisplayName() {
        if (product != null && product.getProductName() != null) return product.getProductName();
        return name;
    }
}
