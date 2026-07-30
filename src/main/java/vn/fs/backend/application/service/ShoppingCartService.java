package vn.fs.backend.application.service;

import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.Product;

import java.util.Collection;

public interface ShoppingCartService {


    int getQuantitySum();
    int getDistinctCount();


    double getAmount();

    Collection<CartItem> getCartItems();

    int addOrIncrease(Long productId, int addQty);


    int increase(Long productId, int step);
    int decrease(Long productId, int step);

    void remove(Product product);
    void remove(CartItem item);

    CartItem getItem(Long productId);

    void clear();
}
