(function (global, document) {
    "use strict";

    var api = global.bookshopApi;
    if (!api) {
        return;
    }

    function numberValue(value, fallback) {
        var parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : fallback;
    }

    function formValue(form, actionUrl, name, fallback) {
        var bodyValue = new FormData(form).get(name);
        if (bodyValue !== null && bodyValue !== "") {
            return bodyValue;
        }
        var queryValue = actionUrl.searchParams.get(name);
        return queryValue === null || queryValue === "" ? fallback : queryValue;
    }

    function setText(id, value) {
        var element = document.getElementById(id);
        if (element) {
            element.textContent = String(value);
        }
    }

    function updateCartBadges(cart) {
        if (!cart) {
            return;
        }
        setText("totalCartItems", cart.distinctItems);
        setText("miniCartCount", cart.totalQuantity);
        document.dispatchEvent(new CustomEvent("bookshop:cart-updated", {
            detail: cart
        }));
    }

    function updateFavoriteBadge(status) {
        if (!status) {
            return;
        }
        setText("totalSave", status.count);
        document.dispatchEvent(new CustomEvent("bookshop:favorites-updated", {
            detail: status
        }));
    }

    function redirectToLogin() {
        var returnUrl = global.location.pathname + global.location.search;
        global.location.assign("/login?redirect=" + encodeURIComponent(returnUrl));
    }

    function showError(error) {
        if (error && error.status === 401) {
            redirectToLogin();
            return;
        }
        global.alert(error && error.message
            ? error.message
            : "Không thể thực hiện thao tác. Vui lòng thử lại.");
    }

    async function mutateCartQuantity(productId, delta) {
        var cart = await api.cart();
        var item = cart.items.find(function (candidate) {
            return Number(candidate.productId) === Number(productId);
        });
        if (!item) {
            throw new api.ApiError(
                "Sản phẩm chưa có trong giỏ hàng",
                404,
                "CART_ITEM_NOT_FOUND",
                null
            );
        }
        var targetQuantity = Math.max(1, item.quantity + delta);
        return api.setCartItemQuantity(productId, targetQuantity);
    }

    async function handleLegacyMutation(form, actionUrl) {
        var path = actionUrl.pathname;
        var productId;
        var quantity;
        var response;

        if (path === "/addToCart") {
            productId = formValue(form, actionUrl, "productId", null);
            quantity = numberValue(formValue(form, actionUrl, "qty", 1), 1);
            response = await api.addCartItem(productId, Math.max(1, quantity));
            updateCartBadges(response);
            return true;
        }

        if (path === "/cart/inc" || path === "/cart/dec") {
            productId = formValue(form, actionUrl, "productId", null);
            var step = Math.max(
                1,
                numberValue(formValue(form, actionUrl, "step", 1), 1)
            );
            response = await mutateCartQuantity(
                productId,
                path === "/cart/inc" ? step : -step
            );
            updateCartBadges(response);
            return true;
        }

        var removeMatch = path.match(/^\/remove\/(\d+)$/);
        if (removeMatch) {
            response = await api.removeCartItem(removeMatch[1]);
            updateCartBadges(response);
            return true;
        }

        if (path === "/doFavorite" || path === "/doUnFavorite") {
            productId = formValue(form, actionUrl, "id", null);
            response = path === "/doFavorite"
                ? await api.addFavorite(productId)
                : await api.removeFavorite(productId);
            updateFavoriteBadge(response);
            return true;
        }

        return false;
    }

    document.addEventListener("submit", async function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.action) {
            return;
        }

        var actionUrl = new URL(form.action, global.location.origin);
        var supported = actionUrl.pathname === "/addToCart"
            || actionUrl.pathname === "/cart/inc"
            || actionUrl.pathname === "/cart/dec"
            || actionUrl.pathname === "/doFavorite"
            || actionUrl.pathname === "/doUnFavorite"
            || /^\/remove\/\d+$/.test(actionUrl.pathname);
        if (!supported) {
            return;
        }

        event.preventDefault();
        if (form.dataset.apiPending === "true") {
            return;
        }
        form.dataset.apiPending = "true";
        var submitButton = form.querySelector("[type='submit']");
        if (submitButton) {
            submitButton.disabled = true;
        }

        try {
            var handled = await handleLegacyMutation(form, actionUrl);
            if (handled) {
                global.location.reload();
            }
        } catch (error) {
            showError(error);
            form.dataset.apiPending = "false";
            if (submitButton) {
                submitButton.disabled = false;
            }
        }
    }, true);

    async function refreshHeaderCounters() {
        if (!document.querySelector("a[href='/profile']")) {
            return;
        }
        var results = await Promise.allSettled([
            api.cart(),
            api.favorites()
        ]);
        if (results[0].status === "fulfilled") {
            updateCartBadges(results[0].value);
        }
        if (results[1].status === "fulfilled") {
            updateFavoriteBadge(results[1].value);
        }
    }

    refreshHeaderCounters();
})(window, document);
