(function () {
    var originalAjax = jQuery.ajax;

    AJS.safe = {
        ajax: function (options) {
            if (options.data && typeof options.data == "object") {
                options.data.atl_token = jQuery("#atlassian-token").attr("content");
                return originalAjax(options);
            }
        },

        get: function () {
            jQuery.ajax = AJS.safe.ajax;
            try {
                return jQuery.get.apply(jQuery, arguments);
            } finally {
                jQuery.ajax = originalAjax;
            }
        },

        getScript: function(url, callback) {
            return AJS.safe.get(url, null, callback, "script");
        },

        getJSON: function(url, data, callback) {
            return AJS.safe.get(url, data, callback, "json");
        },

        post: function(url, data, callback, type) {
            jQuery.ajax = AJS.safe.ajax;
            try {
                return jQuery.post.apply(jQuery, arguments);
            } finally {
                jQuery.ajax = originalAjax;
            }
        }
    };
})();

// DEPRECATED: Use AJS.safe.ajax() directly
AJS.safeAjax = function(options) {
    return AJS.safe.ajax(options);
};
