<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Thẻ thiết bị dùng chung. Trước khi include: <c:set var="shDevice" value="${device}" scope="request"/>; cần isLoggedIn trong model. --%>
<jsp:include page="/WEB-INF/view/client/layout/device-icon.jsp">
    <jsp:param name="name" value="${shDevice.name}" />
</jsp:include>
<c:set var="on" value="${shDevice.status == 'ON'}" />
<div class="sh-card sh-device ${on ? 'on' : ''}" data-device-card="${shDevice.id}">
    <div class="sh-card-head">
        <span class="sh-icon ${on ? 'ok' : 'off'}"><i class="bi ${shDevIcon}" aria-hidden="true"></i></span>
        <c:choose>
            <c:when test="${isLoggedIn}">
                <button type="button" class="sh-switch ${on ? 'on' : ''}" role="switch" aria-checked="${on}"
                        aria-label="Bật/tắt <c:out value='${shDevice.name}'/>" data-device-switch="${shDevice.id}"></button>
            </c:when>
            <c:otherwise>
                <button type="button" class="sh-switch ${on ? 'on' : ''}" role="switch" aria-checked="${on}" disabled
                        aria-label="<c:out value='${shDevice.name}'/> (đăng nhập để điều khiển)" title="Cần đăng nhập để điều khiển"></button>
            </c:otherwise>
        </c:choose>
    </div>
    <h3 class="sh-title"><c:out value="${shDevice.name}" /></h3>
    <div class="d-flex justify-content-between align-items-center gap-2 mt-2 flex-wrap">
        <span class="sh-muted"><i class="bi bi-door-open" aria-hidden="true"></i>
            <c:out value="${shDevice.room != null ? shDevice.room.name : 'Chưa gán phòng'}" /></span>
        <span class="sh-badge ${on ? 'ok' : ''}" data-device-status="${shDevice.id}"><i class="bi bi-power" aria-hidden="true"></i>${on ? 'Đang bật' : 'Đang tắt'}</span>
    </div>
</div>
