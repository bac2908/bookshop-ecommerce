package vn.fs.backend.application.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.fs.backend.application.dto.CartSnapshot;
import vn.fs.backend.application.error.CartItemNotFoundException;
import vn.fs.backend.application.error.InsufficientStockException;
import vn.fs.backend.application.error.ProductUnavailableException;
import vn.fs.backend.application.service.CurrentUserService;
import vn.fs.backend.application.service.ShoppingCartService;
import vn.fs.backend.domain.model.Cart;
import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.CartItemEntity;
import vn.fs.backend.domain.model.CartStatus;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.CartItemRepository;
import vn.fs.backend.infrastructure.persistence.CartRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private static final int CART_TTL_DAYS = 30;

    private final CurrentUserService currentUserService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private User currentUserOrNull() {
        return currentUserService.get();
    }

    private User requireCurrentUser() {
        User user = currentUserOrNull();
        if (user == null) {
            throw new IllegalStateException("Vui lòng đăng nhập để thao tác giỏ hàng.");
        }
        return user;
    }

    private User requireCurrentUserForUpdate() {
        User user = requireCurrentUser();
        return userRepository.findByIdForUpdate(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Người dùng không tồn tại"));
    }

    private Cart activeCartOrNull(User user) {
        if (user == null) {
            return null;
        }
        return cartRepository.findByUser_UserIdAndStatus(user.getUserId(), CartStatus.ACTIVE)
                .orElse(null);
    }

    private Cart createActiveCart(User user, Date now) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCreatedAt(now);
        touchCart(cart, now);
        return cartRepository.save(cart);
    }

    private void touchCart(Cart cart, Date now) {
        cart.setUpdatedAt(now);
        cart.setExpiresAt(Date.from(
                now.toInstant().plus(CART_TTL_DAYS, ChronoUnit.DAYS)));
    }

    private Product requirePurchasableProduct(Long productId) {
        if (productId == null) {
            throw new ProductUnavailableException(null);
        }
        return productRepository.findById(productId)
                .filter(product -> Boolean.TRUE.equals(product.getStatus()))
                .orElseThrow(() -> new ProductUnavailableException(productId));
    }

    private void validateQuantity(Product product, int requestedQuantity) {
        int availableQuantity = Math.max(0, product.getQuantity());
        if (requestedQuantity < 1 || requestedQuantity > availableQuantity) {
            throw new InsufficientStockException(
                    product.getProductId(),
                    requestedQuantity,
                    availableQuantity);
        }
    }

    private double unitPriceAfterDiscount(Product product) {
        return product.getPrice()
                - (product.getPrice() * product.getDiscount() / 100.0);
    }

    private double itemUnitPrice(CartItemEntity item) {
        return item.getUnitPrice() != null
                ? item.getUnitPrice()
                : unitPriceAfterDiscount(item.getProduct());
    }

    private CartItem toDto(CartItemEntity entity) {
        CartItem dto = new CartItem();
        dto.setId(entity.getProduct().getProductId());
        dto.setName(entity.getProduct().getProductName());
        dto.setProduct(entity.getProduct());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(itemUnitPrice(entity));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CartSnapshot getSnapshot() {
        Cart cart = activeCartOrNull(currentUserOrNull());
        if (cart == null) {
            return CartSnapshot.empty();
        }

        List<CartItem> items = cartItemRepository.findByCart(cart).stream()
                .map(this::toDto)
                .toList();
        int totalQuantity = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        double totalAmount = items.stream()
                .mapToDouble(CartItem::getLineTotal)
                .sum();
        return new CartSnapshot(
                items,
                items.size(),
                totalQuantity,
                totalAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public int getQuantitySum() {
        return getSnapshot().totalQuantity();
    }

    @Override
    @Transactional(readOnly = true)
    public int getDistinctCount() {
        return getSnapshot().distinctItems();
    }

    @Override
    @Transactional(readOnly = true)
    public double getAmount() {
        return getSnapshot().totalAmount();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CartItem> getCartItems() {
        return getSnapshot().items();
    }

    @Override
    @Transactional(readOnly = true)
    public CartItem getItem(Long productId) {
        Cart cart = activeCartOrNull(currentUserOrNull());
        if (cart == null || productId == null) {
            return null;
        }
        return cartItemRepository.findByCartAndProduct_ProductId(cart, productId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public int addOrIncrease(Long productId, int addQuantity) {
        User user = requireCurrentUserForUpdate();
        Product product = requirePurchasableProduct(productId);
        Cart cart = activeCartOrNull(user);
        CartItemEntity item = cart == null
                ? null
                : cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
        int targetQuantity = (item == null ? 0 : item.getQuantity())
                + Math.max(1, addQuantity);
        validateQuantity(product, targetQuantity);

        Date now = Date.from(Instant.now());
        if (cart == null) {
            cart = createActiveCart(user, now);
        } else {
            touchCart(cart, now);
            cartRepository.save(cart);
        }

        if (item == null) {
            item = new CartItemEntity();
            item.setCart(cart);
            item.setProduct(product);
            item.setUnitPrice(unitPriceAfterDiscount(product));
            item.setCreatedAt(now);
        }
        item.setQuantity(targetQuantity);
        item.setUpdatedAt(now);
        item.setLastTouch(now);
        cartItemRepository.save(item);
        return targetQuantity;
    }

    @Override
    @Transactional
    public int increase(Long productId, int step) {
        return addOrIncrease(productId, Math.max(1, step));
    }

    @Override
    @Transactional
    public int decrease(Long productId, int step) {
        requireCurrentUserForUpdate();
        CartItem currentItem = getItem(productId);
        if (currentItem == null) {
            throw new CartItemNotFoundException(productId);
        }
        int targetQuantity = Math.max(1, currentItem.getQuantity() - Math.max(1, step));
        return setQuantity(productId, targetQuantity);
    }

    @Override
    @Transactional
    public int setQuantity(Long productId, int quantity) {
        User user = requireCurrentUserForUpdate();
        Product product = requirePurchasableProduct(productId);
        validateQuantity(product, quantity);

        Cart cart = activeCartOrNull(user);
        if (cart == null) {
            throw new CartItemNotFoundException(productId);
        }
        CartItemEntity item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        Date now = Date.from(Instant.now());
        item.setQuantity(quantity);
        item.setUpdatedAt(now);
        item.setLastTouch(now);
        cartItemRepository.save(item);
        touchCart(cart, now);
        cartRepository.save(cart);
        return quantity;
    }

    @Override
    @Transactional
    public void remove(Product product) {
        if (product != null) {
            removeByProductId(product.getProductId());
        }
    }

    @Override
    @Transactional
    public void remove(CartItem item) {
        if (item != null) {
            Long productId = item.getProduct() == null
                    ? item.getId()
                    : item.getProduct().getProductId();
            removeByProductId(productId);
        }
    }

    @Override
    @Transactional
    public boolean removeByProductId(Long productId) {
        User user = requireCurrentUserForUpdate();
        Cart cart = activeCartOrNull(user);
        if (cart == null || productId == null) {
            return false;
        }
        return cartItemRepository.findByCartAndProduct_ProductId(cart, productId)
                .map(item -> {
                    cartItemRepository.delete(item);
                    Date now = Date.from(Instant.now());
                    touchCart(cart, now);
                    cartRepository.save(cart);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void clear() {
        User user = requireCurrentUserForUpdate();
        Cart cart = activeCartOrNull(user);
        if (cart != null) {
            cartItemRepository.deleteByCart(cart);
            Date now = Date.from(Instant.now());
            touchCart(cart, now);
            cartRepository.save(cart);
        }
    }
}
