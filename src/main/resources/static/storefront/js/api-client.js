(function (global) {
    "use strict";

    var csrfState = null;
    var csrfRequest = null;
    var unsafeMethods = new Set(["POST", "PUT", "PATCH", "DELETE"]);

    function BookshopApiError(message, status, code, details) {
        this.name = "BookshopApiError";
        this.message = message;
        this.status = status;
        this.code = code;
        this.details = details;
    }
    BookshopApiError.prototype = Object.create(Error.prototype);
    BookshopApiError.prototype.constructor = BookshopApiError;

    function sameOriginUrl(path) {
        var url = new URL(path, global.location.origin);
        if (url.origin !== global.location.origin) {
            throw new BookshopApiError(
                "API client chỉ cho phép request cùng origin",
                0,
                "CROSS_ORIGIN_REQUEST_BLOCKED",
                null
            );
        }
        return url;
    }

    async function parseResponse(response) {
        if (response.status === 204) {
            return null;
        }
        var contentType = response.headers.get("content-type") || "";
        if (contentType.indexOf("application/json") >= 0) {
            return response.json();
        }
        var text = await response.text();
        return text || null;
    }

    async function loadCsrf(forceRefresh) {
        if (!forceRefresh && csrfState) {
            return csrfState;
        }
        if (!forceRefresh && csrfRequest) {
            return csrfRequest;
        }

        csrfRequest = fetch("/api/v1/auth/csrf", {
            method: "GET",
            credentials: "same-origin",
            headers: { "Accept": "application/json" },
            cache: "no-store"
        }).then(async function (response) {
            var body = await parseResponse(response);
            if (!response.ok) {
                throw toApiError(response, body);
            }
            csrfState = body;
            return csrfState;
        }).finally(function () {
            csrfRequest = null;
        });

        return csrfRequest;
    }

    function toApiError(response, body) {
        var message = body && body.message
            ? body.message
            : "API request không thành công";
        return new BookshopApiError(
            message,
            response.status,
            body && body.code ? body.code : "HTTP_" + response.status,
            body
        );
    }

    async function request(path, options, retriedAfterCsrfFailure) {
        options = options || {};
        var method = (options.method || "GET").toUpperCase();
        var url = sameOriginUrl(path);
        var headers = new Headers(options.headers || {});
        headers.set("Accept", "application/json");

        var body = options.body;
        if (body !== undefined
            && body !== null
            && !(body instanceof FormData)
            && !(body instanceof URLSearchParams)
            && typeof body !== "string") {
            headers.set("Content-Type", "application/json");
            body = JSON.stringify(body);
        }

        if (unsafeMethods.has(method)) {
            var csrf = await loadCsrf(false);
            headers.set(csrf.headerName, csrf.token);
        }

        var response = await fetch(url.toString(), {
            method: method,
            headers: headers,
            body: body,
            credentials: "same-origin",
            cache: options.cache || "no-store",
            signal: options.signal
        });
        var responseBody = await parseResponse(response);

        if (response.status === 403
            && responseBody
            && responseBody.code === "CSRF_TOKEN_INVALID"
            && !retriedAfterCsrfFailure) {
            await loadCsrf(true);
            return request(path, options, true);
        }

        if (!response.ok) {
            throw toApiError(response, responseBody);
        }
        return responseBody;
    }

    async function login(loginValue, password) {
        var user = await request("/api/v1/auth/login", {
            method: "POST",
            body: {
                login: loginValue,
                password: password
            }
        });
        await loadCsrf(true);
        return user;
    }

    async function logout() {
        await request("/api/v1/auth/logout", { method: "POST" });
        csrfState = null;
        await loadCsrf(true);
    }

    global.bookshopApi = Object.freeze({
        request: request,
        get: function (path, options) {
            return request(path, Object.assign({}, options, { method: "GET" }));
        },
        post: function (path, body, options) {
            return request(path, Object.assign({}, options, { method: "POST", body: body }));
        },
        put: function (path, body, options) {
            return request(path, Object.assign({}, options, { method: "PUT", body: body }));
        },
        delete: function (path, options) {
            return request(path, Object.assign({}, options, { method: "DELETE" }));
        },
        csrf: loadCsrf,
        login: login,
        logout: logout,
        me: function () {
            return request("/api/v1/auth/me", { method: "GET" });
        },
        cart: function () {
            return request("/api/v1/cart", { method: "GET" });
        },
        addCartItem: function (productId, quantity) {
            return request("/api/v1/cart/items", {
                method: "POST",
                body: {
                    productId: productId,
                    quantity: quantity
                }
            });
        },
        setCartItemQuantity: function (productId, quantity) {
            return request("/api/v1/cart/items/" + encodeURIComponent(productId), {
                method: "PUT",
                body: { quantity: quantity }
            });
        },
        removeCartItem: function (productId) {
            return request("/api/v1/cart/items/" + encodeURIComponent(productId), {
                method: "DELETE"
            });
        },
        clearCart: function () {
            return request("/api/v1/cart/items", { method: "DELETE" });
        },
        favorites: function () {
            return request("/api/v1/favorites", { method: "GET" });
        },
        addFavorite: function (productId) {
            return request("/api/v1/favorites/" + encodeURIComponent(productId), {
                method: "PUT"
            });
        },
        removeFavorite: function (productId) {
            return request("/api/v1/favorites/" + encodeURIComponent(productId), {
                method: "DELETE"
            });
        },
        checkout: function (checkoutRequest) {
            return request("/api/v1/checkout", {
                method: "POST",
                body: checkoutRequest
            });
        },
        capturePaypal: function (externalOrderId) {
            return request("/api/v1/checkout/paypal/capture", {
                method: "POST",
                body: { externalOrderId: externalOrderId }
            });
        },
        cancelPaypal: function (externalOrderId) {
            return request("/api/v1/checkout/paypal/cancel", {
                method: "POST",
                body: { externalOrderId: externalOrderId }
            });
        },
        orders: function (page, size) {
            return request(
                "/api/v1/orders?page=" + encodeURIComponent(page || 0)
                    + "&size=" + encodeURIComponent(size || 10),
                { method: "GET" }
            );
        },
        order: function (orderId) {
            return request("/api/v1/orders/" + encodeURIComponent(orderId), {
                method: "GET"
            });
        },
        cancelOrder: function (orderId) {
            return request(
                "/api/v1/orders/" + encodeURIComponent(orderId) + "/cancellation",
                { method: "PUT" }
            );
        },
        ApiError: BookshopApiError
    });
})(window);
