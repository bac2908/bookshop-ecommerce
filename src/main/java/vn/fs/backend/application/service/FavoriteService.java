package vn.fs.backend.application.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.fs.backend.application.error.ProductUnavailableException;
import vn.fs.backend.domain.model.Favorite;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.FavoriteRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Favorite> listByUser(Long userId) {
        return userId == null
                ? List.of()
                : favoriteRepository.findByUser_UserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Product> listActiveProducts(Long userId) {
        return listByUser(userId).stream()
                .map(Favorite::getProduct)
                .filter(product -> product != null && Boolean.TRUE.equals(product.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public int countByUser(Long userId) {
        return userId == null
                ? 0
                : Math.toIntExact(favoriteRepository.countByUser_UserId(userId));
    }

    @Transactional(readOnly = true)
    public boolean contains(Long userId, Long productId) {
        return userId != null
                && productId != null
                && favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public boolean add(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return true;
        }

        Product product = productRepository.findById(productId)
                .filter(item -> Boolean.TRUE.equals(item.getStatus()))
                .orElseThrow(() -> new ProductUnavailableException(productId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Người dùng không tồn tại"));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        favoriteRepository.save(favorite);
        return true;
    }

    @Transactional
    public boolean remove(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        favoriteRepository.deleteByUser_UserIdAndProduct_ProductId(userId, productId);
        return true;
    }

    @Transactional
    public boolean toggle(Long userId, Long productId) {
        if (contains(userId, productId)) {
            remove(userId, productId);
            return false;
        }
        return add(userId, productId);
    }
}
