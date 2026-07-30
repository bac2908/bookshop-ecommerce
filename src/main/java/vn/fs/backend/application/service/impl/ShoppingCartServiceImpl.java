package vn.fs.backend.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.domain.model.*;
import vn.fs.backend.infrastructure.persistence.CartItemRepository;
import vn.fs.backend.infrastructure.persistence.CartRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.application.service.ShoppingCartService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private static final int CART_TTL_DAYS = 30;


    private User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        String login = auth.getName();
        return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login).orElse(null);
    }

    private Cart activeCartOrNull() {
        User user = currentUserOrNull();
        if (user == null) return null;
        return cartRepository.findByUser_UserIdAndStatus(user.getUserId(), CartStatus.ACTIVE).orElse(null);
    }

    private Cart getOrCreateActiveCart() {
        User user = currentUserOrNull();
        if (user == null) throw new IllegalStateException("Vui lòng đăng nhập để thao tác giỏ hàng.");
        return cartRepository.findByUser_UserIdAndStatus(user.getUserId(), CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUser(user);
                    c.setStatus(CartStatus.ACTIVE);
                    Date now = Date.from(Instant.now());
                    c.setCreatedAt(now);
                    c.setUpdatedAt(now);
                    c.setExpiresAt(Date.from(Instant.now().plus(CART_TTL_DAYS, ChronoUnit.DAYS)));
                    return cartRepository.save(c);
                });
    }

    private double unitPriceAfterDiscount(Product p) {
        return p.getPrice() - (p.getPrice() * p.getDiscount() / 100.0);
    }

    private CartItem toDto(CartItemEntity e) {
        CartItem dto = new CartItem();
        dto.setId(e.getProduct().getProductId());
        dto.setName(e.getProduct().getProductName());
        dto.setProduct(e.getProduct());
        dto.setQuantity(e.getQuantity());
        dto.setUnitPrice(e.getUnitPrice() != null ? e.getUnitPrice() : unitPriceAfterDiscount(e.getProduct()));
        return dto;
    }

    private int clampToStock(Product p, int desiredQty) {
        int stock = Math.max(0, p.getQuantity());
        return Math.max(0, Math.min(desiredQty, stock));
    }

    @Override
    @Transactional(readOnly = true)
    public int getQuantitySum() {
        Cart cart = activeCartOrNull();
        if (cart == null) return 0;
        return cartItemRepository.findByCart(cart).stream()
                .mapToInt(CartItemEntity::getQuantity).sum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getDistinctCount() {
        Cart cart = activeCartOrNull();
        if (cart == null) return 0;
        return cartItemRepository.findByCart(cart).size();
    }

    @Override
    @Transactional(readOnly = true)
    public double getAmount() {
        Cart cart = activeCartOrNull();
        if (cart == null) return 0.0;
        return cartItemRepository.findByCart(cart).stream()
                .mapToDouble(i -> (i.getUnitPrice() != null ? i.getUnitPrice() : unitPriceAfterDiscount(i.getProduct()))
                        * Math.max(1, i.getQuantity()))
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CartItem> getCartItems() {
        Cart cart = activeCartOrNull();
        if (cart == null) return Collections.emptyList();
        return cartItemRepository.findByCart(cart).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CartItem getItem(Long productId) {
        Cart cart = activeCartOrNull();
        if (cart == null) return null;
        return cartItemRepository.findByCart(cart).stream()
                .filter(ci -> Objects.equals(ci.getProduct().getProductId(), productId))
                .findFirst().map(this::toDto).orElse(null);
    }

    @Override
    @Transactional
    public int addOrIncrease(Long productId, int addQty) {
        Cart cart = getOrCreateActiveCart();
        Product p = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
        int stock = Math.max(0, p.getQuantity());
        if (stock == 0) return 0; // hết hàng

        CartItemEntity row = cartItemRepository.findByCartAndProduct(cart, p).orElse(null);
        Date now = Date.from(Instant.now());
        if (row == null) {
            int target = clampToStock(p, Math.max(1, addQty));
            CartItemEntity e = new CartItemEntity();
            e.setCart(cart);
            e.setProduct(p);
            e.setQuantity(target);
            e.setUnitPrice(unitPriceAfterDiscount(p));
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            e.setLastTouch(now);
            cartItemRepository.save(e);
            return target;
        } else {
            int target = clampToStock(p, row.getQuantity() + Math.max(1, addQty));
            row.setQuantity(target);
            row.setUpdatedAt(now);
            row.setLastTouch(now);
            cartItemRepository.save(row);
            return target;
        }
    }

    @Override
    @Transactional
    public int increase(Long productId, int step) {
        return addOrIncrease(productId, Math.max(1, step));
    }

    @Override
    @Transactional
    public int decrease(Long productId, int step) {
        Cart cart = getOrCreateActiveCart();
        Product p = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
        CartItemEntity row = cartItemRepository.findByCartAndProduct(cart, p)
                .orElseThrow(() -> new IllegalStateException("Mục này chưa có trong giỏ."));
        int newQty = Math.max(1, row.getQuantity() - Math.max(1, step));
        row.setQuantity(Math.min(newQty, Math.max(0, p.getQuantity())));
        row.setUpdatedAt(Date.from(Instant.now()));
        cartItemRepository.save(row);
        return row.getQuantity();
    }

    @Override
    @Transactional
    public void remove(Product product) {
        Cart cart = getOrCreateActiveCart();
        cartItemRepository.findByCartAndProduct(cart, product).ifPresent(cartItemRepository::delete);
    }

    @Override
    @Transactional
    public void remove(CartItem item) {
        if (item == null || item.getProduct() == null) return;
        remove(item.getProduct());
    }

    @Override
    @Transactional
    public void clear() {
        Cart cart = activeCartOrNull();
        if (cart != null) {
            cartItemRepository.deleteAll(cartItemRepository.findByCart(cart));
        }
    }
}
