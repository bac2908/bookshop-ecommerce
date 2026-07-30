package vn.fs.backend.presentation.api.v1;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.service.CategoryService;
import vn.fs.backend.application.service.NxbService;
import vn.fs.backend.presentation.api.v1.dto.CategoryResponse;
import vn.fs.backend.presentation.api.v1.dto.PublisherResponse;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Dữ liệu danh mục công khai")
public class CatalogApiController {

    private final CategoryService categoryService;
    private final NxbService publisherService;

    @GetMapping("/categories")
    @Operation(summary = "Danh sách thể loại đang hoạt động")
    public List<CategoryResponse> categories() {
        return categoryService.findActive().stream()
                .map(ProductApiMapper::toResponse)
                .toList();
    }

    @GetMapping("/publishers")
    @Operation(summary = "Danh sách nhà xuất bản đang hoạt động")
    public List<PublisherResponse> publishers() {
        return publisherService.findActive().stream()
                .map(ProductApiMapper::toResponse)
                .toList();
    }
}
