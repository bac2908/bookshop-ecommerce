package vn.fs.backend.application.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartUpdateResponse {
    private boolean success;
    private String message;

    private Long productId;
    private int itemQty;
    private Integer remainingStock;

    private Double unitPrice;
    private Double lineTotal;
    private Double cartTotal;

    private Integer totalCartItems;
    private String unitPriceText;
    private String lineTotalText;
    private String cartTotalText;

    private List<MiniItem> mini;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniItem {
        private Long productId;
        private String name;
        private String image;
        private int qty;
        private String lineTotalText;
        private String remainingStockText;
    }
}
