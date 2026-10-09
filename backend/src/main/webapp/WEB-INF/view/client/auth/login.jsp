<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="Đăng nhập - SmartHome" />
</jsp:include>

<div class="sh-auth">
    <div class="sh-card">
        <h1>Đăng nhập</h1>
        <p class="sh-muted mb-4">Đăng nhập để điều khiển thiết bị trong nhà.</p>

        <c:if test="${param.error != null}">
            <div class="sh-notice danger" role="alert">
                <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i><span>Email hoặc mật khẩu không đúng. Vui lòng thử lại.</span>
            </div>
        </c:if>
        <c:if test="${param.logout != null}">
            <div class="sh-notice info" role="status">
                <i class="bi bi-check-circle-fill" aria-hidden="true"></i><span>Bạn đã đăng xuất.</span>
            </div>
        </c:if>

        <form method="post" action="/login">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <div class="sh-field">
                <label for="username">Địa chỉ email</label>
                <input class="sh-input" type="email" id="username" name="username" autocomplete="username" placeholder="name@example.com" required autofocus />
            </div>
            <div class="sh-field">
                <label for="password">Mật khẩu</label>
                <div class="sh-pw">
                    <input class="sh-input" type="password" id="password" name="password" autocomplete="current-password" required />
                    <button type="button" class="sh-pw-toggle" data-pw-toggle="password" aria-label="Hiện mật khẩu"><i class="bi bi-eye" aria-hidden="true"></i></button>
                </div>
            </div>
            <button type="submit" class="sh-btn block">Đăng nhập</button>
        </form>
        <p class="text-center mt-4 mb-0 sh-muted">Chưa có tài khoản? <a href="/register">Đăng ký</a></p>
    </div>
</div>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
