const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

test('datastar runtime parses in the shared classic-script global scope', () => {
  const source = fs.readFileSync('resources/public/js/datastar-runtime.js', 'utf8');
  const context = vm.createContext({});

  // Legacy pages may already expose this timer as a global var. A top-level
  // lexical declaration would abort the whole runtime before Transform and
  // the execute-script bridge become available.
  vm.runInContext('var _notifyTimer = null;', context);

  try {
    vm.runInContext(source, context);
  } catch (error) {
    assert.notEqual(error.name, 'SyntaxError', error.stack);
    // This test exercises global parsing, not DOM behavior. The empty VM has
    // no document, so a post-parse ReferenceError is expected and harmless.
  }
});

test('Option-T is captured and opens Transform instead of typing a dagger', () => {
  const source = fs.readFileSync('resources/public/js/editor-commands.js', 'utf8');
  const listeners = [];
  const posts = [];
  const editor = {value: 'before selected after', selectionStart: 7, selectionEnd: 15};
  const context = vm.createContext({
    console,
    window: {},
    document: {
      getElementById: id => id === 'draft-editor' ? editor : null,
      addEventListener: (type, listener, capture) => listeners.push({type, listener, capture})
    },
    postJSON: (url, body) => posts.push({url, body}),
    showNotification: () => {},
    beginEditorCommand: () => true,
    endEditorCommand: () => {},
    applyAuthoritativeEditorFrame: () => true
  });
  context.window = context;
  vm.runInContext(source, context);

  const shortcut = listeners.find(x => x.type === 'keydown');
  let prevented = false;
  let stopped = false;
  shortcut.listener({
    altKey: true,
    code: 'KeyT',
    preventDefault: () => { prevented = true; },
    stopImmediatePropagation: () => { stopped = true; }
  });

  assert.equal(shortcut.capture, true);
  assert.equal(prevented, true);
  assert.equal(stopped, true);
  assert.equal(posts.length, 1);
  assert.equal(posts[0].url, '/api/transform/open');
  assert.equal(posts[0].body.selected, 'selected');
});

test('Format applies the HTTP response frame without any SSE callback', async () => {
  const source = fs.readFileSync('resources/public/js/editor-commands.js', 'utf8');
  const editor = {value: 'visible text', readOnly: false};
  const frame = {'draft': 'formatted text', 'state-version': 8,
                 'editor-sync-key': 'book-node:n1'};
  let applied = null;
  const context = vm.createContext({
    console,
    window: {},
    document: {
      getElementById: id => id === 'draft-editor' ? editor : null,
      addEventListener: () => {}
    },
    beginEditorCommand: () => true,
    endEditorCommand: () => {},
    collectDraftSync: () => ({draft: 'visible text', 'state-version': 7,
                              'editor-sync-key': 'book-node:n1'}),
    postJSON: async () => ({status: 200, json: async () => frame}),
    applyAuthoritativeEditorFrame: (received, reason) => {
      applied = {received, reason};
      return true;
    },
    showNotification: () => {}
  });
  context.window = context;
  vm.runInContext(source, context);

  await context.formatDraft();

  assert.deepEqual(applied, {received: frame, reason: 'accepted-operation'});
  assert.equal(editor.readOnly, false);
});

test('Make it better uses one guarded Transform instruction request', async () => {
  const source = fs.readFileSync('resources/public/js/editor-commands.js', 'utf8');
  const listeners = [];
  const posts = [];
  const input = {
    value: 'Make it better',
    attrs: {},
    setAttribute(name, value) { this.attrs[name] = value; }
  };
  const button = {disabled: false};
  const submit = {disabled: false};
  let resolvePost;
  const response = new Promise(resolve => { resolvePost = resolve; });
  const context = vm.createContext({
    console,
    window: {},
    document: {
      body: {dataset: {}},
      getElementById: id => id === 'transform-input' ? input
        : id === 'transform-make-better' ? button
        : id === 'transform-submit' ? submit
        : null,
      addEventListener: (type, listener, capture) => listeners.push({type, listener, capture})
    },
    postJSON: (url, body) => {
      posts.push({url, body});
      return response;
    },
    showNotification: () => {},
    beginEditorCommand: () => true,
    endEditorCommand: () => {},
    applyAuthoritativeEditorFrame: () => true
  });
  context.window = context;
  vm.runInContext(source, context);

  const first = context.submitTransformInstruction('Make it better');
  const duplicate = await context.submitTransformInstruction('Make it better');
  assert.equal(duplicate, false);
  assert.equal(posts.length, 1);
  assert.equal(posts[0].url, '/api/transform');
  assert.equal(posts[0].body.instruction, 'Make it better');
  assert.deepEqual(Object.keys(posts[0].body), ['instruction']);
  assert.equal(button.disabled, true);
  assert.equal(submit.disabled, true);
  assert.equal(input.attrs['aria-busy'], 'true');

  resolvePost({ok: true});
  assert.equal(await first, true);
  assert.equal(input.value, '');
  assert.equal(button.disabled, false);
  assert.equal(submit.disabled, false);
  assert.equal(input.attrs['aria-busy'], 'false');

  let applies = 0;
  context.applyTransformOption = () => { applies += 1; };
  input.value = '';
  context.submitTransformInput();
  assert.equal(applies, 1);

  input.value = 'Tighten this';
  assert.equal(await context.submitTransformInput(), true);
  assert.equal(posts.at(-1).body.instruction, 'Tighten this');

  context.document.body.dataset.editorCommandPending = 'true';
  assert.equal(await context.submitTransformInstruction('Make it better'), false);
  assert.equal(posts.length, 2);
});

test('Alt-J invokes Make it better only while Transform is visible', async () => {
  const source = fs.readFileSync('resources/public/js/editor-commands.js', 'utf8');
  const listeners = [];
  const posts = [];
  const modal = {style: {display: 'none'}};
  const button = {disabled: false};
  const submit = {disabled: false};
  const input = {
    value: '',
    setAttribute() {}
  };
  const context = vm.createContext({
    console,
    window: {},
    document: {
      body: {dataset: {}},
      getElementById: id => id === 'transform-modal' ? modal
        : id === 'transform-make-better' ? button
        : id === 'transform-submit' ? submit
        : id === 'transform-input' ? input
        : null,
      addEventListener: (type, listener, capture) => listeners.push({type, listener, capture})
    },
    postJSON: async (url, body) => {
      posts.push({url, body});
      return {ok: true};
    },
    showNotification: () => {},
    beginEditorCommand: () => true,
    endEditorCommand: () => {},
    applyAuthoritativeEditorFrame: () => true
  });
  context.window = context;
  vm.runInContext(source, context);

  const shortcut = listeners.find(x => x.type === 'keydown');
  let prevented = 0;
  let stopped = 0;
  const event = {
    altKey: true,
    code: 'KeyJ',
    preventDefault: () => { prevented += 1; },
    stopImmediatePropagation: () => { stopped += 1; }
  };

  shortcut.listener(event);
  assert.equal(posts.length, 0);
  assert.equal(prevented, 0);
  assert.equal(stopped, 0);

  modal.style.display = 'flex';
  shortcut.listener(event);
  await new Promise(resolve => setImmediate(resolve));

  assert.equal(shortcut.capture, true);
  assert.equal(prevented, 1);
  assert.equal(stopped, 1);
  assert.equal(posts.length, 1);
  assert.equal(posts[0].url, '/api/transform');
  assert.equal(posts[0].body.instruction, 'Make it better');
  assert.deepEqual(Object.keys(posts[0].body), ['instruction']);
});
