<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="Đăng ký - SmartHome" />
</jsp:include>

<div class="sh-auth wide">
    <div class="sh-card">
        <h1>Tạo tài khoản</h1>
        <p class="sh-muted mb-4">Mật khẩu cần đủ mạnh theo yêu cầu của hệ thống.</p>

        <form:form method="post" action="/register" modelAttribute="registerUser">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

            <c:set var="errorFirstName"><form:errors path="firstName" cssClass="sh-error" /></c:set>
            <c:set var="errorEmail"><form:errors path="email" cssClass="sh-error" /></c:set>
            <c:set var="errorPassword"><form:errors path="confirmPassword" cssClass="sh-error" /></c:set>

            <div class="row g-3">
                <div class="col-md-6 sh-field">
                    <label for="firstName">Tên</label>
                    <form:input path="firstName" id="firstName" cssClass="sh-input ${not empty errorFirstName ? 'is-invalid' : ''}" autocomplete="given-name" />
                    ${errorFirstName}
                </div>
                <div class="col-md-6 sh-field">
                    <label for="lastName">Họ</label>
                    <form:input path="lastName" id="lastName" cssClass="sh-input" autocomplete="family-name" />
                </div>
            </div>

            <div class="sh-field">
                <label for="email">Địa chỉ email</label>
                <form:input path="email" id="email" type="email" cssClass="sh-input ${not empty errorEmail ? 'is-invalid' : ''}" autocomplete="email" />
                ${errorEmail}
            </div>

            <div class="row g-3">
                <div class="col-md-6 sh-field">
                    <label for="reg-password">Mật khẩu</label>
                    <div class="sh-pw">
                        <form:password path="password" id="reg-password" cssClass="sh-input ${not empty errorPassword ? 'is-invalid' : ''}" autocomplete="new-password" />
                        <button type="button" class="sh-pw-toggle" data-pw-toggle="reg-password" aria-label="Hiện mật khẩu"><i class="bi bi-eye" aria-hidden="true"></i></button>
                    </div>
                </div>
                <div class="col-md-6 sh-field">
                    <label for="confirmPassword">Xác nhận mật khẩu</label>
                    <form:password path="confirmPassword" id="confirmPassword" cssClass="sh-input ${not empty errorPassword ? 'is-invalid' : ''}" autocomplete="new-password" />
                    ${errorPassword}
                </div>
            </div>

            <button type="submit" class="sh-btn block">Tạo tài khoản</button>
        </form:form>
        <p class="text-center mt-4 mb-0 sh-muted">Đã có tài khoản? <a href="/login">Đăng nhập</a></p>
    </div>
</div>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
