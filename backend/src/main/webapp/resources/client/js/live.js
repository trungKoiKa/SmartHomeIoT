/* Tự cập nhật số liệu mà không cần endpoint JSON mới.
   - <body data-live="5">: làm mới các vùng [data-live-region] mỗi 5 giây bằng cách tải lại HTML của trang.
   - Vùng có data-live-src="/url" (và tuỳ chọn data-live-from="id") lấy nội dung từ trang khác (dùng ở trang chủ).
   Dừng khi tab ẩn, có nút Tạm dừng, lùi dần khi lỗi, không thay DOM khi đang gửi lệnh. */
(function () {
  'use strict';

  var body = document.body;
  var base = parseInt(body.getAttribute('data-live'), 10);
  var regions = document.querySelectorAll('[data-live-region]');
  if (!regions.length) return;
  var hasRemote = Array.prototype.some.call(regions, function (r) { return r.hasAttribute('data-live-src'); });
  if (!base && !hasRemote) return;
  base = base || 5;

  var paused = false, fails = 0, timer = null, inflight = false;
  var statusEl = document.getElementById('live-status');
  var wrapEl = document.getElementById('live-indicator');
  var toggleEl = document.getElementById('live-toggle');

  function setStatus(kind, text) {
    if (wrapEl) { wrapEl.classList.toggle('paused', kind === 'paused'); wrapEl.classList.toggle('error', kind === 'error'); }
    if (statusEl) statusEl.textContent = text;
  }
  function clock() { return new Date().toLocaleTimeString('vi-VN'); }

  function fetchDoc(url) {
    return fetch(url, { credentials: 'same-origin', headers: { 'X-Live-Refresh': '1' } }).then(function (res) {
      if (res.redirected && /\/login/.test(res.url)) throw new Error('session');
      if (!res.ok) throw new Error('http ' + res.status);
      return res.text();
    }).then(function (html) { return new DOMParser().parseFromString(html, 'text/html'); });
  }

  function refresh() {
    if (inflight || paused || document.hidden) return Promise.resolve();
    if (window.SH && window.SH.pending > 0) return Promise.resolve();
    inflight = true;
    var bySrc = {};
    regions.forEach(function (r) {
      var src = r.getAttribute('data-live-src') || location.pathname + location.search;
      (bySrc[src] = bySrc[src] || []).push(r);
    });
    var focusedSwitch = document.activeElement && document.activeElement.getAttribute
      ? document.activeElement.getAttribute('data-device-switch') : null;

    return Promise.all(Object.keys(bySrc).map(function (src) {
      return fetchDoc(src).then(function (doc) {
        bySrc[src].forEach(function (r) {
          var node = doc.getElementById(r.getAttribute('data-live-from') || r.id);
          if (node) { r.innerHTML = node.innerHTML; r.removeAttribute('aria-busy'); }
        });
      });
    })).then(function () {
      fails = 0;
      setStatus('ok', 'Cập nhật lúc ' + clock());
      if (focusedSwitch) {
        var again = document.querySelector('[data-device-switch="' + focusedSwitch + '"]');
        if (again) again.focus({ preventScroll: true });
      }
      document.dispatchEvent(new CustomEvent('sh:live-updated'));
    }).catch(function (err) {
      fails++;
      if (err && err.message === 'session') {
        paused = true;
        setStatus('error', 'Phiên đã hết hạn, hãy tải lại trang');
      } else {
        setStatus('error', 'Mất kết nối, đang thử lại…');
      }
    }).then(function () { inflight = false; });
  }

  function schedule() {
    clearTimeout(timer);
    if (paused) return;
    var delay = base * 1000 * Math.min(Math.pow(2, fails), 6);
    timer = setTimeout(function () { refresh().then(schedule); }, delay);
  }

  if (toggleEl) {
    toggleEl.addEventListener('click', function () {
      paused = !paused;
      toggleEl.textContent = paused ? 'Tiếp tục' : 'Tạm dừng';
      toggleEl.setAttribute('aria-pressed', paused ? 'true' : 'false');
      if (paused) { setStatus('paused', 'Đã tạm dừng cập nhật'); clearTimeout(timer); }
      else { fails = 0; refresh().then(schedule); }
    });
  }
  document.addEventListener('visibilitychange', function () {
    if (!document.hidden && !paused) { clearTimeout(timer); refresh().then(schedule); }
  });

  regions.forEach(function (r) { if (r.hasAttribute('data-live-src')) r.setAttribute('aria-busy', 'true'); });
  if (hasRemote) refresh().then(schedule); else { setStatus('ok', 'Tự cập nhật mỗi ' + base + ' giây'); schedule(); }
})();
