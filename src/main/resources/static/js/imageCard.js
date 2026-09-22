/**
 * imageCard.js — Creates and updates per-image DOM cards.
 *
 * Exported functions (attached to window):
 *   createCard(record)  → HTMLElement  (call once after upload)
 *   updateCard(record)  → void         (call on every poll update)
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.5, 6.1
 */
(function () {
  'use strict';

  /** Map<recordId, HTMLElement> — so updateCard can find the card fast */
  const cardMap = new Map();

  /**
   * Creates a card element for a newly uploaded record and appends it to
   * #cardGrid. Returns the created element.
   *
   * @param {Object} record  — { id, originalFilename, status, errorMessage }
   * @param {string} originalPreviewUrl — /api/images/{id}/preview?type=original
   */
  function createCard(record) {
    const card = document.createElement('div');
    card.className = 'img-card';
    card.dataset.recordId = record.id;

    card.innerHTML = `
      <div class="img-card__header">
        <span class="img-card__name" title="${esc(record.originalFilename)}">${esc(record.originalFilename)}</span>
        <span class="badge badge--idle" id="badge-${record.id}">IDLE</span>
      </div>
      <div class="img-card__previews">
        <div class="preview-slot">
          <span class="preview-slot__label">Original</span>
          <img src="/api/images/${record.id}/preview?type=original"
               alt="original" loading="lazy" />
        </div>
        <div class="preview-slot" id="result-slot-${record.id}">
          <span class="preview-slot__label">Result</span>
          <div class="preview-placeholder" id="result-placeholder-${record.id}">⏳</div>
        </div>
      </div>
      <div class="img-card__footer" id="footer-${record.id}">
        <span class="img-card__error" id="error-${record.id}" hidden></span>
      </div>
    `;

    cardMap.set(record.id, card);
    document.getElementById('cardGrid').appendChild(card);
    return card;
  }

  /**
   * Updates an existing card to reflect the latest record state.
   *
   * @param {Object} record — { id, originalFilename, status, errorMessage }
   */
  function updateCard(record) {
    const card = cardMap.get(record.id);
    if (!card) return;

    const badge       = card.querySelector(`#badge-${record.id}`);
    const footer      = card.querySelector(`#footer-${record.id}`);
    const errorEl     = card.querySelector(`#error-${record.id}`);
    const placeholder = card.querySelector(`#result-placeholder-${record.id}`);
    const resultSlot  = card.querySelector(`#result-slot-${record.id}`);

    // --- Badge ---
    const statusLower = record.status.toLowerCase();
    badge.className   = `badge badge--${statusLower}`;
    badge.textContent = record.status;

    // --- Result preview ---
    if (record.status === 'COMPLETED') {
      // Replace placeholder with actual result image on checker background
      if (placeholder) {
        const img = document.createElement('img');
        img.src = `/api/images/${record.id}/preview?type=result`;
        img.alt = 'result';
        img.loading = 'lazy';
        img.className = 'checker-bg';
        placeholder.replaceWith(img);
      }

      // --- Download button ---
      if (!footer.querySelector('.btn--download')) {
        const a = document.createElement('a');
        a.href      = `/api/images/${record.id}/download`;
        a.className = 'btn--download';
        a.textContent = '⬇ Download';
        a.download  = '';
        footer.appendChild(a);
      }

      // Hide error if previously shown
      errorEl.hidden = true;

    } else if (record.status === 'FAILED') {
      // Show error message
      errorEl.textContent = record.errorMessage || 'Background removal failed.';
      errorEl.hidden = false;

      if (placeholder) {
        placeholder.textContent = '✗';
        placeholder.style.color = '#dc2626';
      }
    }
    // IDLE / PROCESSING — spinner handled by CSS; no extra DOM changes needed
  }

  function esc(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // Expose to other modules
  window.ImageCard = { createCard, updateCard };
})();
