(function (global) {
    "use strict";

    function api() {
        return global.bookshopApi;
    }

    function value(form, name) {
        var field = form.querySelector("[name='" + name + "']");
        return field ? field.value : "";
    }

    function booleanValue(form, name) {
        return value(form, name).toLowerCase() === "true";
    }

    function jsonMultipart(payload, fileInput) {
        var data = new FormData();
        data.append(
            "data",
            new Blob([JSON.stringify(payload)], { type: "application/json" })
        );
        if (fileInput && fileInput.files && fileInput.files.length > 0) {
            data.append("file", fileInput.files[0]);
        }
        return data;
    }

    function errorMessage(error) {
        var details = error && error.details;
        var fields = details && Array.isArray(details.fieldErrors)
            ? details.fieldErrors
            : [];
        if (fields.length > 0) {
            return fields.map(function (field) {
                return field.field + ": " + field.message;
            }).join("\n");
        }
        return error && error.message
            ? error.message
            : "Không thể xử lý yêu cầu quản trị";
    }

    function handleError(error) {
        if (error && error.status === 401) {
            global.location.assign("/login");
            return;
        }
        global.alert(errorMessage(error));
    }

    async function submitThroughApi(event, operation) {
        event.preventDefault();
        var form = event.currentTarget;
        if (form.checkValidity && !form.checkValidity()) {
            form.classList.add("was-validated");
            return;
        }

        var button = form.querySelector("[type='submit']");
        if (button) {
            button.disabled = true;
        }
        try {
            await operation(form);
        } catch (error) {
            handleError(error);
            if (button) {
                button.disabled = false;
            }
        }
    }

    function bindUserAccess() {
        document.querySelectorAll("[data-admin-user-access-form]")
            .forEach(function (form) {
                form.addEventListener("submit", function (event) {
                    submitThroughApi(event, async function (currentForm) {
                        var roleIds = Array.from(currentForm.querySelectorAll(
                            "input[name='roleIds']:checked"
                        )).map(function (input) {
                            return Number(input.value);
                        });
                        await api().put(
                            "/api/v1/admin/users/"
                                + encodeURIComponent(currentForm.dataset.userId)
                                + "/access",
                            {
                                enabled: currentForm.querySelector(
                                    "input[name='enabled']"
                                ).checked,
                                roleIds: roleIds
                            }
                        );
                        global.location.assign("/admin/customers?updated=1");
                    });
                });
            });
    }

    function categoryPayload(form) {
        return {
            name: value(form, "categoryName"),
            active: booleanValue(form, "status")
        };
    }

    function bindCategories() {
        document.querySelectorAll("[data-admin-category-create]")
            .forEach(function (form) {
                form.addEventListener("submit", function (event) {
                    submitThroughApi(event, async function (currentForm) {
                        await api().post(
                            "/api/v1/admin/categories",
                            jsonMultipart(
                                categoryPayload(currentForm),
                                currentForm.querySelector("input[name='file']")
                            )
                        );
                        global.location.assign("/admin/categories");
                    });
                });
            });

        document.querySelectorAll("[data-admin-category-update]")
            .forEach(function (form) {
                form.addEventListener("submit", function (event) {
                    submitThroughApi(event, async function (currentForm) {
                        await api().put(
                            "/api/v1/admin/categories/"
                                + encodeURIComponent(currentForm.dataset.categoryId),
                            jsonMultipart(
                                categoryPayload(currentForm),
                                currentForm.querySelector("input[name='file']")
                            )
                        );
                        global.location.assign("/admin/categories");
                    });
                });
            });

        document.querySelectorAll("[data-admin-category-visibility]")
            .forEach(function (form) {
                form.addEventListener("submit", function (event) {
                    submitThroughApi(event, async function (currentForm) {
                        await api().put(
                            "/api/v1/admin/categories/"
                                + encodeURIComponent(currentForm.dataset.categoryId)
                                + "/visibility",
                            { active: currentForm.dataset.active === "true" }
                        );
                        global.location.reload();
                    });
                });
            });
    }

    function publisherPayload(form) {
        return {
            name: value(form, "name"),
            active: booleanValue(form, "status")
        };
    }

    function bindPublishers() {
        document.querySelectorAll("[data-admin-publisher-create]")
            .forEach(function (form) {
                form.addEventListener("submit", function (event) {
                    submitThroughApi(event, async function (currentForm) {
                        await api().post(
                            "/api/v1/admin/publishers",
                            publisherPayload(currentForm)
                        );
                        global.location.assign("/admin/nxbs");
                    });
                });
            });

        document.querySelectorAll("[data-admin-publisher-update]")
            .forEach(function (form) {
                form.addEventListener("submit", function (event) {
                    submitThroughApi(event, async function (currentForm) {
                        await api().put(
                            "/api/v1/admin/publishers/"
                                + encodeURIComponent(currentForm.dataset.publisherId),
                            publisherPayload(currentForm)
                        );
                        global.location.assign("/admin/nxbs");
                    });
                });
            });

        document.querySelectorAll("[data-admin-publisher-visibility]")
            .forEach(function (form) {
                form.addEventListener("submit", function (event) {
                    submitThroughApi(event, async function (currentForm) {
                        await api().put(
                            "/api/v1/admin/publishers/"
                                + encodeURIComponent(currentForm.dataset.publisherId)
                                + "/visibility",
                            { active: currentForm.dataset.active === "true" }
                        );
                        global.location.reload();
                    });
                });
            });
    }

    function currency(value) {
        return new Intl.NumberFormat("vi-VN", {
            maximumFractionDigits: 0
        }).format(Number(value || 0)) + " đ";
    }

    function reportCells(row, index) {
        return [
            index + 1,
            row.group,
            row.quantity,
            currency(row.revenue),
            currency(row.averagePrice),
            currency(row.minimumPrice),
            currency(row.maximumPrice)
        ];
    }

    async function refreshReport() {
        var table = document.querySelector("[data-report-grouping]");
        if (!table) {
            return;
        }
        try {
            var result = await api().get(
                "/api/v1/admin/reports?groupBy="
                    + encodeURIComponent(table.dataset.reportGrouping)
            );
            var rows = result.rows || [];
            if (global.jQuery
                && global.jQuery.fn.DataTable
                && global.jQuery.fn.DataTable.isDataTable(table)) {
                var dataTable = global.jQuery(table).DataTable();
                dataTable.clear();
                rows.forEach(function (row, index) {
                    dataTable.row.add(reportCells(row, index));
                });
                dataTable.draw();
                return;
            }

            var body = table.querySelector("tbody");
            body.innerHTML = "";
            rows.forEach(function (row, index) {
                var tr = document.createElement("tr");
                reportCells(row, index).forEach(function (cell) {
                    var td = document.createElement("td");
                    td.textContent = cell;
                    tr.appendChild(td);
                });
                body.appendChild(tr);
            });
        } catch (error) {
            handleError(error);
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        if (!api()) {
            return;
        }
        bindUserAccess();
        bindCategories();
        bindPublishers();
        refreshReport();
    });
})(window);
