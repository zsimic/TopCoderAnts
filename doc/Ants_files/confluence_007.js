(function($) {
    /**
     * Creates a new hover popup
     *
     * @param items jQuery object - the items that trigger the display of this popup when the user mouses over.
     * @param identifier A unique identifier for this popup. This should be unique across all popups on the page and a valid CSS class.
     * @param url The URL to retrieve popup contents.
     * @param postProcess A function called after the popup contents are loaded.
     *                    `this` will be the popup jQuery object, and the first argument is the popup identifier.
     * @param options Custom options to change default behaviour. See AJS.contentHover.opts for default values and valid options.
     *
     * @return jQuery object - the popup that was created
     */
    AJS.contentHover = function(items, identifier, url, postProcess, options) {
        var opts = $.extend(false, AJS.contentHover.opts, options);
        var hideDelayTimer;
        var showTimer;
        var loadTimer;
        var beingShown = false;
        var shouldShow = false;
        var contentLoaded = false;
        var shouldLoadContent = false;
        var mousePosition;
        var initialMousePosition;
        $(opts.container).append($('<div id="content-hover-' + identifier + '" class="ajs-content-hover"><div class="contents"></div></div>'));
        var popup = $("#content-hover-" + identifier);
        var contents = popup.find(".contents");
        contents.css("width", opts.width + "px");
        contents.mouseover(function() {
            clearTimeout(hideDelayTimer);
            popup.unbind("mouseover");
        }).mouseout(function() {
            hidePopup();
        });

        var showPopup = function() {
            if (popup.is(":visible")) {
                return;
            }

            showTimer = setTimeout(function() {
                if (!contentLoaded || !shouldShow) {
                    return;
                }
                beingShown = true;
                var posx = mousePosition.x - 3;
                var posy = mousePosition.y + 15;

                if (posx + opts.width + 30 > $(window).width()) {
                    popup.css({
                        right: "20px",
                        left: "auto"
                    });
                }
                else {
                    popup.css({
                        left: posx + "px",
                        right: "auto"
                    });
                }

                var bottomOfViewablePage = (window.pageYOffset || document.documentElement.scrollTop) + $(window).height();
                if ((posy + popup.height()) > bottomOfViewablePage) {
                    posy = bottomOfViewablePage - popup.height() - 5;
                    popup.mouseover(function() {
                        clearTimeout(hideDelayTimer);
                    }).mouseout(function() {
                        hidePopup();
                    });
                }
                popup.css({
                    top: posy + "px"
                });

                var shadow = $("#content-hover-shadow").appendTo(popup).show();
                // reset position of popup box
                popup.fadeIn(opts.fadeTime, function() {
                    // once the animation is complete, set the tracker variables
                    beingShown = false;
                });

                shadow.css({
                    width: contents.outerWidth() + 32 + "px",
                    height: contents.outerHeight() + 25 + "px"
                });
                $(".b", shadow).css("width", contents.outerWidth() - 26 + "px");
                $(".l, .r", shadow).css("height", contents.outerHeight() - 21 + "px");
            }, opts.showDelay);
        };

        var hidePopup = function() {
            beingShown = false;
            shouldShow = false;
            clearTimeout(hideDelayTimer);
            clearTimeout(showTimer);
            clearTimeout(loadTimer);
            contentLoading = false;
            shouldLoadContent = false;
            // store the timer so that it can be cleared in the mouseover if required
            hideDelayTimer = setTimeout(function() {
                popup.fadeOut(opts.fadeTime);
            }, opts.hideDelay);
        };

        var contentLoading = false;
        $(items).mousemove(function(e) {
            mousePosition = { x: e.pageX, y: e.pageY };
            
            if (!beingShown) {
                clearTimeout(showTimer);
            }
            shouldShow = true;
            // lazy load popup contents
            if (!contentLoaded) {
                if (contentLoading) {
                    // If the mouse has moved more than the threshold don't load the contents
                    if (shouldLoadContent) {
                        var distance = (mousePosition.x - initialMousePosition.x)*(mousePosition.x - initialMousePosition.x)
                                + (mousePosition.y - initialMousePosition.y) * (mousePosition.y - initialMousePosition.y);
                        if (distance > (opts.mouseMoveThreshold * opts.mouseMoveThreshold)) {
                            contentLoading = false;
                            shouldLoadContent = false;
                            clearTimeout(loadTimer);
                            return;
                        }
                    }
                } else {
                    // Save the position the mouse started from
                    initialMousePosition = mousePosition;
                    shouldLoadContent = true;
                    contentLoading = true;
                    loadTimer = setTimeout(function () {
                        if (!shouldLoadContent)
                            return;

                        contents.load(url, function() {
                            contentLoaded = true;
                            contentLoading = false;
                            postProcess.call(popup, identifier);
                            showPopup();
                        });
                    }, opts.loadDelay);
                }
            }
            // stops the hide event if we move from the trigger to the popup element
            clearTimeout(hideDelayTimer);
            // don't trigger the animation again if we're being shown
            if (!beingShown) {
                showPopup();
            }
        }).mouseout(function() {
            hidePopup();
        });

        contents.click(function(e) {
            e.stopPropagation();
        });

        $("body").click(function() {
            beingShown = false;
            clearTimeout(hideDelayTimer);
            clearTimeout(showTimer);
            popup.hide();
        });

        return popup;
    };

    AJS.contentHover.opts = {
        fadeTime: 100,
        hideDelay: 500,
        showDelay: 700,
        loadDelay: 50,
        width: 300,
        mouseMoveThreshold: 10,
        container: "body"
    };

    AJS.toInit(function(){
        $("body").append($('<div id="content-hover-shadow"><div class="tl"></div><div class="tr"></div><div class="l"></div><div class="r"></div><div class="bl"></div><div class="br"></div><div class="b"></div></div>'));
        $("#content-hover-shadow").hide();
    });
})(jQuery);

if (typeof AJS.followCallbacks == "undefined") AJS.followCallbacks = [];
if (typeof AJS.favouriteCallbacks == "undefined") AJS.favouriteCallbacks = [];

// if you want to customize the behaviour of the follow button you can add callbacks to the above array
// by adding this code to your javascript page:
//
// if (typeof AJS.followCallbacks == "undefined") AJS.followCallbacks = [];
// AJS.followCallbacks.push(function(user) {
//    alert('favourite added'+user');
// });
//
// these callbacks are called after the post to the server has completed.
//
// You can add to the followCallbacks or the favouriteCallbacks if you want callbacks on the follow functions
// or the favourite functions respectively.

// Confluence specific code
AJS.toInit(function($) {

    var postProcess  = function(id) {
        var username = users[id];
        $(".ajs-menu-bar", this).ajsMenu();
        $(".follow-icon, .unfollow-icon", this).each(function() {
            var $this = $(this).click(function(e) {
                if ($this.hasClass("waiting")) {
                    return;
                }
                var url = $this.hasClass("unfollow-icon") ? "/unfollowuser.action" : "/followuser.action";
                $this.addClass("waiting");
                AJS.safe.post(contextPath + url, { username: username, mode: "blank" }, function() {
                    $this.removeClass("waiting");
                    $this.parent().toggleClass("follow-item").toggleClass("unfollow-item");
                    $.each(AJS.followCallbacks, function() {
                        this(username);
                    });
                });
                return AJS.stopEvent(e);
            });
        });

    };

    var users = [];
    $(".confluence-userlink, .userLogoLink").each(function() {
        var userlink = $(this);
        var matched = /username:([^ ]*)/.exec(userlink.attr("class"));
        if (matched == null) {
            return;
        }
        var username = matched[1];
        userlink.attr("title", "");
        $("img", userlink).attr("title", "");
        var arrayIndex = $.inArray(username, users);
        if (arrayIndex == -1) {
            users.push(username);
            arrayIndex = $.inArray(username, users);
        }

        $(this).addClass("userlink-" + arrayIndex);
    });

    $.each(users, function(i) {
        AJS.contentHover($(".userlink-" + i), i, contextPath + "/users/userinfopopup.action?username=" + users[i], postProcess);
    });
});