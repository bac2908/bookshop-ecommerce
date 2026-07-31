(function (global, document) {
    "use strict";

    var api = global.bookshopApi;
    if (!api) {
        return;
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
            : "Không thể hoàn tất yêu cầu. Vui lòng thử lại.");
    }

    function disableSubmit(form, disabled) {
        form.dataset.apiPending = disabled ? "true" : "false";
        var button = form.querySelector("[type='submit']");
        if (button) {
            button.disabled = disabled;
        }
    }

    async function submitCheckout(form) {
        var data = new FormData(form);
        var result = await api.checkout({
            address: data.get("address"),
            phone: data.get("phone"),
            note: data.get("note") || null,
            paymentMethod: data.get("paymentMethod")
        });

        if (result.paymentMethod === "PAYPAL" && result.approvalUrl) {
            global.location.assign(result.approvalUrl);
            return;
        }
        global.location.assign(
            "/checkout_success?orderId=" + encodeURIComponent(result.orderId)
        );
    }

    document.addEventListener("submit", async function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        var isCheckout = form.hasAttribute("data-api-checkout");
        var isCancellation = form.hasAttribute("data-api-order-cancel");
        if (!isCheckout && !isCancellation) {
            return;
        }

        event.preventDefault();
        if (form.dataset.apiPending === "true") {
            return;
        }
        disableSubmit(form, true);

        try {
            if (isCheckout) {
                await submitCheckout(form);
            } else {
                await api.cancelOrder(form.dataset.orderId);
                global.location.reload();
            }
        } catch (error) {
            showError(error);
            disableSubmit(form, false);
        }
    }, true);

    async function handlePaypalReturn() {
        var element = document.querySelector("[data-paypal-return]");
        if (!element || element.dataset.apiPending === "true") {
            return;
        }
        element.dataset.apiPending = "true";
        var action = element.dataset.action;
        var externalOrderId = element.dataset.token;

        try {
            var result = action === "capture"
                ? await api.capturePaypal(externalOrderId)
                : await api.cancelPaypal(externalOrderId);
            if (action === "capture") {
                global.location.assign(
                    "/checkout_paypal_success?orderId="
                        + encodeURIComponent(result.orderId)
                );
            } else {
                global.location.assign("/payment-cancel");
            }
        } catch (error) {
            showError(error);
            element.dataset.apiPending = "false";
        }
    }

    handlePaypalReturn();
})(window, document);
