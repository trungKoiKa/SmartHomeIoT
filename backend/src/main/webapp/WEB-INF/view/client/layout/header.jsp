<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf" content="${_csrf.token}">
    <meta name="_csrf_header" content="${_csrf.headerName}">
    <title><c:out value="${not empty param.title ? param.title : 'SmartHome'}" /></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <link href="/client/css/smarthome.css" rel="stylesheet">
</head>
<body class="sh-body" data-live="${param.live}">
<a class="sh-skip" href="#main">Bỏ qua điều hướng</a>

<header class="sh-nav">
    <div class="sh-wrap">
        <a href="/" class="sh-brand"><i class="bi bi-house-heart-fill" aria-hidden="true"></i> SmartHome</a>

        <nav aria-label="Điều hướng chính">
            <ul class="sh-links">
                <li><a href="/" <c:if test="${param.nav == 'home'}">aria-current="page"</c:if>><i class="bi bi-house-door" aria-hidden="true"></i> Trang chủ</a></li>
                <li><a href="/client/room-list" <c:if test="${param.nav == 'rooms'}">aria-current="page"</c:if>><i class="bi bi-door-open" aria-hidden="true"></i> Phòng</a></li>
                <li><a href="/client/device" <c:if test="${param.nav == 'devices'}">aria-current="page"</c:if>><i class="bi bi-toggles" aria-hidden="true"></i> Thiết bị</a></li>
                <li><a href="/client/sensor-list" <c:if test="${param.nav == 'sensors'}">aria-current="page"</c:if>><i class="bi bi-activity" aria-hidden="true"></i> Cảm biến</a></li>
            </ul>
        </nav>

        <div class="sh-nav-right">
            <c:if test="${not empty pageContext.request.userPrincipal}">
                <div class="dropdown">
                    <button class="sh-user-btn" type="button" data-bs-toggle="dropdown" aria-expanded="false" aria-label="Menu tài khoản">
                        <c:choose>
                            <c:when test="${not empty sessionScope.avatar}">
                                <img class="sh-avatar" src="/images/avatar/<c:out value='${sessionScope.avatar}'/>" alt="">
                            </c:when>
                            <c:otherwise><i class="bi bi-person-circle fs-4" aria-hidden="true"></i></c:otherwise>
                        </c:choose>
                        <span class="d-none d-md-inline"><c:out value="${not empty sessionScope.fullName ? sessionScope.fullName : pageContext.request.userPrincipal.name}" /></span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow border-0 p-2" style="border-radius:16px;min-width:220px">
                        <li class="px-3 py-2 sh-muted"><c:out value="${pageContext.request.userPrincipal.name}" /></li>
                        <li><hr class="dropdown-divider"></li>
                        <li>
                            <form method="post" action="/logout" class="m-0">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                <button class="dropdown-item rounded-3 py-2"><i class="bi bi-box-arrow-right me-2" aria-hidden="true"></i>Đăng xuất</button>
                            </form>
                        </li>
                    </ul>
                </div>
            </c:if>
            <c:if test="${empty pageContext.request.userPrincipal}">
                <a href="/login" class="sh-btn">Đăng nhập</a>
            </c:if>
        </div>
    </div>
</header>

<nav class="sh-tabbar" aria-label="Điều hướng nhanh">
    <a href="/" <c:if test="${param.nav == 'home'}">aria-current="page"</c:if>><i class="bi bi-house-door" aria-hidden="true"></i>Trang chủ</a>
    <a href="/client/room-list" <c:if test="${param.nav == 'rooms'}">aria-current="page"</c:if>><i class="bi bi-door-open" aria-hidden="true"></i>Phòng</a>
    <a href="/client/device" <c:if test="${param.nav == 'devices'}">aria-current="page"</c:if>><i class="bi bi-toggles" aria-hidden="true"></i>Thiết bị</a>
    <a href="/client/sensor-list" <c:if test="${param.nav == 'sensors'}">aria-current="page"</c:if>><i class="bi bi-activity" aria-hidden="true"></i>Cảm biến</a>
</nav>

<main id="main" class="sh-wrap sh-main" tabindex="-1">
