/* Điều khiển thiết bị + giọng nói + toast, dùng chung cho các trang client.
   Dùng event delegation nên vẫn chạy sau khi live.js thay nội dung vùng. */
(function () {
  'use strict';

  var SH = window.SH = window.SH || {};
  SH.pending = 0; // số lệnh đang gửi; live.js không thay DOM trong lúc này

  function csrf() {
    var t = document.querySelector('meta[name="_csrf"]');
    var h = document.querySelector('meta[name="_csrf_header"]');
    return t && h ? { header: h.content, token: t.content } : null;
  }

  function postJson(url, body) {
    var headers = { 'Content-Type': 'application/json' };
    var c = csrf();
    if (c) headers[c.header] = c.token;
    return fetch(url, {
      method: 'POST', headers: headers, credentials: 'same-origin',
      body: body === undefined ? undefined : JSON.stringify(body)
    }).then(function (res) {
      return res.json().catch(function () { return {}; }).then(function (data) {
        data.httpStatus = res.status;
        if (res.status === 503) data.message = data.message || 'Chưa kết nối gateway/MQTT, lệnh chưa được gửi.';
        if (res.redirected || res.status === 401 || res.status === 403) data.message = 'Phiên đã hết hạn hoặc không đủ quyền, hãy đăng nhập lại.';
        return data;
      });
    });
  }

  SH.toast = function (message, ok) {
    var wrap = document.getElementById('sh-toasts');
    if (!wrap) return;
    var el = document.createElement('div');
    el.className = 'sh-toast ' + (ok === false ? 'error' : 'ok');
    var icon = document.createElement('i');
    icon.className = 'bi ' + (ok === false ? 'bi-exclamation-circle-fill' : 'bi-check-circle-fill');
    icon.setAttribute('aria-hidden', 'true');
    var text = document.createElement('span');
    text.textContent = message;
    el.appendChild(icon); el.appendChild(text);
    wrap.appendChild(el);
    setTimeout(function () { el.remove(); }, ok === false ? 5000 : 3000);
  };

  // Cập nhật mọi phần tử hiển thị trạng thái của thiết bị (trang thiết bị, trang phòng, trang chủ)
  SH.applyDeviceState = function (id, status) {
    var on = status === 'ON';
    document.querySelectorAll('[data-device-switch="' + id + '"]').forEach(function (sw) {
      sw.classList.toggle('on', on);
      sw.setAttribute('aria-checked', on ? 'true' : 'false');
      sw.classList.remove('busy');
    });
    document.querySelectorAll('[data-device-card="' + id + '"]').forEach(function (c) { c.classList.toggle('on', on); });
    document.querySelectorAll('[data-device-status="' + id + '"]').forEach(function (b) {
      b.className = 'sh-badge ' + (on ? 'ok' : '');
      b.innerHTML = '';
      var i = document.createElement('i');
      i.className = 'bi ' + (on ? 'bi-power' : 'bi-power');
      i.setAttribute('aria-hidden', 'true');
      b.appendChild(i);
      b.appendChild(document.createTextNode(on ? 'Đang bật' : 'Đang tắt'));
    });
  };

  document.addEventListener('click', function (e) {
    var sw = e.target.closest('[data-device-switch]');
    if (!sw || sw.disabled || sw.classList.contains('busy')) return;
    var id = sw.getAttribute('data-device-switch');
    sw.classList.add('busy');
    SH.pending++;
    postJson('/client/device/' + id + '/toggle').then(function (data) {
      if (!data.success) {
        sw.classList.remove('busy');
        SH.toast(data.message || 'Không thể cập nhật thiết bị', false);
        return;
      }
      SH.applyDeviceState(id, data.status);
      SH.toast(data.status === 'ON' ? 'Đã bật thiết bị' : 'Đã tắt thiết bị', true);
    }).catch(function () {
      sw.classList.remove('busy');
      SH.toast('Không gửi được lệnh, kiểm tra kết nối mạng.', false);
    }).then(function () { SH.pending = Math.max(0, SH.pending - 1); });
  });

  // Lọc theo phòng (trang thiết bị)
  document.addEventListener('click', function (e) {
    var chip = e.target.closest('[data-room-filter]');
    if (!chip) return;
    var room = chip.getAttribute('data-room-filter');
    document.querySelectorAll('[data-room-filter]').forEach(function (c) {
      c.setAttribute('aria-pressed', c === chip ? 'true' : 'false');
    });
    SH.roomFilter = room;
    SH.applyRoomFilter();
  });
  SH.applyRoomFilter = function () {
    var room = SH.roomFilter || 'all';
    document.querySelectorAll('[data-room]').forEach(function (col) {
      col.hidden = !(room === 'all' || col.getAttribute('data-room') === room);
    });
  };
  document.addEventListener('sh:live-updated', function () { SH.applyRoomFilter(); });

  // Hiện/ẩn mật khẩu
  document.addEventListener('click', function (e) {
    var b = e.target.closest('[data-pw-toggle]');
    if (!b) return;
    var input = document.getElementById(b.getAttribute('data-pw-toggle'));
    if (!input) return;
    var show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    b.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
    b.querySelector('i').className = 'bi ' + (show ? 'bi-eye-slash' : 'bi-eye');
  });

  // Giọng nói (Web Speech API, vi-VN)
  document.addEventListener('DOMContentLoaded', function () {
    var btn = document.getElementById('voice-btn');
    if (!btn) return;
    var hint = document.getElementById('voice-hint');
    var label = document.getElementById('voice-label');
    var SR = window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SR || !window.isSecureContext) {
      btn.disabled = true;
      hint.textContent = 'Giọng nói cần Chrome/Edge và kết nối HTTPS (hoặc localhost).';
      return;
    }
    var recognition = new SR();
    recognition.lang = 'vi-VN';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    var listening = false;
    function setListening(on) {
      listening = on;
      btn.classList.toggle('listening', on);
      btn.setAttribute('aria-pressed', on ? 'true' : 'false');
      label.textContent = on ? 'Đang nghe… (bấm để dừng)' : 'Điều khiển bằng giọng nói';
    }

    function send(text) {
      hint.textContent = 'Bạn nói: "' + text + '"';
      SH.pending++;
      postJson('/client/voice/command', { text: text }).then(function (data) {
        if (data.success) (data.devices || []).forEach(function (d) { SH.applyDeviceState(d.id, d.status); });
        SH.toast(data.message || 'Không có phản hồi', !!data.success);
      }).catch(function () {
        SH.toast('Không gửi được lệnh giọng nói.', false);
      }).then(function () { SH.pending = Math.max(0, SH.pending - 1); });
    }

    recognition.onresult = function (e) { send(e.results[0][0].transcript); };
    recognition.onerror = function (e) {
      SH.toast(e.error === 'not-allowed' ? 'Chưa cấp quyền microphone'
        : e.error === 'no-speech' ? 'Không nghe thấy giọng nói'
        : 'Lỗi nhận dạng: ' + e.error, false);
    };
    recognition.onend = function () { setListening(false); };

    btn.addEventListener('click', function () {
      if (listening) { recognition.stop(); return; }
      try { recognition.start(); setListening(true); } catch (err) { setListening(false); }
    });
  });
})();
