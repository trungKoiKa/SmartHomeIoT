<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="Không có quyền truy cập - SmartHome" />
</jsp:include>

<div class="sh-auth">
    <div class="sh-card text-center">
        <span class="sh-icon danger mx-auto mb-3"><i class="bi bi-shield-lock-fill" aria-hidden="true"></i></span>
        <h1>Không có quyền truy cập</h1>
        <p class="sh-muted">Tài khoản của bạn không được phép xem trang này.</p>
        <a href="/" class="sh-btn"><i class="bi bi-house-door" aria-hidden="true"></i> Về trang chủ</a>
    </div>
</div>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
