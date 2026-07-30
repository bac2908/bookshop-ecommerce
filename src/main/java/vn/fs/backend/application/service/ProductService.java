package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.fs.backend.application.dto.ProductDTO;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.infrastructure.persistence.CategoryRepository;
import vn.fs.backend.infrastructure.persistence.NxbRepository;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final NxbRepository nxbRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ImageStorageService image;

    /* ---------- Query ---------- */
    public List<Product> findAll() { return productRepository.findAll(); }
    public Optional<Product> findById(Long id) { return productRepository.findById(id); }
    public boolean nameExists(String name) { return productRepository.existsByProductNameIgnoreCase(name); }
    public boolean nameExistsOther(String name, Long excludeId) {
        return productRepository.existsByProductNameIgnoreCaseAndProductIdNot(name, excludeId);
    }

    /* ---------- Mapping ---------- */
    public ProductDTO newDefaultDTO() {
        return ProductDTO.builder()
                .status(true).discount(0).quantity(0).enteredDate(new Date()).build();
    }
    public ProductDTO toDTO(Product p) {
        return ProductDTO.builder()
                .productId(p.getProductId())
                .productName(StringUtils.defaultString(p.getProductName()))
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .nxbId(p.getNxb() != null ? p.getNxb().getId() : null)
                .price(p.getPrice())
                .quantity(p.getQuantity())
                .discount(p.getDiscount())
                .description(StringUtils.defaultString(p.getDescription()))
                .enteredDate(p.getEnteredDate())
                .status(p.getStatus() == null ? Boolean.TRUE : p.getStatus())
                .productImage(p.getProductImage())
                .build();
    }
    private void apply(ProductDTO dto, Product p) {
        p.setProductName(dto.getProductName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setQuantity(dto.getQuantity());
        p.setDiscount(dto.getDiscount());
        p.setEnteredDate(dto.getEnteredDate());
        p.setStatus(dto.getStatus());
        if (dto.getCategoryId() != null)
            categoryRepository.findById(dto.getCategoryId()).ifPresent(p::setCategory);
        else p.setCategory(null);
        if (dto.getNxbId() != null)
            nxbRepository.findById(dto.getNxbId()).ifPresent(p::setNxb);
        else p.setNxb(null);
    }

    /* ---------- Create ---------- */
    @Transactional
    public Product create(ProductDTO dto, MultipartFile upload) {
        // ảnh: nếu có file → lưu tạm trước khi validate khác ở Controller
        if (upload != null && !upload.isEmpty()) {
            String tmp = image.saveTemp(upload);
            if (tmp != null) dto.setProductImage(tmp);
        }
        String finalImg = image.promoteIfTempOrKeep(dto.getProductImage(), "prd_");
        Product p = new Product();
        apply(dto, p);
        p.setProductImage(finalImg);
        return productRepository.save(p);
    }

    /* ---------- Update ---------- */
    @Transactional
    public Product update(Long id, ProductDTO dto, MultipartFile upload, String existingImage) {
        Product p = productRepository.findById(id).orElseThrow();
        // ảnh: nếu có file mới → lưu tạm cho DTO
        if (upload != null && !upload.isEmpty()) {
            String tmp = image.saveTemp(upload);
            if (tmp != null) dto.setProductImage(tmp);
        }
        String newName = dto.getProductImage();
        if (StringUtils.isBlank(newName)) newName = existingImage;

        String finalName = image.promoteIfTempOrKeep(newName, "prd_");
        String old = p.getProductImage();

        apply(dto, p);
        p.setProductImage(finalName);
        Product saved = productRepository.save(p);

        // xóa ảnh cũ nếu đã đổi sang ảnh mới
        if (StringUtils.isNotBlank(finalName) && !StringUtils.equals(finalName, old)) {
            image.deleteIfPermanent(old);
        }
        return saved;
    }

    /* ---------- Stock ---------- */
    @Transactional
    public boolean increaseStock(Long id, int qty) {
        if (qty <= 0) return false;
        return productRepository.increaseStock(id, qty) == 1;
    }

    /* ---------- Delete / Restore ---------- */
    @Transactional
    public boolean softOrHardDelete(Long id) {
        Product p = productRepository.findById(id).orElseThrow();
        boolean referenced = orderDetailRepository.existsRefByProductId(id);
        if (referenced) {
            p.setStatus(false);
            productRepository.save(p);
            return false; // đã ẩn (soft)
        }
        String img = p.getProductImage();
        productRepository.deleteById(id);
        image.deleteIfPermanent(img);
        return true; // xóa cứng thành công
    }

    @Transactional
    public void restore(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setStatus(true);
            productRepository.save(p);
        });
    }
    /* ---------- Restore by Category / NXB ---------- */
    @Transactional
    public int restoreByCategory(Long categoryId) {
        return productRepository.visibleByCategory(categoryId);
    }

    @Transactional
    public int restoreByNxb(Long nxbId) {
        return productRepository.visibleByNxb(nxbId);
    }

}
