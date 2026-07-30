package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.application.dto.NxbDTO;
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
        NXB entity = new NXB();
        apply(dto, entity);
        return nxbRepository.save(entity);
    }

    // Cập nhật NXB
    @Transactional
    public NXB update(Long id, NxbDTO dto) {
        NXB entity = nxbRepository.findById(id).orElseThrow();
        apply(dto, entity);
        return nxbRepository.save(entity);
    }

    // Ẩn NXB và Ẩn toàn bộ sản phẩm thuộc NXB đó.
    // Nếu không còn sản phẩm tham chiếu thì xóa luôn NXB (hard delete).
    // Trả về true nếu đã xóa cứng; false nếu chỉ ẩn.
    @Transactional
    public boolean hideAndCascade(Long id) {
        NXB nxb = nxbRepository.findById(id).orElseThrow();

        // 1) Ẩn tất cả sản phẩm con theo NXB
        productRepository.hideByNxb(id);

        // 2) Ẩn NXB
        nxb.setStatus(false);
        nxbRepository.save(nxb);

        // 3) Không còn sp tham chiếu → xóa cứng
        long totalRef = productRepository.countByNxb_Id(id);
        if (totalRef == 0) {
            try {
                nxbRepository.deleteById(id);
                return true;
            } catch (Exception ignore) {
                // Nếu DB còn ràng buộc thì thôi, giữ ẩn
            }
        }
        return false;
    }

    // Khôi phục NXB (không tự mở sản phẩm con)
    @Transactional
    public void restore(Long id) {
        nxbRepository.findById(id).ifPresent(n -> {
            n.setStatus(true);
            nxbRepository.save(n);
        });
    }
}
