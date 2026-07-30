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
        if (product == null) return unitPrice;
        double price = product.getPrice();
        double discount = product.getDiscount() / 100.0;
        return price * (1.0 - discount);
    }

    public double getLineTotal() {
        return getUnitPriceAfterDiscount() * Math.max(1, quantity);
    }

    public String getDisplayName() {
        if (product != null && product.getProductName() != null) return product.getProductName();
        return name;
    }
}
