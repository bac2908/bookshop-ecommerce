package vn.fs.backend.presentation.api.v1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductResponse")
public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        BigDecimal salePrice,
        Integer quantity,
        Integer discount,
        String description,
        LocalDate enteredDate,
        Boolean active,
        String imageName,
        String imageUrl,
        CategorySummary category,
        PublisherSummary publisher) {

    public record CategorySummary(Long id, String name) {
    }

    public record PublisherSummary(Long id, String name) {
    }
}
