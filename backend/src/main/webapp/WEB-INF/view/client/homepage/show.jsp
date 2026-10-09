<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="Trang chủ - SmartHome" />
    <jsp:param name="nav" value="home" />
    <jsp:param name="live" value="5" />
</jsp:include>

<div class="sh-page-head">
    <div>
        <h1>
            <c:choose>
                <c:when test="${not empty sessionScope.fullName}">Xin chào, <c:out value="${sessionScope.fullName}" /></c:when>
                <c:otherwise>Nhà của bạn</c:otherwise>
            </c:choose>
        </h1>
        <p>Tổng quan cảm biến và thiết bị trong nhà.</p>
    </div>
    <div class="sh-live" id="live-indicator">
        <span class="sh-live-dot" aria-hidden="true"></span>
        <span id="live-status">Đang tải…</span>
        <button type="button" id="live-toggle" aria-pressed="false">Tạm dừng</button>
    </div>
</div>

<c:if test="${empty pageContext.request.userPrincipal}">
    <div class="sh-notice info" role="note">
        <i class="bi bi-info-circle-fill" aria-hidden="true"></i>
        <span>Bạn đang xem ở chế độ khách. <a href="/login">Đăng nhập</a> để điều khiển thiết bị.</span>
    </div>
</c:if>

<%-- Nội dung lấy từ trang cảm biến/thiết bị qua live.js (không cần thêm endpoint) --%>
<h2 class="sh-section-title">Cảm biến <a class="sh-muted" href="/client/sensor-list">Xem tất cả</a></h2>
<div id="home-sensors" data-live-region data-live-src="/client/sensor-list" data-live-from="sensor-grid">
    <div class="sh-skeleton" aria-hidden="true"></div>
</div>

<h2 class="sh-section-title">Thiết bị <a class="sh-muted" href="/client/device">Xem tất cả</a></h2>
<div id="home-devices" data-live-region data-live-src="/client/device" data-live-from="device-grid">
    <div class="sh-skeleton" aria-hidden="true"></div>
</div>

<h2 class="sh-section-title">Phòng <a class="sh-muted" href="/client/room-list">Xem tất cả</a></h2>
<div class="sh-grid">
    <a class="sh-card" href="/client/room-list">
        <div class="sh-card-head"><span class="sh-icon"><i class="bi bi-door-open" aria-hidden="true"></i></span></div>
        <h3 class="sh-title">Duyệt theo phòng</h3>
        <p class="sh-muted mb-0">Xem cảm biến và thiết bị của từng phòng.</p>
    </a>
</div>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
