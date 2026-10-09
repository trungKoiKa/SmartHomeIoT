<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Thẻ cảm biến dùng chung. Trước khi include: <c:set var="shSensor" value="${sensor}" scope="request"/> --%>
<jsp:include page="/WEB-INF/view/client/layout/sensor-meta.jsp">
    <jsp:param name="type" value="${shSensor.type}" />
</jsp:include>
<c:set var="last" value="${not empty shSensor.latestData ? shSensor.latestData[0] : null}" />
<c:set var="active" value="${shSensor.status == 'ACTIVE'}" />
<a class="sh-card" href="/client/sensor/${shSensor.id}">
    <div class="sh-card-head">
        <span class="sh-icon ${last != null and last.alert ? 'danger' : (active ? '' : 'off')}"><i class="bi ${shIcon}" aria-hidden="true"></i></span>
        <c:choose>
            <c:when test="${last != null and last.alert}">
                <span class="sh-badge danger"><i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>Cảnh báo</span>
            </c:when>
            <c:when test="${not active}">
                <span class="sh-badge"><i class="bi bi-pause-circle" aria-hidden="true"></i>Đang tắt</span>
            </c:when>
            <c:when test="${last == null}">
                <span class="sh-badge warn"><i class="bi bi-hourglass-split" aria-hidden="true"></i>Chưa có dữ liệu</span>
            </c:when>
            <c:otherwise>
                <span class="sh-badge ok"><i class="bi bi-check-circle-fill" aria-hidden="true"></i>Bình thường</span>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="sh-label"><c:out value="${shKind}" /> &middot; <c:out value="${shSensor.name}" /></div>
    <div class="mt-1">
        <c:choose>
            <c:when test="${last != null and last.value != null}">
                <span class="sh-value"><fmt:formatNumber value="${last.value}" maxFractionDigits="1" /></span><span class="sh-unit"><c:out value="${shUnit}" /></span>
            </c:when>
            <c:otherwise><span class="sh-value">--</span></c:otherwise>
        </c:choose>
    </div>
    <div class="sh-muted mt-2">
        <i class="bi bi-door-open" aria-hidden="true"></i> <c:out value="${shSensor.room != null ? shSensor.room.name : 'Chưa gán phòng'}" />
        <c:if test="${last != null}"> &middot; <c:out value="${last.formattedRecordedAt}" /></c:if>
    </div>
</a>
