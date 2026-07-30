package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.fs.backend.application.dto.CategoryDTO;
import vn.fs.backend.application.dto.CatalogVisibilityResult;
import vn.fs.backend.application.error.CategoryNotFoundException;
import vn.fs.backend.application.error.DuplicateCategoryNameException;
import vn.fs.backend.domain.model.Category;
import vn.fs.backend.infrastructure.persistence.CategoryRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ImageStorageService image;

    /* lấy hết category để đổ bảng */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> findActive() {
        return categoryRepository.findByStatusTrueOrderByCategoryNameAsc();
    }

    /* lấy theo id để sửa */
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    /* check trùng tên khi thêm */
    public boolean nameExists(String name) {
        return StringUtils.isNotBlank(name)
                && categoryRepository.existsByCategoryNameIgnoreCase(name.trim());
    }

    /* check trùng tên khi sửa (bỏ qua chính nó) */
    public boolean nameExistsOther(String name, Long excludeId) {
        return StringUtils.isNotBlank(name)
                && categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(name.trim(), excludeId);
    }

    /* dto mặc định để bind form add */
    public CategoryDTO newDefaultDTO() {
        return CategoryDTO.builder().status(Boolean.TRUE).build();
    }

    /* map entity -> dto (đổ form edit) */
    public CategoryDTO toDTO(Category c) {
        return CategoryDTO.builder()
                .categoryId(c.getCategoryId())
                .categoryName(StringUtils.defaultString(c.getCategoryName()))
                .categoryImage(c.getCategoryImage())
                .status(c.getStatus() == null ? Boolean.TRUE : c.getStatus())
                .build();
    }

    /* map dto -> entity (xài nội bộ) */
    private void apply(CategoryDTO dto, Category c) {
        c.setCategoryName(dto.getCategoryName());
        c.setStatus(dto.getStatus() != null ? dto.getStatus() : Boolean.TRUE);
    }

    /* thêm mới category (ảnh nếu có thì promote từ file tạm) */
    @Transactional
    public Category create(CategoryDTO dto, MultipartFile upload) {
        dto.normalize();
        if (nameExists(dto.getCategoryName())) {
            throw new DuplicateCategoryNameException(dto.getCategoryName());
        }
        // nếu user có upload ảnh mới thì lưu tạm để tránh mất khi validate
        if (upload != null && !upload.isEmpty()) {
            String tmp = image.saveTemp(upload);
            if (tmp != null) dto.setCategoryImage(tmp);
        }
        // nếu là ảnh tạm thì đổi sang tên chính
        String finalImg = image.promoteIfTempOrKeep(dto.getCategoryImage(), "cat_");

        Category c = new Category();
        apply(dto, c);
        c.setCategoryImage(finalImg);
        return categoryRepository.save(c);
    }

    /* cập nhật category (hỗ trợ đổi ảnh, xóa ảnh cũ nếu đổi) */
    @Transactional
    public Category update(Long id, CategoryDTO dto, MultipartFile upload, String existingImage) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        dto.normalize();
        if (nameExistsOther(dto.getCategoryName(), id)) {
            throw new DuplicateCategoryNameException(dto.getCategoryName());
        }

        // nếu upload ảnh mới -> lưu tạm, set vào dto
        if (upload != null && !upload.isEmpty()) {
            String tmp = image.saveTemp(upload);
            if (tmp != null) dto.setCategoryImage(tmp);
        }

        // quyết định tên ảnh cuối cùng (ưu tiên dto, không thì dùng existing)
        String newName = StringUtils.isNotBlank(dto.getCategoryImage())
                ? dto.getCategoryImage()
                : existingImage;

        String finalName = image.promoteIfTempOrKeep(newName, "cat_");
        String old = c.getCategoryImage();
        boolean wasActive = Boolean.TRUE.equals(c.getStatus());

        apply(dto, c);
        c.setCategoryImage(finalName);
        Category saved = categoryRepository.save(c);
        boolean isActive = Boolean.TRUE.equals(saved.getStatus());
        if (wasActive != isActive) {
            if (isActive) {
                productRepository.visibleByCategory(id);
            } else {
                productRepository.hideByCategory(id);
            }
        }

        // nếu đổi ảnh thì xóa ảnh cũ (chỉ ảnh "chính", không xóa file tạm)
        if (StringUtils.isNotBlank(finalName) && !StringUtils.equals(finalName, old)) {
            image.deleteIfPermanent(old);
        }
        return saved;
    }

    /* ẩn category + ẩn toàn bộ sản phẩm con; nếu không còn sp tham chiếu thì xóa cứng */
    @Transactional
    public boolean hideAndCascade(Long id) {
        setVisibility(id, false);
        return false;
    }

    /* khôi phục category (không tự mở sp con) */
    @Transactional
    public void restore(Long id) {
        setVisibility(id, true);
    }

    @Transactional
    public CatalogVisibilityResult setVisibility(Long id, boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        category.setStatus(active);
        categoryRepository.save(category);
        int affected = active
                ? productRepository.visibleByCategory(id)
                : productRepository.hideByCategory(id);
        return new CatalogVisibilityResult(id, active, affected);
    }
}
