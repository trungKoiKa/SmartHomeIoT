<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Suy ra đơn vị / icon / nhãn từ Sensor.type (chuỗi tự do). Dùng: jsp:include với param "type"; kết quả ở request scope shUnit, shIcon, shKind. --%>
<c:set var="t" value="${fn:toLowerCase(param.type)}" />
<c:choose>
    <c:when test="${fn:contains(t, 'temp') or fn:contains(t, 'nhiệt') or fn:contains(t, 'nhiet')}">
        <c:set var="shUnit" value="°C" scope="request" /><c:set var="shIcon" value="bi-thermometer-half" scope="request" /><c:set var="shKind" value="Nhiệt độ" scope="request" />
    </c:when>
    <c:when test="${fn:contains(t, 'humi') or fn:contains(t, 'ẩm') or fn:contains(t, 'am')}">
        <c:set var="shUnit" value="%" scope="request" /><c:set var="shIcon" value="bi-droplet-half" scope="request" /><c:set var="shKind" value="Độ ẩm" scope="request" />
    </c:when>
    <c:when test="${fn:contains(t, 'gas') or fn:contains(t, 'khí') or fn:contains(t, 'khi')}">
        <c:set var="shUnit" value="%" scope="request" /><c:set var="shIcon" value="bi-wind" scope="request" /><c:set var="shKind" value="Khí gas" scope="request" />
    </c:when>
    <c:when test="${fn:contains(t, 'light') or fn:contains(t, 'sáng') or fn:contains(t, 'sang')}">
        <c:set var="shUnit" value="%" scope="request" /><c:set var="shIcon" value="bi-brightness-high" scope="request" /><c:set var="shKind" value="Ánh sáng" scope="request" />
    </c:when>
    <c:otherwise>
        <c:set var="shUnit" value="" scope="request" /><c:set var="shIcon" value="bi-cpu" scope="request" /><c:set var="shKind" value="Cảm biến" scope="request" />
    </c:otherwise>
</c:choose>
