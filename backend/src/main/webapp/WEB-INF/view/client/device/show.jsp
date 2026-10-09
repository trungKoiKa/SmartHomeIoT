<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="Thiết bị - SmartHome" />
    <jsp:param name="nav" value="devices" />
    <jsp:param name="live" value="5" />
</jsp:include>

<div class="sh-page-head">
    <div>
        <h1>Thiết bị</h1>
        <p>Bật/tắt thiết bị theo từng phòng. Trạng thái được cập nhật từ gateway.</p>
    </div>
    <div class="sh-live" id="live-indicator">
        <span class="sh-live-dot" aria-hidden="true"></span>
        <span id="live-status">Đang chờ cập nhật…</span>
        <button type="button" id="live-toggle" aria-pressed="false">Tạm dừng</button>
    </div>
</div>

<c:if test="${isLoggedIn}">
    <div class="sh-card mb-4">
        <div class="sh-voice">
            <button type="button" id="voice-btn" class="sh-btn ghost sh-voice-btn" aria-pressed="false">
                <i class="bi bi-mic-fill" aria-hidden="true"></i> <span id="voice-label">Điều khiển bằng giọng nói</span>
            </button>
            <span id="voice-hint" class="sh-muted" aria-live="polite">Ví dụ: "bật đèn 1", "tắt quạt", "tắt tất cả".</span>
        </div>
    </div>
</c:if>
<c:if test="${not isLoggedIn}">
    <div class="sh-notice" role="note">
        <i class="bi bi-info-circle-fill" aria-hidden="true"></i>
        <span>Bạn đang xem ở chế độ khách. <a href="/login">Đăng nhập</a> để điều khiển thiết bị.</span>
    </div>
</c:if>

<c:if test="${empty devices}">
    <div class="sh-empty"><i class="bi bi-plug" aria-hidden="true"></i>Chưa có thiết bị nào trong hệ thống.</div>
</c:if>

<c:if test="${not empty devices}">
    <div class="sh-chips" role="group" aria-label="Lọc theo phòng">
        <button type="button" class="sh-chip" data-room-filter="all" aria-pressed="true">Tất cả</button>
        <c:forEach var="room" items="${rooms}">
            <button type="button" class="sh-chip" data-room-filter="${room.id}" aria-pressed="false"><c:out value="${room.name}" /></button>
        </c:forEach>
    </div>

    <div class="sh-grid" id="device-grid" data-live-region>
        <c:forEach var="device" items="${devices}">
            <div data-room="${device.room != null ? device.room.id : 'none'}">
                <c:set var="shDevice" value="${device}" scope="request" />
                <jsp:include page="/WEB-INF/view/client/layout/device-card.jsp" />
            </div>
        </c:forEach>
    </div>
</c:if>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
