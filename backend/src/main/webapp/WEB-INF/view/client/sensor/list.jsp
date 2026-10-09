<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="Cảm biến - SmartHome" />
    <jsp:param name="nav" value="sensors" />
    <jsp:param name="live" value="5" />
</jsp:include>

<div class="sh-page-head">
    <div>
        <h1>Cảm biến</h1>
        <p>Thông số môi trường mới nhất trong nhà bạn.</p>
    </div>
    <div class="sh-live" id="live-indicator">
        <span class="sh-live-dot" aria-hidden="true"></span>
        <span id="live-status">Đang chờ cập nhật…</span>
        <button type="button" id="live-toggle" aria-pressed="false">Tạm dừng</button>
    </div>
</div>

<div id="sensor-grid" data-live-region>
    <c:if test="${empty sensors}">
        <div class="sh-empty"><i class="bi bi-cpu" aria-hidden="true"></i>
            Chưa có cảm biến nào. Hãy kiểm tra kết nối phần cứng hoặc thêm cảm biến trong trang quản trị.</div>
    </c:if>
    <c:if test="${not empty sensors}">
        <div class="sh-grid">
            <c:forEach var="sensor" items="${sensors}">
                <c:set var="shSensor" value="${sensor}" scope="request" />
                <jsp:include page="/WEB-INF/view/client/layout/sensor-card.jsp" />
            </c:forEach>
        </div>
    </c:if>
</div>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
