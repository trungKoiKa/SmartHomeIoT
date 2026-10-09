<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="${sensor.name} - SmartHome" />
    <jsp:param name="nav" value="sensors" />
    <jsp:param name="live" value="5" />
</jsp:include>
<jsp:include page="/WEB-INF/view/client/layout/sensor-meta.jsp">
    <jsp:param name="type" value="${sensor.type}" />
</jsp:include>

<script defer src="https://cdn.jsdelivr.net/npm/chart.js@4.4.3/dist/chart.umd.min.js"></script>
<script defer src="/client/js/sensor-chart.js"></script>

<div class="sh-page-head">
    <div>
        <div class="sh-breadcrumb"><a href="/client/sensor-list">Cảm biến</a> / <c:out value="${sensor.name}" /></div>
        <h1><i class="bi ${shIcon}" aria-hidden="true"></i> <c:out value="${sensor.name}" /></h1>
        <p><c:out value="${shKind}" /> &middot; <c:out value="${sensor.room != null ? sensor.room.name : 'Chưa gán phòng'}" />
            &middot; <c:out value="${sensor.status == 'ACTIVE' ? 'Đang hoạt động' : 'Đang tắt'}" /></p>
    </div>
    <div class="sh-live" id="live-indicator">
        <span class="sh-live-dot" aria-hidden="true"></span>
        <span id="live-status">Đang chờ cập nhật…</span>
        <button type="button" id="live-toggle" aria-pressed="false">Tạm dừng</button>
    </div>
</div>

<div class="sh-card mb-4">
    <div class="sh-card-head">
        <h2 class="sh-title">Biểu đồ <c:out value="${dataCount}" /> lần đo gần nhất</h2>
    </div>
    <div class="sh-chart">
        <canvas id="sensor-chart" role="img" aria-label="Biểu đồ đường các lần đo gần nhất của <c:out value='${sensor.name}'/>. Bảng dữ liệu bên dưới có cùng nội dung."></canvas>
    </div>
</div>

<div id="sensor-live" data-live-region>
    <%-- Dữ liệu cho biểu đồ; live.js thay vùng này rồi sensor-chart.js vẽ lại --%>
    <script type="application/json" id="chart-data">{"label":"<c:out value='${sensor.name}'/>","unit":"<c:out value='${shUnit}'/>","points":[<c:forEach var="d" items="${dataList}" varStatus="s"><c:if test="${d.value != null}">{"t":"<c:out value='${d.formattedRecordedAt}'/>","v":${d.value}}<c:if test="${not s.last}">,</c:if></c:if></c:forEach>]}</script>

    <div class="row g-4">
        <div class="col-lg-7">
            <div class="sh-card h-100">
                <div class="sh-card-head"><h2 class="sh-title">Lịch sử dữ liệu</h2></div>
                <c:if test="${empty dataList}">
                    <div class="sh-empty shadow-none"><i class="bi bi-clipboard-x" aria-hidden="true"></i>Chưa có dữ liệu nào cho cảm biến này.</div>
                </c:if>
                <c:if test="${not empty dataList}">
                    <div class="table-responsive">
                        <table class="sh-table">
                            <caption class="visually-hidden">Các lần đo gần nhất</caption>
                            <thead><tr><th scope="col">Thời gian</th><th scope="col" class="text-end">Giá trị</th></tr></thead>
                            <tbody>
                            <c:forEach var="data" items="${dataList}">
                                <tr>
                                    <td class="sh-muted"><c:out value="${data.formattedRecordedAt}" /></td>
                                    <td class="num"><fmt:formatNumber value="${data.value}" maxFractionDigits="1" /> <c:out value="${shUnit}" /></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:if>
            </div>
        </div>

        <div class="col-lg-5">
            <div class="sh-card h-100">
                <div class="sh-card-head">
                    <h2 class="sh-title">Cảnh báo</h2>
                    <span class="sh-badge ${alertCount > 0 ? 'danger' : 'ok'}">
                        <i class="bi ${alertCount > 0 ? 'bi-exclamation-triangle-fill' : 'bi-shield-check'}" aria-hidden="true"></i>
                        <c:out value="${alertCount}" /> sự cố
                    </span>
                </div>
                <c:if test="${empty alertList}">
                    <div class="sh-empty shadow-none"><i class="bi bi-shield-check" aria-hidden="true"></i>Hệ thống an toàn, chưa có cảnh báo nào.</div>
                </c:if>
                <c:if test="${not empty alertList}">
                    <ul class="list-unstyled m-0">
                        <c:forEach var="alert" items="${alertList}">
                            <li class="d-flex gap-3 py-2 border-bottom">
                                <span class="sh-icon ${alert.alert ? 'danger' : 'ok'}">
                                    <i class="bi ${alert.alert ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'}" aria-hidden="true"></i>
                                </span>
                                <div>
                                    <div class="fw-semibold"><c:out value="${not empty alert.alertMessage ? alert.alertMessage : (alert.alert ? 'Vượt ngưỡng an toàn' : 'Đã trở lại an toàn')}" /></div>
                                    <div class="sh-muted"><c:out value="${alert.formattedRecordedAt}" /></div>
                                </div>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
