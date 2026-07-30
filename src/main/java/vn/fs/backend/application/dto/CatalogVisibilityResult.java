package vn.fs.backend.application.dto;

public record CatalogVisibilityResult(
        Long id,
        boolean active,
        int affectedProducts) {
}
