package vn.fs.backend.presentation.api.v1;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.dto.CatalogVisibilityResult;
import vn.fs.backend.application.dto.NxbDTO;
import vn.fs.backend.application.error.PublisherNotFoundException;
import vn.fs.backend.application.service.NxbService;
import vn.fs.backend.domain.model.NXB;
import vn.fs.backend.presentation.api.v1.dto.CatalogVisibilityRequest;
import vn.fs.backend.presentation.api.v1.dto.CatalogVisibilityResponse;
import vn.fs.backend.presentation.api.v1.dto.PublisherAdminUpsertRequest;
import vn.fs.backend.presentation.api.v1.dto.PublisherResponse;

@RestController
@RequestMapping("/api/v1/admin/publishers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Tag(name = "Admin publishers", description = "Quản trị nhà xuất bản")
@SecurityRequirement(name = "sessionCookie")
public class AdminPublisherApiController {

    private final NxbService publisherService;

    @GetMapping
    @Operation(summary = "Danh sách tất cả nhà xuất bản, gồm cả bản ghi đã ẩn")
    public List<PublisherResponse> publishers() {
        return publisherService.findAll().stream()
                .map(ProductApiMapper::toResponse)
                .toList();
    }

    @GetMapping("/{publisherId}")
    @Operation(summary = "Chi tiết nhà xuất bản")
    public PublisherResponse publisher(@PathVariable Long publisherId) {
        return ProductApiMapper.toResponse(requirePublisher(publisherId));
    }

    @PostMapping
    @Operation(summary = "Tạo nhà xuất bản")
    @SecurityRequirement(name = "csrfToken")
    public ResponseEntity<PublisherResponse> create(
            @Valid @RequestBody PublisherAdminUpsertRequest request) {
        NXB created = publisherService.create(toDto(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ProductApiMapper.toResponse(created));
    }

    @PutMapping("/{publisherId}")
    @Operation(summary = "Cập nhật nhà xuất bản")
    @SecurityRequirement(name = "csrfToken")
    public PublisherResponse update(
            @PathVariable Long publisherId,
            @Valid @RequestBody PublisherAdminUpsertRequest request) {
        requirePublisher(publisherId);
        return ProductApiMapper.toResponse(
                publisherService.update(publisherId, toDto(request)));
    }

    @PutMapping("/{publisherId}/visibility")
    @Operation(summary = "Ẩn/hiện nhà xuất bản và đồng bộ trạng thái sản phẩm con")
    @SecurityRequirement(name = "csrfToken")
    public CatalogVisibilityResponse visibility(
            @PathVariable Long publisherId,
            @Valid @RequestBody CatalogVisibilityRequest request) {
        CatalogVisibilityResult result = publisherService.setVisibility(
                publisherId,
                request.active());
        return new CatalogVisibilityResponse(
                result.id(),
                result.active(),
                result.affectedProducts());
    }

    private NXB requirePublisher(Long id) {
        return publisherService.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));
    }

    private NxbDTO toDto(PublisherAdminUpsertRequest request) {
        return NxbDTO.builder()
                .name(request.normalizedName())
                .status(request.active())
                .build();
    }
}
