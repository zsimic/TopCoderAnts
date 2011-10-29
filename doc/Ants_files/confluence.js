AJS.toInit(function($) {
    var hideSidebarCookie = "com.atlassian.confluence.sidebar.hide",
        sidebar = $("#personal-info-sidebar"),
        height = sidebar.height(),
        content = $("#content");

    function toggleSidebar() {
        sidebar.toggleClass("collapsed");
        content.toggleClass("sidebar-collapsed");
    }

    if (getCookie(hideSidebarCookie) == "true") {
        toggleSidebar();
    }

    $(".sidebar-collapse").click(function(e) {
        toggleSidebar();
        setCookie(hideSidebarCookie, sidebar.hasClass("collapsed"));
        return AJS.stopEvent(e);
    }).height(height);
    // sidebar.height(height);
});