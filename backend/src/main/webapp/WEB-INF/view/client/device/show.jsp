<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="/WEB-INF/view/client/layout/header.jsp">
    <jsp:param name="title" value="Điều khiển thiết bị - SmartHome" />
</jsp:include>

<div class="container py-5 mt-5">
    <div class="text-center mx-auto mb-5" style="max-width: 760px;">
        <h1 class="display-5 fw-bold">Điều khiển thiết bị</h1>
        <p class="text-muted mb-0">Quản lý trạng thái bật/tắt thiết bị theo từng phòng.</p>

        <c:if test="${isLoggedIn}">
            <div class="mt-4">
                <button type="button" id="voice-btn" class="btn btn-outline-primary rounded-pill px-4 py-2 shadow-sm">
                    <span id="voice-icon">&#127908;</span> <span id="voice-label">Điều khiển bằng giọng nói</span>
                </button>
                <div id="voice-hint" class="small text-muted mt-2">Ví dụ: "bật đèn 1", "tắt quạt", "tắt tất cả".</div>
            </div>
        </c:if>
    </div>

    <c:if test="${empty devices}">
        <div class="alert alert-info rounded-4 shadow-sm border-0 p-4 text-center">
            Chưa có thiết bị nào trong hệ thống.
        </div>
    </c:if>

    <c:if test="${not empty devices}">
        <div class="mb-4 text-center">
            <div class="btn-group shadow-sm rounded-pill p-1 bg-white" role="group">
                <button class="btn btn-primary rounded-pill px-4 filter-btn active" type="button" onclick="filterRoom('all', this)">
                    Tất cả
                </button>
                <c:forEach var="room" items="${rooms}">
                    <button class="btn btn-white rounded-pill px-4 filter-btn text-muted" type="button" onclick="filterRoom('${room.id}', this)">
                        ${room.name}
                    </button>
                </c:forEach>
            </div>
        </div>

        <div class="row g-4" id="device-grid">
            <c:forEach var="device" items="${devices}">
                <div class="col-lg-4 col-md-6 device-col" data-room="${device.room != null ? device.room.id : 'none'}">
                    <div class="card border-0 shadow-sm rounded-4 h-100 device-card">
                        <div class="card-body p-4">
                            <div class="d-flex justify-content-between align-items-start mb-3">
                                <div>
                                    <h5 class="fw-bold text-dark mb-1">${device.name}</h5>
                                    <small class="text-muted">Thiết bị</small>
                                </div>
                                <c:choose>
                                    <c:when test="${isLoggedIn}">
                                        <button class="toggle-switch ${device.status == 'ON' ? 'on' : 'off'}"
                                                id="toggle-${device.id}"
                                                data-device-id="${device.id}"
                                                onclick="handleToggle(this)"></button>
                                    </c:when>
                                    <c:otherwise>
                                        <button class="toggle-switch ${device.status == 'ON' ? 'on' : 'off'}" disabled
                                                title="Cần đăng nhập để điều khiển"></button>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <div class="d-flex justify-content-between align-items-center">
                                <span class="badge bg-light text-muted border rounded-pill px-3 py-2">
                                    ${device.room != null ? device.room.name : 'Chưa gán phòng'}
                                </span>
                                <span class="badge ${device.status == 'ON' ? 'bg-success' : 'bg-secondary'} rounded-pill px-3 py-2"
                                      id="status-${device.id}">
                                    ${device.status}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:if>
</div>

<c:if test="${not isLoggedIn}">
    <div class="container mt-2">
        <div class="alert alert-warning border-0 shadow-sm rounded-4 text-center">
            Bạn đang ở chế độ khách, chỉ có quyền xem thông tin. Vui lòng <a href="/login">đăng nhập</a> để điều khiển thiết bị.
        </div>
    </div>
</c:if>

<div class="position-fixed bottom-0 end-0 p-3" style="z-index:9999">
    <div id="toggleToast" class="toast align-items-center text-white border-0 shadow-lg" role="alert">
        <div class="d-flex">
            <div class="toast-body" id="toast-msg">Đã cập nhật trạng thái</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    </div>
</div>

<script>
    function handleToggle(btn) {
        const deviceId = btn.getAttribute('data-device-id');
        toggleDevice(deviceId);
    }

    function toggleDevice(deviceId) {
        fetch('/client/device/' + deviceId + '/toggle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                '${_csrf.headerName}': '${_csrf.token}'
            }
        })
        .then(res => res.json())
        .then(data => {
            if (!data.success) {
                alert('Lỗi: ' + (data.message || 'Không thể cập nhật thiết bị'));
                return;
            }

            const btn = document.getElementById('toggle-' + deviceId);
            const badge = document.getElementById('status-' + deviceId);

            if (data.status === 'ON') {
                btn.className = 'toggle-switch on';
                badge.className = 'badge bg-success rounded-pill px-3 py-2';
                badge.textContent = 'ON';
            } else {
                btn.className = 'toggle-switch off';
                badge.className = 'badge bg-secondary rounded-pill px-3 py-2';
                badge.textContent = 'OFF';
            }

            const toastEl = document.getElementById('toggleToast');
            document.getElementById('toast-msg').textContent = data.status === 'ON'
                ? 'Thiết bị đã bật'
                : 'Thiết bị đã tắt';
            toastEl.className = 'toast align-items-center text-white border-0 shadow-lg ' + (data.status === 'ON' ? 'bg-success' : 'bg-primary');
            new bootstrap.Toast(toastEl, { delay: 2500 }).show();
        })
        .catch(err => alert('Lỗi: ' + err));
    }

    function filterRoom(roomId, btn) {
        document.querySelectorAll('.filter-btn').forEach(b => {
            b.classList.remove('btn-primary', 'active');
            b.classList.add('btn-white', 'text-muted');
        });

        btn.classList.remove('btn-white', 'text-muted');
        btn.classList.add('btn-primary', 'active');

        document.querySelectorAll('.device-col').forEach(col => {
            col.style.display = (roomId === 'all' || col.dataset.room === roomId) ? '' : 'none';
        });
    }
</script>

<c:if test="${isLoggedIn}">
<script>
    (function () {
        const btn = document.getElementById('voice-btn');
        const hint = document.getElementById('voice-hint');
        const label = document.getElementById('voice-label');
        const SR = window.SpeechRecognition || window.webkitSpeechRecognition;

        // Web Speech API chỉ chạy trên Chrome/Edge và trong secure context (HTTPS hoặc localhost)
        if (!SR || !window.isSecureContext) {
            btn.disabled = true;
            hint.textContent = 'Giọng nói cần Chrome/Edge và kết nối HTTPS (hoặc localhost).';
            return;
        }

        const recognition = new SR();
        recognition.lang = 'vi-VN';
        recognition.interimResults = false;
        recognition.maxAlternatives = 1;

        let listening = false;
        const setListening = (on) => {
            listening = on;
            btn.classList.toggle('btn-danger', on);
            btn.classList.toggle('btn-outline-primary', !on);
            label.textContent = on ? 'Đang nghe... (bấm để dừng)' : 'Điều khiển bằng giọng nói';
        };

        const showToast = (message, ok) => {
            const toastEl = document.getElementById('toggleToast');
            document.getElementById('toast-msg').textContent = message;
            toastEl.className = 'toast align-items-center text-white border-0 shadow-lg ' + (ok ? 'bg-success' : 'bg-danger');
            new bootstrap.Toast(toastEl, { delay: 3500 }).show();
        };

        const applyState = (id, status) => {
            const toggle = document.getElementById('toggle-' + id);
            const badge = document.getElementById('status-' + id);
            const on = status === 'ON';
            if (toggle) toggle.className = 'toggle-switch ' + (on ? 'on' : 'off');
            if (badge) {
                badge.className = 'badge ' + (on ? 'bg-success' : 'bg-secondary') + ' rounded-pill px-3 py-2';
                badge.textContent = status;
            }
        };

        const sendCommand = (text) => {
            hint.textContent = 'Bạn nói: "' + text + '"';
            fetch('/client/voice/command', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    '${_csrf.headerName}': '${_csrf.token}'
                },
                body: JSON.stringify({ text: text })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    (data.devices || []).forEach(d => applyState(d.id, d.status));
                }
                showToast(data.message || 'Không có phản hồi', !!data.success);
            })
            .catch(err => showToast('Lỗi: ' + err, false));
        };

        recognition.onresult = (e) => sendCommand(e.results[0][0].transcript);
        recognition.onerror = (e) => {
            const msg = e.error === 'not-allowed' ? 'Chưa cấp quyền microphone'
                      : e.error === 'no-speech' ? 'Không nghe thấy giọng nói'
                      : 'Lỗi nhận dạng: ' + e.error;
            showToast(msg, false);
        };
        recognition.onend = () => setListening(false);

        btn.addEventListener('click', () => {
            if (listening) {
                recognition.stop();
                return;
            }
            try {
                recognition.start();
                setListening(true);
            } catch (err) {
                setListening(false);
            }
        });
    })();
</script>
</c:if>

<style>
    .device-card {
        transition: all 0.25s ease;
        border: 1px solid rgba(0, 0, 0, 0.05) !important;
    }

    .device-card:hover {
        transform: translateY(-6px);
        box-shadow: 0 1rem 2rem rgba(0, 0, 0, 0.08) !important;
    }

    .btn-white {
        background: #fff;
        border: 1px solid transparent;
    }
</style>

<jsp:include page="/WEB-INF/view/client/layout/footer.jsp" />
