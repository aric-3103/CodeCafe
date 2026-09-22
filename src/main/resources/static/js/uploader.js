/**
 * uploader.js — Handles drag-and-drop / click-to-browse and the
 * "Remove Backgrounds" button click.
 *
 * Reads window.AppState.sessionId set by app.js.
 * Calls window.ImageCard.createCard() for each accepted record.
 * Fires window.Poller.start() once upload succeeds.
 *
 * Requirements: 1.7, 2.1, 2.2, 3.3, 10.2
 */
(function () {
  'use strict';

  const MAX_FILES = 50;

  /** Staged files waiting for the "Remove Backgrounds" click */
  let stagedFiles = [];

  function init() {
    const dropZone   = document.getElementById('dropZone');
    const fileInput  = document.getElementById('fileInput');
    const removeBtn  = document.getElementById('removeBtn');
    const errorsDiv  = document.getElementById('validationErrors');

    // --- Click to browse ---
    dropZone.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', () => {
      stageFiles(Array.from(fileInput.files));
      fileInput.value = ''; // allow re-selecting same files
    });

    // --- Drag and drop ---
    dropZone.addEventListener('dragover', e => {
      e.preventDefault();
      dropZone.classList.add('drag-over');
    });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
    dropZone.addEventListener('drop', e => {
      e.preventDefault();
      dropZone.classList.remove('drag-over');
      stageFiles(Array.from(e.dataTransfer.files));
    });

    // --- Remove Backgrounds button ---
    removeBtn.addEventListener('click', () => {
      if (stagedFiles.length === 0) return;
      uploadFiles(stagedFiles);
    });
  }

  /**
   * Validates that count ≤ 50 and at least one file is present,
   * then enables the Remove button.
   */
  function stageFiles(files) {
    const errorsDiv = document.getElementById('validationErrors');
    const removeBtn = document.getElementById('removeBtn');

    if (files.length === 0) return;

    if (files.length > MAX_FILES) {
      showErrors([`You selected ${files.length} files. Maximum is ${MAX_FILES}.`]);
      return;
    }

    stagedFiles = files;
    errorsDiv.hidden = true;
    errorsDiv.innerHTML = '';

    // Show staged count in the drop zone text
    const text = document.querySelector('.drop-zone__text');
    if (text) {
      text.textContent = `${files.length} file${files.length > 1 ? 's' : ''} selected — click Remove Backgrounds to start`;
    }

    removeBtn.disabled = false;
  }

  /**
   * POSTs the staged files to /api/images/upload.
   * Creates cards for accepted records; shows errors for rejected ones.
   */
  async function uploadFiles(files) {
    const removeBtn     = document.getElementById('removeBtn');
    const downloadAllBtn = document.getElementById('downloadAllBtn');
    const progressSection = document.getElementById('progressSection');
    const summaryPanel  = document.getElementById('summaryPanel');

    removeBtn.disabled   = true;
    summaryPanel.hidden  = true;

    const sessionId = window.AppState.sessionId;
    const formData  = new FormData();
    formData.append('sessionId', sessionId);
    files.forEach(f => formData.append('files', f));

    let data;
    try {
      const res = await fetch('/api/images/upload', { method: 'POST', body: formData });
      data = await res.json();
      if (!res.ok) {
        showErrors([data.error || 'Upload failed.']);
        removeBtn.disabled = false;
        return;
      }
    } catch (err) {
      showErrors(['Network error: ' + err.message]);
      removeBtn.disabled = false;
      return;
    }

    // Render cards for accepted records
    if (data.records && data.records.length > 0) {
      data.records.forEach(r => window.ImageCard.createCard(r));
      progressSection.hidden = false;
      window.Poller.start(sessionId, data.records.length);
    }

    // Show inline errors for rejected files
    if (data.rejectedFiles && data.rejectedFiles.length > 0) {
      const msgs = data.rejectedFiles.map(
        f => `"${f}" was rejected (unsupported format, MIME mismatch, or exceeds 25 MB).`
      );
      showErrors(msgs);
    }

    stagedFiles = [];
  }

  function showErrors(messages) {
    const errorsDiv = document.getElementById('validationErrors');
    errorsDiv.innerHTML = '<ul>' + messages.map(m => `<li>${escHtml(m)}</li>`).join('') + '</ul>';
    errorsDiv.hidden = false;
  }

  function escHtml(s) {
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }

  // Expose init
  window.Uploader = { init };
})();
