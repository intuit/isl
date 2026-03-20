# Features to Add (from isl-language extension)

Features the isl-language extension has that we need to add to this plugin.

---

## 1. Lint-on-save only

- **Add:** Option to run validation **only on save** (and optionally on open), not on every text change.
- **Config:** e.g. `isl.validation.lintOnSave` (boolean). When `true`, validate on save/open only; when `false`, keep current debounced-on-typing behavior.

---

## 2. Exclude patterns

- **Add:** Glob patterns to **exclude files** from linting (e.g. `**/node_modules/**`, `**/generated/**`).
- **Config:** e.g. `isl.validation.exclude` (string[]). Skip validation when the document path matches any pattern.

---

## 3. Grammar-based syntax validation

- **Add:** Syntax diagnostics from a **formal grammar** (e.g. ANTLR) so all syntax errors (unexpected tokens, missing brackets, etc.) are caught in one place with consistent positions and messages. Keep existing semantic checks (undefined refs, etc.) on top.

---

## 4. Diagnostic related information

- **Add:** Attach **related information** to diagnostics (e.g. rule id) so the editor can show “Rule: XYZ” or similar.

---

## 5. CLI linter

- **Add:** A **standalone CLI** that lints ISL files from the command line (e.g. `isl-lint file.isl` or `isl-validate file.isl`) for CI and scripts. It should accept file paths, output diagnostics (human-readable or JSON), and exit non-zero when there are errors.

---

## 6. NPX install

- **Add:** A way to **install this extension via npx** without cloning (e.g. `npx isl-language-support install-extension --vscode` / `--cursor`) for users who don’t use the marketplace.

---

## 7. “Lint file” command

- **Add:** A **command** that forces a lint/validation refresh for the current file (e.g. “Lint file” or “ISL: Lint file”), so users can re-run validation on demand.
