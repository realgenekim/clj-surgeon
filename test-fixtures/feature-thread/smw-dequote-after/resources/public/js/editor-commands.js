// Browser-owned manuscript command bridge.
//
// Keep these high-value gestures out of app-purgatory.js: a failure in that
// legacy shortcut dispatcher must not allow Option-T to type a native dagger
// or make Format depend on an SSE round trip.

var _selectionToolbarFrame = null;

// DRAFT MARKDOWN PREVIEW -----------------------------------------------------
//
// Preview is a projection of the exact browser-owned textarea bytes. The
// textarea remains mounted and untouched so its undo stack, selection and
// scroll position survive the round trip. No editor command or save is sent.

var _draftPresentationMode = 'edit';
var _draftPreviewSession = null;
var _draftPreviewRenderGeneration = 0;

var DRAFT_PREVIEW_CSP = "default-src 'none'; img-src data:; style-src 'unsafe-inline'; form-action 'none'; base-uri 'none'";
var DRAFT_PREVIEW_CSS = `
  :root { color-scheme: light; }
  * { box-sizing: border-box; }
  body { margin: 0; padding: 18px clamp(24px, 6vw, 72px) 64px; color: #26262b;
    background: #fff; font: 16px/1.65 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
  body > :first-child { margin-top: 0; }
  body > :last-child { margin-bottom: 0; }
  h1, h2, h3, h4, h5, h6 { margin: 1.35em 0 .55em; color: #202124; line-height: 1.25; }
  h1 { font-size: 1.75em; }
  h2 { padding-bottom: .2em; border-bottom: 1px solid #e3e5e8; font-size: 1.42em; }
  h3 { font-size: 1.2em; }
  h4, h5, h6 { font-size: 1em; }
  p { margin: 0 0 1em; }
  ul, ol { margin: 0 0 1em 1.5em; padding: 0; }
  li { margin: .25em 0; }
  blockquote { margin: 1em 0; padding: .2em 1em; border-left: 4px solid #c9ccd1; color: #5f6368; }
  code { padding: .12em .3em; border-radius: 3px; background: #f1f3f4;
    font: .88em ui-monospace, SFMono-Regular, Menlo, monospace; }
  pre { margin: 1em 0; padding: 12px 14px; overflow: auto; border-radius: 5px; background: #f6f8fa; }
  pre code { padding: 0; background: transparent; }
  table { width: 100%; margin: 1em 0; border-collapse: collapse; font-size: .92em; }
  th, td { padding: 7px 9px; border: 1px solid #d8dce1; text-align: left; vertical-align: top; }
  th { background: #f6f8fa; font-weight: 650; }
  img { max-width: 100%; height: auto; }
  a { color: #2459b3; }
`;

function normalizeDraftPresentationMode(mode) {
  return mode === 'preview' ? 'preview' : 'edit';
}

function isDraftPreviewShortcut(event) {
  return Boolean(event?.altKey && !event.shiftKey && !event.ctrlKey && !event.metaKey &&
    event.code === 'KeyP');
}

function isCherryPickShortcut(event) {
  return Boolean(event?.altKey && event.shiftKey && !event.ctrlKey && !event.metaKey &&
    event.code === 'KeyP');
}

function captureDraftView(editor) {
  return {
    selectionStart: editor.selectionStart,
    selectionEnd: editor.selectionEnd,
    selectionDirection: editor.selectionDirection || 'none',
    scrollTop: editor.scrollTop,
    scrollLeft: editor.scrollLeft,
    focused: document.activeElement === editor
  };
}

function restoreDraftView(editor, view) {
  editor.hidden = false;
  if (!view) return;
  if (Number.isInteger(view.selectionStart) && Number.isInteger(view.selectionEnd)) {
    editor.setSelectionRange(
      view.selectionStart, view.selectionEnd, view.selectionDirection || 'none');
  }
  editor.scrollTop = view.scrollTop || 0;
  editor.scrollLeft = view.scrollLeft || 0;
  if (view.focused) editor.focus({preventScroll: true});
}

async function renderDraftMarkdown(source) {
  const response = await fetch('/api/draft/render-markdown', {
    method: 'POST',
    headers: {'Content-Type': 'text/plain; charset=utf-8', 'Accept': 'text/html'},
    body: source
  });
  if (!response.ok) throw new Error(`Markdown preview failed: HTTP ${response.status}`);
  return response.text();
}

function draftPreviewDocument(renderedHtml) {
  return '<!doctype html><html><head><meta charset="utf-8">' +
    '<meta http-equiv="Content-Security-Policy" content="' + DRAFT_PREVIEW_CSP + '">' +
    '<meta name="viewport" content="width=device-width, initial-scale=1">' +
    '<style>' + DRAFT_PREVIEW_CSS + '</style></head><body>' + renderedHtml + '</body></html>';
}

function draftPreviewAvailable() {
  const draftTab = document.getElementById('draft-tab');
  return Boolean(document.getElementById('draft-editor') &&
    document.getElementById('draft-markdown-preview') &&
    draftTab?.classList.contains('active'));
}

function updateDraftPreviewControl() {
  const control = document.getElementById('draft-view-mode');
  if (!control) return;
  control.value = _draftPresentationMode;
  const wrapper = document.getElementById('draft-view-control');
  if (wrapper) wrapper.hidden = !draftPreviewAvailable();
}

function logDraftPreviewAction(type, data) {
  if (typeof logAction !== 'function') return;
  logAction(type, data || {});
}

function draftPreviewProjectionStale() {
  // INTENT: DRAFT-PREV-003
  return !_draftPreviewSession ||
    document.getElementById('draft-editor') !== _draftPreviewSession.editor ||
    document.getElementById('draft-markdown-preview') !== _draftPreviewSession.preview;
}

async function showDraftMarkdownPreview(editor, preserveView) {
  const preview = document.getElementById('draft-markdown-preview');
  if (!preview) return false;
  const source = editor.value;
  const generation = ++_draftPreviewRenderGeneration;
  const priorView = preserveView && _draftPreviewSession?.editor === editor
    ? _draftPreviewSession.view
    : captureDraftView(editor);
  _draftPreviewSession = {editor, preview, view: priorView, source};
  hideSelectionToolbar();
  editor.hidden = true;
  preview.hidden = false;
  preview.setAttribute('aria-busy', 'true');
  preview.srcdoc = draftPreviewDocument('<p aria-live="polite">Rendering preview…</p>');

  try {
    const renderedHtml = await renderDraftMarkdown(source);
    if (_draftPresentationMode !== 'preview' || generation !== _draftPreviewRenderGeneration ||
        !editor.isConnected || document.getElementById('draft-editor') !== editor ||
        document.getElementById('draft-markdown-preview') !== preview ||
        editor.value !== source) {
      logDraftPreviewAction('draft.preview.stale', {
        generation,
        mode: _draftPresentationMode,
        editorCurrent: document.getElementById('draft-editor') === editor,
        previewCurrent: document.getElementById('draft-markdown-preview') === preview,
        sourceCurrent: editor.value === source
      });
      return false;
    }
    preview.srcdoc = draftPreviewDocument(renderedHtml);
    preview.removeAttribute('aria-busy');
    logDraftPreviewAction('draft.preview.rendered', {length: source.length});
    return true;
  } catch (error) {
    console.error('[DraftPreview]', error);
    logDraftPreviewAction('draft.preview.error', {message: String(error)});
    if (generation === _draftPreviewRenderGeneration) {
      _draftPresentationMode = 'edit';
      preview.hidden = true;
      preview.removeAttribute('aria-busy');
      restoreDraftView(editor, priorView);
      updateDraftPreviewControl();
      if (typeof showNotification === 'function') {
        showNotification('Markdown preview could not load; the editor is unchanged.', true, 9000);
      }
    }
    return false;
  }
}

function showDraftMarkdownEditor() {
  ++_draftPreviewRenderGeneration;
  const session = _draftPreviewSession;
  const preview = document.getElementById('draft-markdown-preview');
  if (preview) preview.hidden = true;
  if (session?.editor?.isConnected) restoreDraftView(session.editor, session.view);
  _draftPreviewSession = null;
}

function setDraftPresentationMode(mode) {
  const next = normalizeDraftPresentationMode(mode);
  const editor = document.getElementById('draft-editor');
  if (next === 'preview' && !draftPreviewAvailable()) return Promise.resolve(false);
  const previous = _draftPresentationMode;
  _draftPresentationMode = next;
  if (next !== previous) {
    logDraftPreviewAction('draft.preview.mode', {from: previous, to: next});
  }
  updateDraftPreviewControl();
  if (next === 'edit') {
    showDraftMarkdownEditor();
    return Promise.resolve(true);
  }
  return showDraftMarkdownPreview(editor, false);
}

function toggleDraftMarkdownPreview() {
  return setDraftPresentationMode(
    _draftPresentationMode === 'preview' ? 'edit' : 'preview');
}

function refreshDraftMarkdownPreview() {
  if (_draftPresentationMode !== 'preview') return Promise.resolve(false);
  const editor = document.getElementById('draft-editor');
  if (!editor || !draftPreviewAvailable()) return Promise.resolve(false);
  return showDraftMarkdownPreview(editor, _draftPreviewSession?.editor === editor);
}

function handleDraftPreviewShortcut(event) {
  if (!isDraftPreviewShortcut(event) || !draftPreviewAvailable()) return false;
  event.preventDefault();
  event.stopImmediatePropagation();
  toggleDraftMarkdownPreview();
  return true;
}

function handleCherryPickShortcut(event) {
  if (!isCherryPickShortcut(event) || typeof toggleCherryPick !== 'function') return false;
  event.preventDefault();
  event.stopImmediatePropagation();
  toggleCherryPick();
  return true;
}

function handleDraftViewModeChange(event) {
  if (event.target?.id !== 'draft-view-mode') return false;
  setDraftPresentationMode(event.target.value);
  return true;
}

function installDraftPreviewUi() {
  updateDraftPreviewControl();
}

// END DRAFT MARKDOWN PREVIEW -------------------------------------------------

function hideSelectionToolbar() {
  const toolbar = document.getElementById('selection-toolbar');
  if (!toolbar) return;
  toolbar.style.display = 'none';
  toolbar.setAttribute('aria-hidden', 'true');
}

function _textareaSelectionRect(editor) {
  const start = editor.selectionStart;
  const end = editor.selectionEnd;
  if (!Number.isInteger(start) || !Number.isInteger(end) || start === end) return null;

  const editorRect = editor.getBoundingClientRect();
  const computed = getComputedStyle(editor);
  const mirror = document.createElement('div');
  const marker = document.createElement('span');
  const copiedProperties = [
    'boxSizing', 'fontFamily', 'fontSize', 'fontStyle', 'fontWeight',
    'letterSpacing', 'lineHeight', 'paddingTop', 'paddingRight',
    'paddingBottom', 'paddingLeft', 'borderTopWidth', 'borderRightWidth',
    'borderBottomWidth', 'borderLeftWidth', 'wordSpacing', 'tabSize'
  ];
  copiedProperties.forEach(property => { mirror.style[property] = computed[property]; });
  mirror.style.position = 'fixed';
  mirror.style.visibility = 'hidden';
  mirror.style.pointerEvents = 'none';
  mirror.style.whiteSpace = 'pre-wrap';
  mirror.style.overflowWrap = 'break-word';
  mirror.style.width = editor.clientWidth + 'px';
  mirror.style.left = editorRect.left + 'px';
  mirror.style.top = (editorRect.top - editor.scrollTop) + 'px';
  mirror.textContent = editor.value.slice(0, start);
  marker.textContent = editor.value.slice(start, end) || '\u200b';
  mirror.appendChild(marker);
  document.body.appendChild(mirror);
  const firstRect = marker.getClientRects()[0] || marker.getBoundingClientRect();
  const rect = {
    left: firstRect.left,
    top: firstRect.top,
    right: firstRect.right,
    bottom: firstRect.bottom,
    width: firstRect.width,
    height: firstRect.height
  };
  mirror.remove();
  return rect;
}

function showSelectionToolbar() {
  const editor = document.getElementById('draft-editor');
  const toolbar = document.getElementById('selection-toolbar');
  const draftTab = document.getElementById('draft-tab');
  if (!editor || !toolbar || !draftTab?.classList.contains('active') ||
      document.activeElement !== editor || editor.selectionStart === editor.selectionEnd) {
    hideSelectionToolbar();
    return;
  }

  const selectionRect = _textareaSelectionRect(editor);
  const editorRect = editor.getBoundingClientRect();
  if (!selectionRect || selectionRect.bottom < editorRect.top || selectionRect.top > editorRect.bottom) {
    hideSelectionToolbar();
    return;
  }

  toolbar.style.display = 'flex';
  toolbar.setAttribute('aria-hidden', 'false');
  const toolbarRect = toolbar.getBoundingClientRect();
  const center = selectionRect.left + (selectionRect.width / 2);
  const left = Math.max(8, Math.min(window.innerWidth - toolbarRect.width - 8,
    center - (toolbarRect.width / 2)));
  const preferredTop = selectionRect.top - toolbarRect.height - 8;
  const top = preferredTop >= editorRect.top
    ? preferredTop
    : Math.min(editorRect.bottom - toolbarRect.height - 8, selectionRect.bottom + 8);
  toolbar.style.left = left + 'px';
  toolbar.style.top = Math.max(editorRect.top + 4, top) + 'px';
}

function scheduleSelectionToolbar() {
  if (_selectionToolbarFrame !== null) cancelAnimationFrame(_selectionToolbarFrame);
  _selectionToolbarFrame = requestAnimationFrame(() => {
    _selectionToolbarFrame = null;
    showSelectionToolbar();
  });
}

function openTransformFromSelection() {
  const editor = document.getElementById('draft-editor');
  if (!editor) return;
  const selected = editor.value.substring(editor.selectionStart, editor.selectionEnd).trim();
  if (!selected) {
    showNotification('Select text first', true);
    return;
  }
  hideSelectionToolbar();
  window._transformSelStart = editor.selectionStart;
  window._transformSelEnd = editor.selectionEnd;
  postJSON('/api/transform/open', {selected});
}

var _transformInstructionPending = false;

function setTransformInstructionBusy(busy) {
  const button = document.getElementById('transform-make-better');
  const submit = document.getElementById('transform-submit');
  const input = document.getElementById('transform-input');
  if (button) button.disabled = busy;
  if (submit) submit.disabled = busy;
  if (input) input.setAttribute('aria-busy', busy ? 'true' : 'false');
}

async function submitTransformInstruction(instruction) {
  const value = (instruction || '').trim();
  if (!value || _transformInstructionPending ||
      document.body?.dataset.editorCommandPending === 'true') return false;

  _transformInstructionPending = true;
  setTransformInstructionBusy(true);
  try {
    const response = await postJSON('/api/transform', {instruction: value});
    if (!response.ok) {
      showNotification('Transform request failed; your instruction remains available.', true, 9000);
      return false;
    }
    const input = document.getElementById('transform-input');
    if (input && input.value.trim() === value) input.value = '';
    return true;
  } catch (error) {
    console.error('[TransformInstruction]', error);
    showNotification('Transform request failed; try again.', true, 9000);
    return false;
  } finally {
    _transformInstructionPending = false;
    setTransformInstructionBusy(false);
  }
}

function submitTransformInput() {
  const input = document.getElementById('transform-input');
  const value = (input?.value || '').trim();
  return value ? submitTransformInstruction(value) : applyTransformOption();
}

async function formatDraft() {
  if (!beginEditorCommand('format draft')) return;
  const draftEditor = document.getElementById('draft-editor');
  // Restore-what-you-saved: remember the readOnly value this command is about
  // to overwrite, so cleanup can hand back exactly that and never unlatch a
  // journal-dead editor.
  // INTENT: EDITOR-JDEAD-009
  // @spec EDITOR-JDEAD-009
  const readOnlyBeforeCommand = draftEditor ? draftEditor.readOnly === true : false;
  if (draftEditor) draftEditor.readOnly = true;
  try {
    const sync = (typeof collectDraftSync === 'function') ? collectDraftSync() : null;
    // INTENT: EDITOR-SNAP-011
    // @spec EDITOR-SNAP-011
    if (!sync) {
      showNotification('Format blocked: the visible editor snapshot is unavailable.', true, 9000);
      return;
    }
    const response = await postJSON('/api/transform/format', {sync});
    // A Format 409 is the OPPOSITE of a failed transform: the server refused
    // because the visible snapshot is stale, and that is resolvable.
    // INTENT: EDITOR-CONF-005
    // @spec EDITOR-CONF-005
    if (response.status === 409) {
      const raised = await raiseConflictFromResponse(response);
      if (raised === 'shown') {
        showNotification('FORMAT BLOCKED—visible text is preserved. Choose an action in the red banner.', true, 12000);
      } else if (raised === 'unusable') {
        showNotification('FORMAT REFUSED—the server rejected it and its conflict could not be displayed. Your visible text is unchanged.', true, 20000);
      }
      return;
    }
    if (response.status !== 200) {
      showNotification('Format was not committed; visible text remains intact.', true, 10000);
      return;
    }
    const frame = await response.json();
    if (!applyAuthoritativeEditorFrame(frame, 'accepted-operation')) {
      showNotification('Format committed, but its editor frame was invalid. Reload before editing.', true, 12000);
      return;
    }
    if (typeof acknowledgeDurableDraftJournal === 'function') {
      acknowledgeDurableDraftJournal(response);
    }
    // INTENT: EDITOR-DURA-007
    // @spec EDITOR-DURA-007
    if (typeof editorCommandWasDurablySaved === 'function' &&
        !editorCommandWasDurablySaved(response, sync)) {
      showNotification('Format committed, but the response carried no durable receipt for your exact text. Keep this page open and try again.', true, 15000);
      return;
    }
    showNotification('Formatted and saved.');
  } catch (error) {
    console.error('[FormatDraft]', error);
    showNotification('Format failed; visible text remains intact.', true, 9000);
  } finally {
    const currentDraftEditor = document.getElementById('draft-editor');
    // INTENT: EDITOR-JDEAD-009
    // @spec EDITOR-JDEAD-009
    if (currentDraftEditor) {
      currentDraftEditor.readOnly = readOnlyBeforeCommand ||
        (typeof draftJournalDead === 'function' && draftJournalDead());
    }
    endEditorCommand();
  }
}


// INTENT: EDITOR-DEQUOTE-016
// @spec EDITOR-DEQUOTE-016
async function dequoteFormatSelection() {
  const draftEditor = document.getElementById('draft-editor');
  if (!draftEditor || draftEditor.selectionStart === draftEditor.selectionEnd) {
    showNotification('Select text to Dequote/Format first.', true, 9000);
    return false;
  }
  const selection = {start: draftEditor.selectionStart, end: draftEditor.selectionEnd};
  if (!beginEditorCommand('dequote/format selection')) return false;
  const readOnlyBeforeCommand = draftEditor.readOnly === true;
  draftEditor.readOnly = true;
  hideSelectionToolbar();
  try {
    const sync = (typeof collectDraftSync === 'function') ? collectDraftSync() : null;
    if (!sync) {
      showNotification('Dequote/Format blocked: the visible editor snapshot is unavailable.', true, 9000);
      return false;
    }
    const response = await postJSON('/api/transform/format', {sync, selection});
    if (response.status === 409) {
      const raised = await raiseConflictFromResponse(response);
      if (raised === 'shown') {
        showNotification('DEQUOTE/FORMAT BLOCKED—visible text is preserved. Choose an action in the red banner.', true, 12000);
      } else if (raised === 'unusable') {
        showNotification('DEQUOTE/FORMAT REFUSED—the server rejected it and its conflict could not be displayed. Your visible text is unchanged.', true, 20000);
      }
      return false;
    }
    if (response.status !== 200) {
      showNotification('Dequote/Format was not committed; visible text remains intact.', true, 10000);
      return false;
    }
    const frame = await response.json();
    if (!applyAuthoritativeEditorFrame(frame, 'accepted-operation')) {
      showNotification('Dequote/Format committed, but its editor frame was invalid. Reload before editing.', true, 12000);
      return false;
    }
    if (typeof acknowledgeDurableDraftJournal === 'function') {
      acknowledgeDurableDraftJournal(response);
    }
    if (typeof editorCommandWasDurablySaved === 'function' &&
        !editorCommandWasDurablySaved(response, sync)) {
      showNotification('Dequote/Format committed, but the response carried no durable receipt for your exact text. Keep this page open and try again.', true, 15000);
      return false;
    }
    const currentDraftEditor = document.getElementById('draft-editor');
    if (currentDraftEditor && Number.isInteger(frame['selection-start']) &&
        Number.isInteger(frame['selection-end'])) {
      currentDraftEditor.setSelectionRange(frame['selection-start'], frame['selection-end']);
      currentDraftEditor.focus({preventScroll: true});
    }
    showNotification('Dequoted, formatted, and saved.');
    return true;
  } catch (error) {
    console.error('[DequoteFormatSelection]', error);
    showNotification('Dequote/Format failed; visible text remains intact.', true, 9000);
    return false;
  } finally {
    const currentDraftEditor = document.getElementById('draft-editor');
    if (currentDraftEditor) {
      currentDraftEditor.readOnly = readOnlyBeforeCommand ||
        (typeof draftJournalDead === 'function' && draftJournalDead());
    }
    endEditorCommand();
  }
}
// Capture phase makes Transform shortcuts safe even when a later legacy
// listener fails. Physical key codes also prevent macOS Option-key glyphs
// from being inserted into the active textarea.
document.addEventListener('keydown', function (event) {
  if (handleDraftPreviewShortcut(event)) return;
  if (handleCherryPickShortcut(event)) return;
  if (event.key === 'Escape') {
    hideSelectionToolbar();
  }
  if (event.altKey && event.code === 'KeyT') {
    event.preventDefault();
    event.stopImmediatePropagation();
    openTransformFromSelection();
    return;
  }

  if (event.altKey && event.code === 'KeyJ') {
    const modal = document.getElementById('transform-modal');
    if (!modal || modal.style.display === 'none') return;
    event.preventDefault();
    event.stopImmediatePropagation();
    submitTransformInstruction('Make it better');
  }
}, true);

document.addEventListener('change', handleDraftViewModeChange);

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', installDraftPreviewUi, {once: true});
} else {
  installDraftPreviewUi();
}

document.addEventListener('selectionchange', scheduleSelectionToolbar);
document.addEventListener('mouseup', event => {
  if (event.target?.id === 'draft-editor') scheduleSelectionToolbar();
});
document.addEventListener('keyup', event => {
  if (event.target?.id === 'draft-editor') scheduleSelectionToolbar();
});
document.addEventListener('mousedown', event => {
  const toolbar = document.getElementById('selection-toolbar');
  if (toolbar?.contains(event.target) || event.target?.id === 'draft-editor') return;
  hideSelectionToolbar();
});
document.addEventListener('scroll', () => {
  const toolbar = document.getElementById('selection-toolbar');
  if (toolbar?.style.display === 'flex') scheduleSelectionToolbar();
}, true);
