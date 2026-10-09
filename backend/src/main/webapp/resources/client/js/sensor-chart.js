/* Biểu đồ đường lịch sử cảm biến. Dữ liệu lấy từ <script type="application/json" id="chart-data"> trong vùng live,
   nên mỗi lần live.js thay vùng thì biểu đồ được vẽ lại. */
(function () {
  'use strict';
  var chart = null;

  function read() {
    var el = document.getElementById('chart-data');
    if (!el) return null;
    try { return JSON.parse(el.textContent); } catch (e) { return null; }
  }

  function draw() {
    var canvas = document.getElementById('sensor-chart');
    var data = read();
    if (!canvas || !data || !window.Chart) return;
    // dataList mới nhất đứng đầu -> đảo lại để thời gian tăng dần từ trái sang phải
    var rows = data.points.slice().reverse();
    var labels = rows.map(function (p) { return p.t; });
    var values = rows.map(function (p) { return p.v; });
    var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (chart) {
      chart.data.labels = labels;
      chart.data.datasets[0].data = values;
      chart.update('none');
      return;
    }
    chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: data.label, data: values, borderColor: '#0a6fe0', backgroundColor: 'rgba(10,111,224,.12)',
          fill: true, tension: .35, pointRadius: 4, pointHoverRadius: 6, borderWidth: 2
        }]
      },
      options: {
        responsive: true, maintainAspectRatio: false, animation: reduce ? false : { duration: 300 },
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: function (c) { return c.parsed.y + (data.unit ? ' ' + data.unit : ''); } } }
        },
        scales: {
          y: { grid: { color: '#ececf0' }, ticks: { callback: function (v) { return v + (data.unit ? ' ' + data.unit : ''); } } },
          x: { grid: { display: false }, ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 5 } }
        }
      }
    });
  }

  document.addEventListener('DOMContentLoaded', draw);
  document.addEventListener('sh:live-updated', draw);
})();
