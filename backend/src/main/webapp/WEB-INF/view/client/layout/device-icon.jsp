<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Device chưa có "loại", nên đoán icon từ tên. Kết quả: request scope shDevIcon. --%>
<c:set var="n" value="${fn:toLowerCase(param.name)}" />
<c:choose>
    <c:when test="${fn:contains(n, 'đèn') or fn:contains(n, 'den') or fn:contains(n, 'light') or fn:contains(n, 'lamp') or fn:contains(n, 'bóng')}">
        <c:set var="shDevIcon" value="bi-lightbulb" scope="request" />
    </c:when>
    <c:when test="${fn:contains(n, 'quạt') or fn:contains(n, 'quat') or fn:contains(n, 'fan')}">
        <c:set var="shDevIcon" value="bi-fan" scope="request" />
    </c:when>
    <c:otherwise><c:set var="shDevIcon" value="bi-plug" scope="request" /></c:otherwise>
</c:choose>
