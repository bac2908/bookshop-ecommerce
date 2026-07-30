package vn.fs.backend.presentation.api.v1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;

import org.springframework.web.util.UriComponentsBuilder;

import vn.fs.backend.application.dto.ProductDTO;
import vn.fs.backend.domain.model.Category;
import vn.fs.backend.domain.model.NXB;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.presentation.api.v1.dto.CategoryResponse;
import vn.fs.backend.presentation.api.v1.dto.ProductResponse;
import vn.fs.backend.presentation.api.v1.dto.ProductUpsertRequest;
import vn.fs.backend.presentation.api.v1.dto.PublisherResponse;

final class ProductApiMapper {

    private ProductApiMapper() {
    }

    static ProductResponse toResponse(Product product) {
        BigDecimal price = BigDecimal.valueOf(product.getPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal salePrice = price
                .multiply(BigDecimal.valueOf(100L - product.getDiscount()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        ProductResponse.CategorySummary category = product.getCategory() == null
                ? null
                : new ProductResponse.CategorySummary(
                        product.getCategory().getCategoryId(),
                        product.getCategory().getCategoryName());
        ProductResponse.PublisherSummary publisher = product.getNxb() == null
                ? null
                : new ProductResponse.PublisherSummary(
                        product.getNxb().getId(),
                        product.getNxb().getName());

        return new ProductResponse(
                product.getProductId(),
                product.getProductName(),
                price,
                salePrice,
                product.getQuantity(),
                product.getDiscount(),
                product.getDescription(),
                toLocalDate(product),
                product.getStatus(),
                product.getProductImage(),
                imageUrl(product.getProductImage()),
                category,
                publisher);
    }

    static ProductDTO toApplicationDto(ProductUpsertRequest request) {
        return ProductDTO.builder()
                .productName(request.normalizedName())
                .price(request.price().doubleValue())
                .quantity(request.quantity())
                .discount(request.discount())
                .description(request.normalizedDescription())
                .enteredDate(Date.valueOf(request.enteredDate()))
                .status(request.active())
                .categoryId(request.categoryId())
                .nxbId(request.publisherId())
                .productImage(request.imageName())
                .build();
    }

    static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getCategoryImage(),
                imageUrl(category.getCategoryImage()),
                category.getStatus());
    }

    static PublisherResponse toResponse(NXB publisher) {
        return new PublisherResponse(
                publisher.getId(),
                publisher.getName(),
                publisher.getStatus());
    }

    private static LocalDate toLocalDate(Product product) {
        if (product.getEnteredDate() == null) {
            return null;
        }
        return new Date(product.getEnteredDate().getTime()).toLocalDate();
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
