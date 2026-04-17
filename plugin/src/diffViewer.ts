import * as vscode from 'vscode';
import * as yaml from 'js-yaml';

/** Show the ISL Result Comparison Viewer webview panel with the full expected/actual payloads. */
export function showResultDiffViewer(testName: string, expected: string, actual: string): void {
    const panel = vscode.window.createWebviewPanel(
        'islResultDiff',
        `ISL Diff: ${testName}`,
        vscode.ViewColumn.Beside,
        { enableScripts: true, retainContextWhenHidden: true, localResourceRoots: [] }
    );
    panel.webview.html = buildDiffViewerHtml(testName, expected, actual);
}

// ---------------------------------------------------------------------------
// Diff computation
// ---------------------------------------------------------------------------

type DiffLineType = 'same' | 'removed' | 'added' | 'empty';

interface DiffLine {
    text: string;
    type: DiffLineType;
}

/**
 * Compute a side-by-side line diff using LCS. Returns parallel left/right arrays
 * where "removed" lines appear on the left, "added" on the right, and "empty"
 * is used to keep the two columns vertically aligned.
 */
function computeLineDiff(leftLines: string[], rightLines: string[]): { left: DiffLine[]; right: DiffLine[] } {
    const m = leftLines.length;
    const n = rightLines.length;

    const dp: number[][] = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0));
    for (let i = 1; i <= m; i++) {
        for (let j = 1; j <= n; j++) {
            dp[i][j] = leftLines[i - 1] === rightLines[j - 1]
                ? dp[i - 1][j - 1] + 1
                : Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
    }

    type Op = { type: 'same'; text: string } | { type: 'left'; text: string } | { type: 'right'; text: string };
    const ops: Op[] = [];
    let i = m, j = n;
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && leftLines[i - 1] === rightLines[j - 1]) {
            ops.unshift({ type: 'same', text: leftLines[i - 1] });
            i--; j--;
        } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            ops.unshift({ type: 'right', text: rightLines[j - 1] });
            j--;
        } else {
            ops.unshift({ type: 'left', text: leftLines[i - 1] });
            i--;
        }
    }

    // Pair adjacent left/right runs so rows align visually
    const leftResult: DiffLine[] = [];
    const rightResult: DiffLine[] = [];
    let k = 0;
    while (k < ops.length) {
        const op = ops[k];
        if (op.type === 'same') {
            leftResult.push({ text: op.text, type: 'same' });
            rightResult.push({ text: op.text, type: 'same' });
            k++;
        } else {
            const lefts: string[] = [];
            const rights: string[] = [];
            while (k < ops.length && ops[k].type !== 'same') {
                if (ops[k].type === 'left') lefts.push(ops[k].text);
                else rights.push(ops[k].text);
                k++;
            }
            const maxLen = Math.max(lefts.length, rights.length);
            for (let p = 0; p < maxLen; p++) {
                leftResult.push(p < lefts.length
                    ? { text: lefts[p], type: 'removed' }
                    : { text: '', type: 'empty' });
                rightResult.push(p < rights.length
                    ? { text: rights[p], type: 'added' }
                    : { text: '', type: 'empty' });
            }
        }
    }

    return { left: leftResult, right: rightResult };
}

// ---------------------------------------------------------------------------
// Value formatting
// ---------------------------------------------------------------------------

/**
 * Recursively sort object keys so that field-order differences between two
 * semantically equal JSON objects don't produce spurious diff lines.
 * Array element order is preserved (arrays are ordered by definition).
 */
function canonicalize(value: unknown): unknown {
    if (value === null || typeof value !== 'object') return value;
    if (Array.isArray(value)) return value.map(canonicalize);
    const obj = value as Record<string, unknown>;
    const sorted: Record<string, unknown> = {};
    for (const key of Object.keys(obj).sort()) {
        sorted[key] = canonicalize(obj[key]);
    }
    return sorted;
}

/** Pretty-print a JSON or YAML value with canonicalized key order. */
function tryFormatValue(raw: string): string {
    const trimmed = raw.trim();
    try {
        return JSON.stringify(canonicalize(JSON.parse(trimmed)), null, 2);
    } catch { /* not JSON */ }
    try {
        const parsed = yaml.load(trimmed);
        if (parsed !== null && typeof parsed === 'object') {
            return JSON.stringify(canonicalize(parsed as object), null, 2);
        }
    } catch { /* not YAML */ }
    return trimmed;
}

/** Returns true if value is valid JSON (any type — object, array, string, number, boolean, null). */
function isJsonValue(raw: string): boolean {
    const t = raw.trim();
    if (!t) return false;
    try { JSON.parse(t); return true; } catch { return false; }
}

// ---------------------------------------------------------------------------
// HTML / syntax highlighting helpers
// ---------------------------------------------------------------------------

function escapeHtml(s: string): string {
    return s
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

/**
 * Tokenize and syntax-highlight one line of pretty-printed JSON.
 * Input is raw (not HTML-escaped); output is safe HTML with span tags.
 */
function highlightJsonLine(line: string): string {
    const result: string[] = [];
    const len = line.length;
    let i = 0;

    while (i < len) {
        const ch = line[i];

        if (ch === ' ' || ch === '\t') { result.push(ch); i++; continue; }

        // String token
        if (ch === '"') {
            let j = i + 1;
            while (j < len) {
                if (line[j] === '\\') { j += 2; continue; }
                if (line[j] === '"') { j++; break; }
                j++;
            }
            const str = line.substring(i, j);
            let k = j;
            while (k < len && line[k] === ' ') k++;
            const cls = k < len && line[k] === ':' ? 'jk' : 'js';
            result.push(`<span class="${cls}">${escapeHtml(str)}</span>`);
            i = j;
            continue;
        }

        // Number
        if (ch === '-' || (ch >= '0' && ch <= '9')) {
            let j = i;
            if (line[j] === '-') j++;
            while (j < len && /[\d.eE+\-]/.test(line[j])) j++;
            result.push(`<span class="jn">${escapeHtml(line.substring(i, j))}</span>`);
            i = j;
            continue;
        }

        // Keywords
        const rest = line.substring(i);
        if (rest.startsWith('true'))  { result.push('<span class="jb">true</span>');  i += 4; continue; }
        if (rest.startsWith('false')) { result.push('<span class="jb">false</span>'); i += 5; continue; }
        if (rest.startsWith('null'))  { result.push('<span class="jl">null</span>');  i += 4; continue; }

        result.push(escapeHtml(ch));
        i++;
    }

    return result.join('');
}

function renderLine(line: DiffLine, isJson: boolean): string {
    const cls = line.type !== 'same' ? ` ${line.type}` : '';
    const content = line.text === ''
        ? '&nbsp;'
        : (isJson ? highlightJsonLine(line.text) : escapeHtml(line.text));
    return `<div class="dl${cls}">${content}</div>`;
}

function generateNonce(): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    return Array.from({ length: 32 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}

// ---------------------------------------------------------------------------
// HTML builder
// ---------------------------------------------------------------------------

function buildDiffViewerHtml(testName: string, rawExpected: string, rawActual: string): string {
    const nonce = generateNonce();

    const expFormatted = tryFormatValue(rawExpected);
    const actFormatted = tryFormatValue(rawActual);
    const json = isJsonValue(rawExpected) || isJsonValue(rawActual);

    const expLines = expFormatted.split('\n');
    const actLines = actFormatted.split('\n');
    const { left, right } = computeLineDiff(expLines, actLines);

    const leftHtml  = left.map(l  => renderLine(l,  json)).join('');
    const rightHtml = right.map(r => renderLine(r, json)).join('');

    const changedRows = left.filter(l => l.type !== 'same' && l.type !== 'empty').length;
    const badge = changedRows === 0
        ? 'no differences'
        : changedRows === 1 ? '1 changed line' : `${changedRows} changed lines`;

    return /* html */`<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-${nonce}';">
<meta name="color-scheme" content="dark light">
<title>ISL Diff: ${escapeHtml(testName)}</title>
<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}

:root {
  --c-added-bg:    rgba(35,134,54,.14);
  --c-added-bar:   #3fb950;
  --c-removed-bg:  rgba(248,81,73,.14);
  --c-removed-bar: #f85149;
  --c-empty-bg:    rgba(127,127,127,.04);
  --c-border:      var(--vscode-panel-border, #3c3c3c);
  --c-sidebar:     var(--vscode-sideBar-background, #252526);
  --font-mono:     var(--vscode-editor-font-family, 'Consolas','Courier New',monospace);
  --font-size-mono:var(--vscode-editor-font-size, 12px);
  --line-h:        1.55;
}

html, body { height: 100%; margin: 0; overflow: hidden; }
body {
  font-family: var(--vscode-font-family,-apple-system,'Segoe UI',sans-serif);
  background: var(--vscode-editor-background, #1e1e1e);
  color: var(--vscode-editor-foreground, #d4d4d4);
  font-size: 13px;
  line-height: 1.6;
  display: flex;
  flex-direction: column;
}

/* Root: header + search + legend fixed; only comparison scrolls */
.diff-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.diff-top {
  flex-shrink: 0;
  padding: 16px 20px 0;
  border-bottom: 1px solid var(--c-border);
  background: var(--vscode-editor-background, #1e1e1e);
}
.diff-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px 20px 20px;
}

/* ── page header ─────────────────────────────────────────────────────────── */
.hdr {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
  display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap;
}
.hdr-title { font-size: 15px; font-weight: 600; color: var(--vscode-foreground, #ccc); }
.hdr-test  {
  font-size: 13px; color: var(--vscode-descriptionForeground, #9d9d9d);
  font-style: italic; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.badge {
  display: inline-flex; align-items: center;
  background: rgba(248,81,73,.18); color: #f85149;
  border-radius: 10px; padding: 1px 9px; font-size: 11px; font-weight: 600; white-space: nowrap;
}
.badge.ok { background: rgba(35,134,54,.18); color: #3fb950; }

/* ── search bar ──────────────────────────────────────────────────────────── */
.sbar {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 12px;
  background: var(--vscode-input-background, #3c3c3c);
  border: 1px solid var(--vscode-input-border, #555);
  border-radius: 4px; padding: 4px 8px;
}
.sbar:focus-within { border-color: var(--vscode-focusBorder, #007fd4); outline: none; }
.s-ico {
  width: 13px; height: 13px; flex-shrink: 0;
  color: var(--vscode-descriptionForeground, #9d9d9d); fill: currentColor;
}
#si {
  flex: 1; background: transparent; border: none; outline: none; min-width: 0;
  color: var(--vscode-input-foreground, #cccccc);
  font-family: var(--vscode-font-family, sans-serif); font-size: 13px;
}
#si::placeholder { color: var(--vscode-input-placeholderForeground, #6e6e6e); }
#sc {
  font-size: 11px; color: var(--vscode-descriptionForeground, #9d9d9d);
  white-space: nowrap; min-width: 52px; text-align: right;
}
#sc.no-match { color: #f85149; }
.sbtn {
  background: transparent; border: 1px solid transparent; border-radius: 3px;
  color: var(--vscode-foreground, #ccc); cursor: pointer; padding: 2px 7px;
  font-size: 14px; line-height: 1; user-select: none;
}
.sbtn:hover:not(:disabled) { background: var(--vscode-toolbar-hoverBackground, rgba(255,255,255,.1)); }
.sbtn:disabled { opacity: .35; cursor: default; }

/* ── search highlights ───────────────────────────────────────────────────── */
mark.sm {
  background: rgba(234,179,8,.28); color: inherit;
  border-radius: 2px; padding: 0 1px;
}
mark.sm-cur {
  background: rgba(234,179,8,.72);
  outline: 1px solid rgba(234,179,8,.9);
}

/* ── diff card ───────────────────────────────────────────────────────────── */
.card { border: 1px solid var(--c-border); border-radius: 6px; overflow: hidden; }
.card-cols { display: grid; grid-template-columns: 1fr 1fr; }
.pane { overflow: hidden; min-width: 0; }
.pane-exp { border-right: 1px solid var(--c-border); }

.pane-hdr {
  display: flex; align-items: center; justify-content: space-between;
  padding: 5px 14px; border-bottom: 1px solid var(--c-border);
  font-size: 11px; font-weight: 600; letter-spacing: .4px; text-transform: uppercase;
}
.hdr-exp { background: rgba(63,185,80,.08); color: var(--c-added-bar); }
.hdr-act { background: rgba(248,81,73,.08); color: var(--c-removed-bar); }
.pane-count { font-weight: 400; opacity: .75; font-size: 10px; text-transform: none; }

/* ── code lines ──────────────────────────────────────────────────────────── */
.lines {
  font-family: var(--font-mono);
  font-size: var(--font-size-mono);
  line-height: var(--line-h);
  padding: 6px 0;
  overflow-x: auto;
  white-space: pre;
}
.dl {
  display: block; padding: 0 14px;
  min-height: calc(var(--font-size-mono) * var(--line-h));
}
.dl.removed { background: var(--c-removed-bg); border-left: 3px solid var(--c-removed-bar); padding-left: 11px; }
.dl.added   { background: var(--c-added-bg);   border-left: 3px solid var(--c-added-bar);   padding-left: 11px; }
.dl.empty   { background: var(--c-empty-bg); }

/* ── JSON syntax highlighting ────────────────────────────────────────────── */
.jk { color: var(--vscode-symbolIcon-propertyForeground,
              var(--vscode-editor-foreground, #9cdcfe)); }
.js { color: var(--vscode-debugTokenExpression-string,  #ce9178); }
.jn { color: var(--vscode-debugTokenExpression-number,  #b5cea8); }
.jb { color: var(--vscode-debugTokenExpression-boolean, #569cd6); }
.jl { color: var(--vscode-debugTokenExpression-error,   #f44747); }

/* ── legend ──────────────────────────────────────────────────────────────── */
.legend {
  display: flex; gap: 16px; margin-bottom: 12px;
  font-size: 11px; color: var(--vscode-descriptionForeground, #9d9d9d); flex-wrap: wrap;
}
.legend-item { display: flex; align-items: center; gap: 5px; }
.legend-swatch { width: 12px; height: 12px; border-radius: 2px; border-left: 3px solid; display: inline-block; }
.swatch-exp { background: var(--c-removed-bg); border-color: var(--c-removed-bar); }
.swatch-act { background: var(--c-added-bg);   border-color: var(--c-added-bar); }
</style>
</head>
<body>
<div class="diff-root">
  <div class="diff-top">
    <div class="hdr">
      <span class="hdr-title">Result Comparison</span>
      <span class="hdr-test">${escapeHtml(testName)}</span>
      <span class="badge${changedRows === 0 ? ' ok' : ''}">${escapeHtml(badge)}</span>
    </div>
    <div class="sbar" id="sbar">
      <svg class="s-ico" viewBox="0 0 16 16"><path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1 1 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0"/></svg>
      <input id="si" type="text" autocomplete="off" spellcheck="false" placeholder="Search in diff…  (Ctrl+F, F3 / Shift+F3)">
      <span id="sc"></span>
      <button id="sp" class="sbtn" title="Previous match (Shift+F3)" disabled>↑</button>
      <button id="sn" class="sbtn" title="Next match (F3)" disabled>↓</button>
      <button id="sx" class="sbtn" title="Clear search (Esc)">✕</button>
    </div>
    <div class="legend">
      <span class="legend-item"><span class="legend-swatch swatch-exp"></span>Expected (differs)</span>
      <span class="legend-item"><span class="legend-swatch swatch-act"></span>Actual (differs)</span>
    </div>
  </div>
  <div class="diff-scroll" id="diff-scroll">
    <div class="card">
      <div class="card-cols">
        <div class="pane pane-exp">
          <div class="pane-hdr hdr-exp">
            <span class="pane-label">Expected</span>
            <span class="pane-count">${expLines.length} line${expLines.length !== 1 ? 's' : ''}</span>
          </div>
          <pre class="lines" id="pane-left">${leftHtml}</pre>
        </div>
        <div class="pane pane-act">
          <div class="pane-hdr hdr-act">
            <span class="pane-label">Actual</span>
            <span class="pane-count">${actLines.length} line${actLines.length !== 1 ? 's' : ''}</span>
          </div>
          <pre class="lines" id="pane-right">${rightHtml}</pre>
        </div>
      </div>
    </div>
  </div>
</div>

<script nonce="${nonce}">
(function () {
  'use strict';

  // ── horizontal scroll sync ────────────────────────────────────────────────
  var pL = document.getElementById('pane-left');
  var pR = document.getElementById('pane-right');
  var _syncing = false;
  function syncScroll(src, dst) {
    return function () {
      if (_syncing) return;
      _syncing = true;
      dst.scrollLeft = src.scrollLeft;
      requestAnimationFrame(function () { _syncing = false; });
    };
  }
  if (pL && pR) {
    pL.addEventListener('scroll', syncScroll(pL, pR));
    pR.addEventListener('scroll', syncScroll(pR, pL));
  }

  // ── search engine ─────────────────────────────────────────────────────────
  var marks    = [];   // all <mark> nodes in DOM order
  var curIdx   = -1;

  /** Remove all <mark class="sm"> wrappers, restoring original text nodes. */
  function clearMarks() {
    // query fresh each time – live NodeList is tricky, use a static copy
    var ms = Array.prototype.slice.call(document.querySelectorAll('mark.sm'));
    ms.forEach(function (m) {
      var p = m.parentNode;
      if (!p) return;
      while (m.firstChild) p.insertBefore(m.firstChild, m);
      p.removeChild(m);
      p.normalize();
    });
    marks = [];
    curIdx = -1;
  }

  /** Wrap all occurrences of [term] inside .dl text nodes with <mark class="sm">. */
  function applySearch(term) {
    var lower = term.toLowerCase();
    var lines = document.querySelectorAll('.dl');
    lines.forEach(function (line) {
      // Collect text nodes first (TreeWalker is invalidated by DOM mutations)
      var walker = document.createTreeWalker(line, NodeFilter.SHOW_TEXT, null);
      var tnodes = [];
      var n;
      while ((n = walker.nextNode())) tnodes.push(n);

      tnodes.forEach(function (tn) {
        var text = tn.textContent || '';
        var lo   = text.toLowerCase();
        var idx  = lo.indexOf(lower);
        if (idx === -1) return;           // fast exit — no match in this node

        var frag = document.createDocumentFragment();
        var pos  = 0;
        while (idx !== -1) {
          if (idx > pos) frag.appendChild(document.createTextNode(text.slice(pos, idx)));
          var mk = document.createElement('mark');
          mk.className = 'sm';
          mk.textContent = text.slice(idx, idx + term.length);
          frag.appendChild(mk);
          marks.push(mk);
          pos = idx + term.length;
          idx = lo.indexOf(lower, pos);
        }
        if (pos < text.length) frag.appendChild(document.createTextNode(text.slice(pos)));
        tn.parentNode.replaceChild(frag, tn);
      });
    });
  }

  /** Scroll the active mark into view inside .diff-scroll. */
  function scrollToMark(mk) {
    if (!mk) return;
    var scrollEl = document.getElementById('diff-scroll');
    if (scrollEl) {
      mk.scrollIntoView({ block: 'nearest', behavior: 'smooth', inline: 'nearest' });
    } else {
      mk.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
  }

  function setActive(idx) {
    if (marks.length === 0) return;
    if (curIdx >= 0 && marks[curIdx]) marks[curIdx].classList.remove('sm-cur');
    curIdx = (idx + marks.length) % marks.length;
    marks[curIdx].classList.add('sm-cur');
    scrollToMark(marks[curIdx]);
    updateUI();
  }

  function runSearch(term) {
    clearMarks();
    if (term) applySearch(term);
    if (marks.length > 0) setActive(0); else updateUI();
  }

  function updateUI() {
    var input = document.getElementById('si');
    var ctr   = document.getElementById('sc');
    var btnP  = document.getElementById('sp');
    var btnN  = document.getElementById('sn');
    var val   = input ? input.value : '';

    if (!val) {
      ctr.textContent = '';
      ctr.className = '';
    } else if (marks.length === 0) {
      ctr.textContent = 'no matches';
      ctr.className = 'no-match';
    } else {
      ctr.textContent = (curIdx + 1) + ' / ' + marks.length;
      ctr.className = '';
    }
    btnP.disabled = marks.length === 0;
    btnN.disabled = marks.length === 0;
  }

  // ── wire up UI ────────────────────────────────────────────────────────────
  var inp  = document.getElementById('si');
  var btnP = document.getElementById('sp');
  var btnN = document.getElementById('sn');
  var btnX = document.getElementById('sx');

  var searchTimer = null;
  inp.addEventListener('input', function () {
    clearTimeout(searchTimer);
    var val = inp.value;
    searchTimer = setTimeout(function () { runSearch(val); }, 80);
  });

  inp.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (marks.length === 0) { runSearch(inp.value); return; }
      setActive(e.shiftKey ? curIdx - 1 : curIdx + 1);
    }
    if (e.key === 'Escape') { inp.value = ''; clearMarks(); updateUI(); }
  });

  btnP.addEventListener('click', function () { if (marks.length > 0) setActive(curIdx - 1); });
  btnN.addEventListener('click', function () { if (marks.length > 0) setActive(curIdx + 1); });
  btnX.addEventListener('click', function () { inp.value = ''; clearMarks(); updateUI(); inp.focus(); });

  // Global shortcuts: F3 (Find Next), Shift+F3 (Find Previous), Ctrl/Cmd+F (focus search)
  document.addEventListener('keydown', function (e) {
    if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
      e.preventDefault();
      inp.focus();
      inp.select();
      return;
    }
    if (e.key === 'F3') {
      e.preventDefault();
      if (marks.length === 0 && inp.value) runSearch(inp.value);
      if (marks.length > 0) setActive(e.shiftKey ? curIdx - 1 : curIdx + 1);
    }
  });

  updateUI();
}());
</script>
</body>
</html>`;
}
