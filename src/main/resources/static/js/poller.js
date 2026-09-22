/**
 * poller.js — Polls /api/session/{sessionId}/status every 1 second.
 *
 * Stops when allTerminal === true.
 * Updates cards via window.ImageCard.updateCard().
 * Updates the progress bar.
 * Shows the summary panel and "Download All" button on completion.
 *
 * Requirements: 2.3, 2.4, 2.5, 5.6, 9.1, 9.2, 9.5
 */
(function () {
  'use strict';

  let intervalId  = null;
  let totalFiles  = 0;

  /**
   * Start polling for the given session.
   * @param {string} sessionId
   * @param {number} total — number of accepted records (used for progress display)
   */
  function start(sessionId, total) {
    if (intervalId) stop(); // cancel any previous poll
    totalFiles = total;
    updateProgressBar(0, total);

    intervalId = setInterval(() => poll(sessionId), 1000);
  }

  function stop() {
    if (intervalId) {
      clearInterval(intervalId);
      intervalId = null;
    }
  }

  async function poll(sessionId) {
    let data;
    try {
      const res = await fetch(`/api/session/${sessionId}/status`);
      if (!res.ok) return; // transient error — retry next tick
      data = await res.json();
    } catch {
      return; // network blip — retry
    }

    // Update each card
    if (data.records) {
      data.records.forEach(r => window.ImageCard.updateCard(r));
    }

    // Progress bar: completed + failed out of total
    const done = (data.completed || 0) + (data.failed || 0);
    updateProgressBar(done, data.total || totalFiles);

    // Show "Download All" button as soon as at least one COMPLETED
    if ((data.completed || 0) > 0) {
      const btn = document.getElementById('downloadAllBtn');
      if (btn) btn.hidden = false;
    }

    // Terminal state — stop polling and show summary
    if (data.allTerminal) {
      stop();
      showSummary(data);
    }
  }

  function updateProgressBar(done, total) {
    const fill = document.getElementById('progressBarFill');
    const text = document.getElementById('progressText');
    if (!fill || !text) return;

    const pct = total > 0 ? Math.round((done / total) * 100) : 0;
    fill.style.width = pct + '%';
    text.textContent = total > 0
      ? `Processing… ${done} / ${total} (${pct}%)`
      : 'Processing…';
  }

  function showSummary(data) {
    const panel = document.getElementById('summaryPanel');
    if (!panel) return;

    document.getElementById('sumTotal').textContent     = data.total     || 0;
    document.getElementById('sumCompleted').textContent = data.completed || 0;
    document.getElementById('sumFailed').textContent    = data.failed    || 0;
    document.getElementById('sumSkipped').textContent   = data.skipped   || 0;

    // Update progress bar to 100%
    const fill = document.getElementById('progressBarFill');
    const text = document.getElementById('progressText');
    if (fill) fill.style.width = '100%';
    if (text) text.textContent =
      `Done — ${data.completed || 0} completed, ${data.failed || 0} failed`;

    panel.hidden = false;
  }

  window.Poller = { start, stop };
})();
