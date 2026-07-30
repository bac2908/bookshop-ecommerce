package vn.fs.backend.application.service;

import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.application.dto.CartSnapshot;

import java.util.Collection;

public interface ShoppingCartService {

    CartSnapshot getSnapshot();

    int getQuantitySum();
    int getDistinctCount();


    double getAmount();

    Collection<CartItem> getCartItems();

    int addOrIncrease(Long productId, int addQty);


    int increase(Long productId, int step);
    int decrease(Long productId, int step);
    int setQuantity(Long productId, int quantity);

    void remove(Product product);
    void remove(CartItem item);
    boolean removeByProductId(Long productId);

    CartItem getItem(Long productId);

    void clear();
}
