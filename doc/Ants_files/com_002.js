if (!/jwebunit/.test(navigator.userAgent.toLowerCase())) {
AJS.dropDown = function (obj, isVisibleByDefault) {
    var dd = null,
        result = [],
        $doc = AJS.$(document);

    if (obj && obj.jquery) { // if jQuery
        dd = obj;
    } else if (typeof obj == "string") { // if jQuery selector
        dd = AJS.$(obj);
    } else if (obj && obj.constructor == Array) { // if JSON
        dd = AJS("ul").attr("class", (isVisibleByDefault ? "hidden" : "") + "ajs-drop-down");
        for (var i = 0, ii = obj.length; i < ii; i++) {
            var ol = AJS("ol");
            for (var j = 0, jj = obj[i].length; j < jj; j++) {
                var li = AJS("li");
                
                if (obj[i][j].href) {
                	li.append(AJS("a")
                        .html("<span>" + obj[i][j].name + "</span>")
                        .attr({href:  obj[i][j].href})
                        .addClass(obj[i][j].className));
                    
                    var properties = obj[i][j];
                    AJS.$.data(AJS.$("a > span", li)[0], "properties", properties);
                } else {
                    li.html(obj[i][j].html).addClass(obj[i][j].className);
                }
                if (obj[i][j].icon) {
                    li.prepend(AJS("img").attr("src", obj[i][j].icon));
                }
                ol.append(li);
            }
            if (i == ii - 1) {
                ol.addClass("last");
            }
            dd.append(ol);
        }
        AJS.$("body").append(dd);
    } else {
        throw new Error("AJS.dropDown function was called with illegal parameter. Should be jQuery object, jQuery selector or array.");
    }

    var blocker = function (e) {
        var c = e.which;
        if (c == 40 || c == 38) {
            e.stopPropagation();
            e.preventDefault();
            return false;
        }
    };
    var movefocus = function (e) {
        if (!AJS.dropDown.current) {
            return true;
        }
        var c = e.which,
            cdd = AJS.dropDown.current.$[0],
            focus = (typeof cdd.focused == "number" ? cdd.focused : -1);
        AJS.dropDown.current.cleanFocus();
        cdd.focused = focus;
        switch (c) {
            case 40:
            case 9: {
                cdd.focused++;
                break;
            }
            case 38: {
                cdd.focused--;
                break;
            }
            case 27: {
                AJS.dropDown.current.hide("escape");
                return false;
            }
            default: {
                return true;
            }
        }
        if (cdd.focused < 0) {
            cdd.focused = AJS.dropDown.current.links.length - 1;
        }
        if (cdd.focused > AJS.dropDown.current.links.length - 1) {
            cdd.focused = 0;
        }
        if (AJS.dropDown.current.links.length) {
        	AJS.dropDown.current.links[cdd.focused].focus();
        	AJS.dropDown.current.links[cdd.focused].pa.addClass("active");
        }
        e.stopPropagation();
        e.preventDefault();
        return false;
    };
    var hider = function (e) {
        if (!((e && e.which && (e.which == 3)) || (e && e.button && (e.button == 2)) || false)) { // right click check
            AJS.dropDown.current && AJS.dropDown.current.hide("click");
        }
    };
    var active = function (i) {
        return function (e) {
            var pa = this.parentNode;
            AJS.dropDown.current.cleanFocus();
            pa.originalClass = pa.className;
            pa.className = "active";
            AJS.dropDown.current.$[0].focused = i;
        };
    };

    dd.each(function () {
        var cdd = this,
            $cdd = AJS.$(this),
            res = {
                $: $cdd,
                links: AJS.$("a", this),
                cleanFocus: function () {
                    if (cdd.focused + 1 && res.links.length) {
                        var pa = res.links[cdd.focused].pa[0];
                        pa.className = "";
                    }
                    cdd.focused = -1;
                }
            };

        res.links.each(function (i) {
            var pa = AJS.$(this.parentNode);
            this.pa = pa;
            AJS.$(this).hover(active(i), res.cleanFocus);
        });
        var methods = {
            appear: function (dir) {
                if (dir) {
                    $cdd.removeClass("hidden");
                } else {
                    $cdd.addClass("hidden");
                }
            },
            fade: function (dir) {
                if (dir) {
                    $cdd.fadeIn("fast");
                } else {
                    $cdd.fadeOut("fast");
                }
            },
            scroll: function (dir) {
                if (dir) {
                    $cdd.slideDown("fast");
                } else {
                    $cdd.slideUp("fast");
                }
            }
        };
        res.show = function (method) {
                hider();
                AJS.dropDown.current = this;
                this.method = method || this.method || "appear";
                methods[this.method](true);
                this.timer = setTimeout(function () {
                    $doc.click(hider);
                }, 0);
                $doc.keydown(movefocus).keypress(blocker);
                this.onshow();
            };
        res.hide = function (causer) {
                this.method = this.method || "appear";
                this.cleanFocus();
                methods[this.method](false);
                $doc.unbind("click", hider).unbind("keydown", movefocus).unbind("keypress", blocker);
                this.onhide(causer);
                AJS.dropDown.current = null;
            };
        res.onshow = res.onhide = function () {};
        if ($cdd.hasClass("hidden")) {
            res.hide();
        } else {
            res.show();
        }
        result.push(res);
    });
    return result;
};

// for each item in the drop down get the value of the named additional property. If there is no
// property with the specified name then null will be returned.
AJS.dropDown.getAdditionalPropertyValue = function (item, name) {
    var properties = AJS.$.data(item[0], "properties");
    if (typeof properties != "undefined") {
        return properties[name];
    }
    else {
        return null;
    }
};

// remove all additional properties
AJS.dropDown.removeAllAdditionalProperties = function(item) {
	// only here for backwards compatibility
};
}