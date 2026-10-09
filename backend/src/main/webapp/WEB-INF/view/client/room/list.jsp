<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="Phòng - SmartHome" />
    <jsp:param name="nav" value="rooms" />
</jsp:include>

<div class="sh-page-head">
    <div>
        <h1>Phòng</h1>
        <p>Chọn một phòng để xem cảm biến và điều khiển thiết bị trong phòng đó.</p>
    </div>
</div>

<c:if test="${empty rooms}">
    <div class="sh-empty"><i class="bi bi-door-closed" aria-hidden="true"></i>Chưa có phòng nào. Quản trị viên có thể thêm phòng trong trang quản trị.</div>
</c:if>

<c:if test="${not empty rooms}">
    <div class="sh-grid">
        <c:forEach var="room" items="${rooms}">
            <a class="sh-card" href="/client/room/${room.id}">
                <div class="sh-card-head">
                    <span class="sh-icon"><i class="bi bi-door-open" aria-hidden="true"></i></span>
                    <i class="bi bi-chevron-right sh-muted" aria-hidden="true"></i>
                </div>
                <h2 class="sh-title"><c:out value="${room.name}" /></h2>
                <div class="d-flex gap-2 flex-wrap mt-3">
                    <span class="sh-badge info"><i class="bi bi-activity" aria-hidden="true"></i>
                        <c:out value="${empty room.sensors ? 0 : room.sensors.size()}" /> cảm biến</span>
                    <span class="sh-badge"><i class="bi bi-toggles" aria-hidden="true"></i>
                        <c:out value="${empty room.devices ? 0 : room.devices.size()}" /> thiết bị</span>
                </div>
            </a>
        </c:forEach>
    </div>
</c:if>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
