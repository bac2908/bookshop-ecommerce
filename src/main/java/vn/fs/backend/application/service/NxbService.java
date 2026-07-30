package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.application.dto.NxbDTO;
import vn.fs.backend.application.dto.CatalogVisibilityResult;
import vn.fs.backend.application.error.DuplicatePublisherNameException;
import vn.fs.backend.application.error.PublisherNotFoundException;
import vn.fs.backend.domain.model.NXB;
import vn.fs.backend.infrastructure.persistence.NxbRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NxbService {

    private final NxbRepository nxbRepository;
    private final ProductRepository productRepository;

    // Lấy tất cả NXB (dùng cho trang list)
    public List<NXB> findAll() {
        return nxbRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<NXB> findActive() {
        return nxbRepository.findByStatusTrueOrderByNameAsc();
    }

    // Lấy NXB theo id
    public Optional<NXB> findById(Long id) {
        return nxbRepository.findById(id);
    }

    // Check trùng tên khi thêm mới
    public boolean nameExists(String name) {
        return StringUtils.isNotBlank(name) && nxbRepository.existsByNameIgnoreCase(name.trim());
    }

    // Check trùng tên khi sửa (bỏ qua chính nó)
    public boolean nameExistsOther(String name, Long excludeId) {
        return StringUtils.isNotBlank(name)
                && nxbRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), excludeId);
    }

    // Tạo DTO mặc định để bind form
    public NxbDTO newDefaultDTO() {
        return NxbDTO.builder().status(true).build();
    }

    // Map entity → DTO để đổ form edit
    public NxbDTO toDTO(NXB n) {
        return NxbDTO.builder()
                .id(n.getId())
                .name(StringUtils.defaultString(n.getName()))
                .status(n.getStatus() == null ? Boolean.TRUE : n.getStatus())
                .build();
    }

    // Map DTO → entity (xài nội bộ)
    private void apply(NxbDTO dto, NXB n) {
        n.setName(dto.getName());
        n.setStatus(dto.getStatus() == null ? Boolean.TRUE : dto.getStatus());
    }

    // Thêm mới NXB
    @Transactional
    public NXB create(NxbDTO dto) {
        dto.normalize();
        if (nameExists(dto.getName())) {
            throw new DuplicatePublisherNameException(dto.getName());
        }
        NXB entity = new NXB();
        apply(dto, entity);
        return nxbRepository.save(entity);
    }

    // Cập nhật NXB
    @Transactional
    public NXB update(Long id, NxbDTO dto) {
        NXB entity = nxbRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));
        dto.normalize();
        if (nameExistsOther(dto.getName(), id)) {
            throw new DuplicatePublisherNameException(dto.getName());
        }
        boolean wasActive = Boolean.TRUE.equals(entity.getStatus());
        apply(dto, entity);
        NXB saved = nxbRepository.save(entity);
        boolean isActive = Boolean.TRUE.equals(saved.getStatus());
        if (wasActive != isActive) {
            if (isActive) {
                productRepository.visibleByNxb(id);
            } else {
                productRepository.hideByNxb(id);
            }
        }
        return saved;
    }

    // Ẩn NXB và Ẩn toàn bộ sản phẩm thuộc NXB đó.
    // Nếu không còn sản phẩm tham chiếu thì xóa luôn NXB (hard delete).
    // Trả về true nếu đã xóa cứng; false nếu chỉ ẩn.
    @Transactional
    public boolean hideAndCascade(Long id) {
        setVisibility(id, false);
        return false;
    }

    // Khôi phục NXB (không tự mở sản phẩm con)
    @Transactional
    public void restore(Long id) {
        setVisibility(id, true);
    }

    @Transactional
    public CatalogVisibilityResult setVisibility(Long id, boolean active) {
        NXB publisher = nxbRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));
        publisher.setStatus(active);
        nxbRepository.save(publisher);
        int affected = active
                ? productRepository.visibleByNxb(id)
                : productRepository.hideByNxb(id);
        return new CatalogVisibilityResult(id, active, affected);
    }
}
