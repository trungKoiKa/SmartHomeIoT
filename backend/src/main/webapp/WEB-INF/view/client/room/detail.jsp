<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="${room.name} - SmartHome" />
    <jsp:param name="nav" value="rooms" />
    <jsp:param name="live" value="5" />
</jsp:include>

<div class="sh-page-head">
    <div>
        <div class="sh-breadcrumb"><a href="/client/room-list">Phòng</a> / <c:out value="${room.name}" /></div>
        <h1><c:out value="${room.name}" /></h1>
        <p><c:out value="${deviceCount}" /> thiết bị &middot; <c:out value="${empty room.sensors ? 0 : room.sensors.size()}" /> cảm biến</p>
    </div>
    <div class="sh-live" id="live-indicator">
        <span class="sh-live-dot" aria-hidden="true"></span>
        <span id="live-status">Đang chờ cập nhật…</span>
        <button type="button" id="live-toggle" aria-pressed="false">Tạm dừng</button>
    </div>
</div>

<c:if test="${not isLoggedIn}">
    <div class="sh-notice" role="note">
        <i class="bi bi-info-circle-fill" aria-hidden="true"></i>
        <span>Bạn đang xem ở chế độ khách. <a href="/login">Đăng nhập</a> để điều khiển thiết bị.</span>
    </div>
</c:if>

<div id="room-live" data-live-region>
    <h2 class="sh-section-title">Thiết bị</h2>
    <c:if test="${empty devices}">
        <div class="sh-empty"><i class="bi bi-plug" aria-hidden="true"></i>Phòng này chưa có thiết bị.</div>
    </c:if>
    <c:if test="${not empty devices}">
        <div class="sh-grid">
            <c:forEach var="device" items="${devices}">
                <c:set var="shDevice" value="${device}" scope="request" />
                <jsp:include page="/WEB-INF/view/client/layout/device-card.jsp" />
            </c:forEach>
        </div>
    </c:if>

    <h2 class="sh-section-title">Cảm biến</h2>
    <c:if test="${empty room.sensors}">
        <div class="sh-empty"><i class="bi bi-activity" aria-hidden="true"></i>Phòng này chưa có cảm biến.</div>
    </c:if>
    <c:if test="${not empty room.sensors}">
        <div class="sh-grid">
            <c:forEach var="sensor" items="${room.sensors}">
                <c:set var="shSensor" value="${sensor}" scope="request" />
                <jsp:include page="/WEB-INF/view/client/layout/sensor-card.jsp" />
            </c:forEach>
        </div>
    </c:if>
</div>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
