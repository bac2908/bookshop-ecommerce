package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.domain.model.Favorite;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.FavoriteRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    // ds theo user
    public List<Favorite> listByUser(Long userId) {
        return favoriteRepository.findByUser_UserId(userId);
    }

    // đếm
    public int countByUser(Long userId) {
        Integer n = favoriteRepository.selectCountSave(userId);
        return n == null ? 0 : n;
    }

    // thêm (tránh trùng)
    @Transactional
    public boolean add(Long userId, Long productId) {
        if (userId == null || productId == null) return false;
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) return true;
        Optional<Product> p = productRepository.findById(productId);
        if (p.isEmpty()) return false;

        Favorite f = new Favorite();
        User u = new User();
        u.setUserId(userId);
        f.setUser(u);
        f.setProduct(p.get());
        favoriteRepository.save(f);
        return true;
    }

    // xoá
    @Transactional
    public boolean remove(Long userId, Long productId) {
        if (userId == null || productId == null) return false;
        favoriteRepository.deleteByUser_UserIdAndProduct_ProductId(userId, productId);
        return true;
    }

    // toggle
    @Transactional
    public boolean toggle(Long userId, Long productId) {
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            favoriteRepository.deleteByUser_UserIdAndProduct_ProductId(userId, productId);
            return false;
        }
        return add(userId, productId);
    }
}
