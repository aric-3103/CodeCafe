/**
 * app.js — Entry point. Wires everything together.
 *
 * - Generates a unique sessionId once per page load.
 * - Initialises Uploader (drag-drop + upload logic).
 * - Wires "Download All as ZIP" button.
 * - Wires summary panel dismiss button.
 *
 * Requirements: 7.1, 9.5, 10.3
 */
(function () {
  'use strict';

  // Shared state accessible by uploader.js and poller.js
  window.AppState = {
    sessionId: crypto.randomUUID()
  };

  document.addEventListener('DOMContentLoaded', () => {

    // Initialise uploader (drag-drop, file input, upload button)
    window.Uploader.init();

    // --- Download All as ZIP ---
    const downloadAllBtn = document.getElementById('downloadAllBtn');
    if (downloadAllBtn) {
      downloadAllBtn.addEventListener('click', () => {
        const url = `/api/session/${window.AppState.sessionId}/download-all`;
        // Trigger browser download via a temporary anchor
        const a = document.createElement('a');
        a.href     = url;
        a.download = 'processed-images.zip';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      });
    }

    // --- Summary panel dismiss ---
    const summaryClose = document.getElementById('summaryClose');
    if (summaryClose) {
      summaryClose.addEventListener('click', () => {
        const panel = document.getElementById('summaryPanel');
        if (panel) panel.hidden = true;
      });
    }
  });
})();
