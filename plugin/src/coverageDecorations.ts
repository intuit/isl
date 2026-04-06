import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';

/** Master switch for in-editor coverage UI (run/test still can skip --coverage-report when false). */
export function islCoverageUiEnabled(): boolean {
    return vscode.workspace.getConfiguration('isl.coverage').get<boolean>('enabled', true);
}

/** Module names in coverage match ISL import paths (e.g. ./child.isl, ../lib/x.isl), not always workspace-relative paths. */
function normalizeIslModuleKey(key: string): string {
    let s = key.replace(/\\/g, '/');
    while (s.startsWith('./')) {
        s = s.slice(2);
    }
    return s;
}

function islModulePathBasename(moduleKey: string): string {
    return path.posix.basename(normalizeIslModuleKey(moduleKey));
}

/** Must match `CoverageReportJson.COVERAGE_REPORT_VERSION` in isl-cmd. */
const ISL_COVERAGE_JSON_VERSION = 1;

/**
 * Wrapper commands whose parser span covers the whole `prop: if … else …` (or whole assignment) while only
 * one branch may run. Exclude from: (1) green hit highlights, (2) per-line gutter `allHit` / `anyHit` counts
 * so a line is not "fully covered" just because the wrapper ran.
 */
const STATEMENT_KINDS_SKIP_WIDE_COVERAGE_SPAN = new Set<string>([
    'AssignPropertyCommand',
    'AssignDynamicPropertyCommand',
    'AssignVariableCommand',
    'ConditionCommand'
]);

/** Coverage JSON from `isl transform|test --coverage-report` (single schema). */
export interface IslCoverageReport {
    version: number;
    /** Script basename for single-file transform; empty string for aggregated test runs. */
    moduleName?: string;
    /**
     * When true, [statements] lists every coverable statement (`hits` may be 0). When false (e.g. aggregated tests),
     * only statements with hits appear; [lines] may carry coarse per-line counts.
     */
    statementCatalogComplete?: boolean;
    lines: Record<string, Record<string, number>>;
    /**
     * 1-based line numbers (strings) per file key: if/else spans that are branch-incomplete in a single run.
     */
    linesBranchPartial?: Record<string, string[]>;
    /**
     * Precise spans (VS Code 0-based) for the if/else **branch that had no hits** when the other branch did
     * (aggregated for tests). Same [file] keys as statements.
     */
    branchUncoveredRanges?: IslBranchUncoveredRangeV1[];
    statements: IslCoverageStatementV1[];
    /** Emitted by isl-cmd; log in Output to confirm which CLI/JAR produced the report. */
    generator?: {
        islVersion?: string;
        moduleNames?: string[];
        statementCount?: number;
        catalogComplete?: boolean;
    };
}

export interface IslBranchUncoveredRangeV1 {
    file: string;
    range: {
        startLineNumber: number;
        startCharacter: number;
        endLineNumber: number;
        endCharacter: number;
    };
}

export interface IslCoverageStatementV1 {
    statementId: number;
    commandKind: string;
    hits: number;
    source: {
        file: string;
        startLine: number;
        startColumn: number;
        endLine: number | null;
        endColumn: number | null;
    };
    range: {
        startLineNumber: number;
        startCharacter: number;
        endLineNumber: number;
        endCharacter: number;
    };
}

interface BuiltDecorations {
    /** Whole-line gutter: every coverable statement on the line was hit (catalog-complete reports only). */
    lineFull: vscode.DecorationOptions[];
    /** Whole-line gutter: some statements on the line were hit and some were not. */
    linePartial: vscode.DecorationOptions[];
    /** Coarse line map when [statementCatalogComplete] is false (e.g. tests). */
    lineLegacy: vscode.DecorationOptions[];
    /** Unused (gutter-only for hits); kept for apply/clear shape. */
    stmtHit: vscode.DecorationOptions[];
    /** Orange underline: coverable statements with 0 hits (catalog-complete only). */
    stmtMiss: vscode.DecorationOptions[];
    /** Branch that had no executions while the sibling branch did (if/else). */
    branchUncovered: vscode.DecorationOptions[];
}

/**
 * Inline “not executed” styling. VS Code often ignores [textDecoration] on decorations, so we use a thick
 * bottom border + light tint. Shown on: (1) [branchUncoveredRanges] = missed if/else branch spans from the report,
 * (2) catalog statement misses. Also see the overview ruler (center lane) for the same ranges.
 */
const COVERAGE_NOT_EXECUTED_INLINE = {
    borderWidth: '0 0 3px 0',
    borderStyle: 'solid',
    borderColor: 'rgba(212, 138, 30, 0.95)',
    backgroundColor: 'rgba(212, 138, 30, 0.14)'
} as const;

/**
 * Coverage UI: left gutter bars (full / partial / legacy); inline amber bottom-border on spans that did not run.
 * Hit ranges stay gutter-only (no green fill).
 */
export class IslCoverageDecorationManager implements vscode.Disposable {
    private static readonly EDIT_IDLE_CLEAR_MS = 2500;

    /** Whole line fully covered (every coverable statement on the line hit). */
    private readonly lineDecorationFullType = vscode.window.createTextEditorDecorationType({
        isWholeLine: true,
        borderWidth: '0 0 0 2px',
        borderStyle: 'solid',
        borderColor: new vscode.ThemeColor('testing.iconPassed'),
        overviewRulerColor: new vscode.ThemeColor('testing.iconPassed'),
        overviewRulerLane: vscode.OverviewRulerLane.Left
    });

    /** Line has both hit and missed coverable statements, or branch-incomplete if/else. */
    private readonly lineDecorationPartialType = vscode.window.createTextEditorDecorationType({
        isWholeLine: true,
        borderWidth: '0 0 0 5px',
        borderStyle: 'solid',
        borderColor: new vscode.ThemeColor('editorWarning.foreground'),
        overviewRulerColor: new vscode.ThemeColor('editorWarning.foreground'),
        overviewRulerLane: vscode.OverviewRulerLane.Left
    });

    /** Coarse line hits when the report has no full statement catalog. */
    private readonly lineDecorationLegacyType = vscode.window.createTextEditorDecorationType({
        isWholeLine: true,
        borderWidth: '0 0 0 2px',
        borderStyle: 'solid',
        borderColor: new vscode.ThemeColor('testing.iconPassed'),
        overviewRulerColor: new vscode.ThemeColor('testing.iconPassed'),
        overviewRulerLane: vscode.OverviewRulerLane.Left
    });

    /** Unused (kept so clear/apply paths stay stable); hit coverage is gutter-only. */
    private readonly statementHitDecorationType = vscode.window.createTextEditorDecorationType({});

    /** Coverable statement with 0 hits (catalog-complete reports). */
    private readonly statementMissDecorationType = vscode.window.createTextEditorDecorationType({
        ...COVERAGE_NOT_EXECUTED_INLINE,
        overviewRulerColor: 'rgba(212, 138, 30, 0.55)',
        overviewRulerLane: vscode.OverviewRulerLane.Center
    });

    /** If/else branch that had no hits while the other branch did (see [branchUncoveredRanges]). */
    private readonly branchUncoveredDecorationType = vscode.window.createTextEditorDecorationType({
        ...COVERAGE_NOT_EXECUTED_INLINE,
        overviewRulerColor: 'rgba(212, 138, 30, 0.55)',
        overviewRulerLane: vscode.OverviewRulerLane.Center
    });

    /**
     * Last parsed report. The temp file from Run with input is deleted right after; we keep JSON in memory
     * so dependent modules still paint and newly opened tabs can apply the same report.
     */
    private lastCoverageReport: IslCoverageReport | null = null;
    /** Documents that currently have non-empty coverage decorations (for edit-invalidates). */
    private readonly decoratedDocumentUris = new Set<string>();
    /** Debounced invalidation when `clearWhenDocumentEdited` is on. */
    private readonly editInvalidateTimers = new Map<string, ReturnType<typeof setTimeout>>();
    private readonly disposables: vscode.Disposable[] = [
        this.lineDecorationFullType,
        this.lineDecorationPartialType,
        this.lineDecorationLegacyType,
        this.statementHitDecorationType,
        this.statementMissDecorationType,
        this.branchUncoveredDecorationType,
        vscode.window.onDidChangeVisibleTextEditors(() => this.refreshAllVisible()),
        vscode.workspace.onDidChangeTextDocument(e => this.onDocumentChanged(e.document)),
        vscode.workspace.onDidOpenTextDocument(doc => this.onDidOpenIslDocument(doc)),
        vscode.workspace.onDidChangeConfiguration(e => {
            if (e.affectsConfiguration('isl.coverage')) {
                this.onCoverageSettingsChanged();
            }
        })
    ];

    dispose(): void {
        this.cancelAllEditInvalidateTimers();
        this.clearAll();
        for (const d of this.disposables) {
            d.dispose();
        }
    }

    private coverageEnabled(): boolean {
        return islCoverageUiEnabled();
    }

    /** When true, clear cached coverage for a file after idle typing (default false = keep until next run). */
    private clearWhenDocumentEdited(): boolean {
        return vscode.workspace.getConfiguration('isl.coverage').get<boolean>('clearWhenDocumentEdited', false);
    }

    private onCoverageSettingsChanged(): void {
        if (!this.coverageEnabled()) {
            this.cancelAllEditInvalidateTimers();
            this.stripAllEditorDecorations();
            return;
        }
        if (!this.clearWhenDocumentEdited()) {
            this.cancelAllEditInvalidateTimers();
        }
        this.refreshAllVisible();
    }

    private stripAllEditorDecorations(): void {
        for (const ed of vscode.window.visibleTextEditors) {
            ed.setDecorations(this.lineDecorationFullType, []);
            ed.setDecorations(this.lineDecorationPartialType, []);
            ed.setDecorations(this.lineDecorationLegacyType, []);
            ed.setDecorations(this.statementHitDecorationType, []);
            ed.setDecorations(this.statementMissDecorationType, []);
            ed.setDecorations(this.branchUncoveredDecorationType, []);
        }
    }

    /**
     * Load coverage JSON and decorate every open ISL editor that matches the report (not only the active file).
     */
    applyReportForDocument(_document: vscode.TextDocument, reportPath: string): void {
        this.applyReportFromPathToOpenDocuments(reportPath);
    }

    /**
     * Apply one coverage report to every open ISL file document (e.g. after `isl test --coverage-report`).
     */
    applyReportToOpenIslDocuments(reportPath: string): void {
        this.applyReportFromPathToOpenDocuments(reportPath);
    }

    /** One-line summary for the ISL output channel (from report.generator). */
    formatLastCoverageVerification(): string | undefined {
        const g = this.lastCoverageReport?.generator;
        if (!g) {
            return undefined;
        }
        const mods = g.moduleNames?.length ? g.moduleNames.join(', ') : '—';
        return `Coverage: ISL ${g.islVersion ?? '?'} · ${g.statementCount ?? '?'} statements · catalogComplete=${String(
            g.catalogComplete ?? false
        )} · modules: ${mods}`;
    }

    private onDidOpenIslDocument(doc: vscode.TextDocument): void {
        if (!this.coverageEnabled() || !this.lastCoverageReport) {
            return;
        }
        if (doc.languageId !== 'isl' || doc.uri.scheme !== 'file') {
            return;
        }
        this.applyReportToSingleDocument(doc, this.lastCoverageReport);
    }

    private applyReportFromPathToOpenDocuments(reportPath: string): void {
        if (!this.coverageEnabled()) {
            return;
        }
        const report = this.tryReadCoverageReport(reportPath);
        if (!report) {
            return;
        }
        this.activateReport(report);
        for (const doc of vscode.workspace.textDocuments) {
            if (doc.languageId === 'isl' && doc.uri.scheme === 'file') {
                this.applyReportToSingleDocument(doc, report);
            }
        }
    }

    private tryReadCoverageReport(reportPath: string): IslCoverageReport | null {
        try {
            const raw = fs.readFileSync(reportPath, 'utf8');
            const report = JSON.parse(raw) as IslCoverageReport;
            if (report.version !== ISL_COVERAGE_JSON_VERSION) {
                return null;
            }
            return report;
        } catch {
            return null;
        }
    }

    private activateReport(report: IslCoverageReport): void {
        this.cancelAllEditInvalidateTimers();
        this.decoratedDocumentUris.clear();
        this.stripAllEditorDecorations();
        this.lastCoverageReport = report;
    }

    private applyReportToSingleDocument(document: vscode.TextDocument, report: IslCoverageReport): void {
        if (document.languageId !== 'isl' || document.uri.scheme !== 'file') {
            return;
        }
        if (!this.coverageEnabled()) {
            return;
        }

        const uriKey = document.uri.toString();
        const basename = path.basename(document.uri.fsPath);
        const mod = report.moduleName ?? '';
        if (mod.length > 0 && basename !== mod) {
            const lineMapProbe = this.findLineMapForDocument(document, report);
            const hasStmt = (report.statements ?? []).some(s => this.sourceFileMatchesDocument(s.source.file, document));
            if (Object.keys(lineMapProbe).length === 0 && !hasStmt) {
                this.decoratedDocumentUris.delete(uriKey);
                this.clearDecorationsForDocumentUri(document.uri);
                return;
            }
        }

        const built = this.buildDecorationsForDocument(document, report);
        if (this.isBuiltEmpty(built)) {
            this.decoratedDocumentUris.delete(uriKey);
            this.clearDecorationsForDocumentUri(document.uri);
            return;
        }

        this.decoratedDocumentUris.add(uriKey);
        for (const ed of vscode.window.visibleTextEditors) {
            if (ed.document.uri.toString() === uriKey) {
                this.applyBuiltDecorations(ed, built);
            }
        }
    }

    private isBuiltEmpty(built: BuiltDecorations): boolean {
        return (
            built.lineFull.length === 0 &&
            built.linePartial.length === 0 &&
            built.lineLegacy.length === 0 &&
            built.stmtHit.length === 0 &&
            built.stmtMiss.length === 0 &&
            built.branchUncovered.length === 0
        );
    }

    private applyBuiltDecorations(ed: vscode.TextEditor, built: BuiltDecorations): void {
        ed.setDecorations(this.lineDecorationFullType, built.lineFull);
        ed.setDecorations(this.lineDecorationPartialType, built.linePartial);
        ed.setDecorations(this.lineDecorationLegacyType, built.lineLegacy);
        ed.setDecorations(this.statementHitDecorationType, built.stmtHit);
        ed.setDecorations(this.statementMissDecorationType, built.stmtMiss);
        ed.setDecorations(this.branchUncoveredDecorationType, built.branchUncovered);
    }

    private clearDecorationsForDocumentUri(uri: vscode.Uri): void {
        for (const ed of vscode.window.visibleTextEditors) {
            if (ed.document.uri.toString() === uri.toString()) {
                ed.setDecorations(this.lineDecorationFullType, []);
                ed.setDecorations(this.lineDecorationPartialType, []);
                ed.setDecorations(this.lineDecorationLegacyType, []);
                ed.setDecorations(this.statementHitDecorationType, []);
                ed.setDecorations(this.statementMissDecorationType, []);
                ed.setDecorations(this.branchUncoveredDecorationType, []);
            }
        }
    }

    private statementCatalogComplete(report: IslCoverageReport): boolean {
        return report.statementCatalogComplete === true;
    }

    private static statementTouchesLine(s: IslCoverageStatementV1, line1Based: number): boolean {
        const end = s.source.endLine ?? s.source.startLine;
        return s.source.startLine <= line1Based && line1Based <= end;
    }

    private buildDecorationsForDocument(document: vscode.TextDocument, report: IslCoverageReport): BuiltDecorations {
        const stmtsForDoc = (report.statements ?? []).filter(s => this.sourceFileMatchesDocument(s.source.file, document));

        const stmtHit: vscode.DecorationOptions[] = [];
        const stmtMiss: vscode.DecorationOptions[] = [];
        for (const s of stmtsForDoc) {
            if (s.hits > 0 || !this.statementCatalogComplete(report)) {
                continue;
            }
            const range = this.clampRange(document, s.range);
            if (!range) {
                continue;
            }
            const md = new vscode.MarkdownString();
            md.appendMarkdown(`**${s.commandKind}**\n\n`);
            md.appendMarkdown(`0 hits · statement id ${s.statementId}`);
            stmtMiss.push({ range, hoverMessage: md });
        }

        let lineFull: vscode.DecorationOptions[] = [];
        const linePartial: vscode.DecorationOptions[] = [];
        const lineLegacy: vscode.DecorationOptions[] = [];
        const branchPartial = this.branchPartialLineSetForDocument(document, report);
        const branchPartialHover = new vscode.MarkdownString(
            'Part of an if/else where only one branch ran in this run (or aggregated test run), so branch coverage is incomplete on this line.'
        );

        if (this.statementCatalogComplete(report) && stmtsForDoc.length > 0) {
            for (let ln = 1; ln <= document.lineCount; ln++) {
                const onLine = stmtsForDoc.filter(
                    s =>
                        IslCoverageDecorationManager.statementTouchesLine(s, ln) &&
                        !STATEMENT_KINDS_SKIP_WIDE_COVERAGE_SPAN.has(s.commandKind)
                );
                if (onLine.length === 0) {
                    continue;
                }
                const allHit = onLine.every(s => s.hits > 0);
                const anyHit = onLine.some(s => s.hits > 0);
                const idx = ln - 1;
                const line = document.lineAt(idx);
                if (allHit) {
                    lineFull.push({
                        range: line.range,
                        hoverMessage: new vscode.MarkdownString(
                            `All ${onLine.length} coverable statement(s) on this line were executed.`
                        )
                    });
                } else if (anyHit) {
                    const missed = onLine.filter(s => s.hits === 0).length;
                    linePartial.push({
                        range: line.range,
                        hoverMessage: new vscode.MarkdownString(
                            `Partial coverage: ${missed} of ${onLine.length} coverable statement(s) on this line were not executed.`
                        )
                    });
                }
            }
        } else {
            const lineMap = this.findLineMapForDocument(document, report);
            for (const [lineStr, hits] of Object.entries(lineMap)) {
                const ln = parseInt(lineStr, 10);
                if (!Number.isFinite(ln) || ln < 1 || ln > document.lineCount) {
                    continue;
                }
                const idx = ln - 1;
                const line = document.lineAt(idx);
                if (branchPartial.has(ln)) {
                    linePartial.push({
                        range: line.range,
                        hoverMessage: branchPartialHover
                    });
                } else {
                    lineLegacy.push({
                        range: line.range,
                        hoverMessage: new vscode.MarkdownString(`${hits} execution hit(s) on this line`)
                    });
                }
            }
        }

        if (this.statementCatalogComplete(report) && branchPartial.size > 0) {
            lineFull = lineFull.filter(opt => !branchPartial.has(opt.range.start.line + 1));
            for (const ln of branchPartial) {
                if (ln < 1 || ln > document.lineCount) {
                    continue;
                }
                const idx = ln - 1;
                if (linePartial.some(o => o.range.start.line === idx)) {
                    continue;
                }
                linePartial.push({
                    range: document.lineAt(idx).range,
                    hoverMessage: branchPartialHover
                });
            }
        }

        const branchUncoveredHover = new vscode.MarkdownString(
            'This **if/else branch did not run** in this coverage run (the other branch had hits). For `isl test`, coverage is aggregated across all tests in the run.'
        );
        const branchUncovered: vscode.DecorationOptions[] = [];
        for (const u of report.branchUncoveredRanges ?? []) {
            if (!this.sourceFileMatchesDocument(u.file, document)) {
                continue;
            }
            const range = this.clampRange(document, u.range);
            if (!range) {
                continue;
            }
            branchUncovered.push({ range, hoverMessage: branchUncoveredHover });
        }

        return { lineFull, linePartial, lineLegacy, stmtHit, stmtMiss, branchUncovered };
    }

    private workspaceRelativePath(document: vscode.TextDocument): string | undefined {
        const folder = vscode.workspace.getWorkspaceFolder(document.uri);
        if (!folder) {
            return undefined;
        }
        return path.relative(folder.uri.fsPath, document.uri.fsPath).replace(/\\/g, '/');
    }

    /**
     * True if a coverage JSON module key (import path or relative file id from the compiler) refers to this document.
     */
    private moduleKeyMatchesDocument(moduleKey: string, document: vscode.TextDocument): boolean {
        const docPath = document.uri.fsPath;
        try {
            const na = path.normalize(moduleKey);
            const nb = path.normalize(docPath);
            const same =
                process.platform === 'win32' ? na.toLowerCase() === nb.toLowerCase() : na === nb;
            if (same) {
                return true;
            }
        } catch {
            /* ignore */
        }
        const docBase = path.basename(docPath);
        const rel = this.workspaceRelativePath(document);
        const k = moduleKey.replace(/\\/g, '/');
        const kNorm = normalizeIslModuleKey(k);
        const kBase = islModulePathBasename(moduleKey);

        if (kBase !== docBase) {
            if (rel && (k === rel || kNorm === rel)) {
                return true;
            }
            return false;
        }
        if (!rel) {
            return true;
        }
        if (k === rel || kNorm === rel) {
            return true;
        }
        if (rel.endsWith('/' + kNorm) || rel.endsWith(kNorm)) {
            return true;
        }
        if (kNorm.endsWith(rel)) {
            return true;
        }
        if (kNorm === docBase) {
            return true;
        }
        return false;
    }

    private pickLineMapKey(document: vscode.TextDocument, report: IslCoverageReport): string | undefined {
        const lines = report.lines ?? {};
        const keys = Object.keys(lines);
        if (keys.length === 0) {
            return undefined;
        }
        const basename = path.basename(document.uri.fsPath);
        const rel = this.workspaceRelativePath(document);
        const mod = report.moduleName ?? '';

        const tryKey = (candidate: string | undefined): string | undefined => {
            if (!candidate || lines[candidate] === undefined) {
                return undefined;
            }
            return this.moduleKeyMatchesDocument(candidate, document) ? candidate : undefined;
        };

        const fromDirect =
            tryKey(basename) ??
            (mod.length > 0 ? tryKey(mod) : undefined) ??
            (rel ? tryKey(rel) : undefined);
        if (fromDirect) {
            return fromDirect;
        }

        const matches = keys.filter(k => this.moduleKeyMatchesDocument(k, document));
        if (matches.length === 0) {
            return undefined;
        }
        if (matches.length === 1) {
            return matches[0];
        }
        if (rel) {
            const exact = matches.find(k => {
                const kn = normalizeIslModuleKey(k.replace(/\\/g, '/'));
                return kn === rel || k.replace(/\\/g, '/') === rel;
            });
            if (exact) {
                return exact;
            }
            const bySuffix = matches.find(k => {
                const kn = normalizeIslModuleKey(k.replace(/\\/g, '/'));
                return rel.endsWith('/' + kn) || rel.endsWith(kn);
            });
            if (bySuffix) {
                return bySuffix;
            }
        }
        return matches[0];
    }

    private findLineMapForDocument(document: vscode.TextDocument, report: IslCoverageReport): Record<string, number> {
        const k = this.pickLineMapKey(document, report);
        if (!k) {
            return {};
        }
        return report.lines?.[k] ?? {};
    }

    private sourceFileMatchesDocument(sourceFile: string, document: vscode.TextDocument): boolean {
        return this.moduleKeyMatchesDocument(sourceFile, document);
    }

    /** 1-based line numbers for [report.linesBranchPartial] keys that match this document. */
    private branchPartialLineSetForDocument(document: vscode.TextDocument, report: IslCoverageReport): Set<number> {
        const out = new Set<number>();
        const raw = report.linesBranchPartial ?? {};
        for (const [fileKey, lines] of Object.entries(raw)) {
            if (!this.moduleKeyMatchesDocument(fileKey, document)) {
                continue;
            }
            for (const s of lines) {
                const ln = parseInt(s, 10);
                if (Number.isFinite(ln)) {
                    out.add(ln);
                }
            }
        }
        return out;
    }

    clearAll(): void {
        this.cancelAllEditInvalidateTimers();
        this.decoratedDocumentUris.clear();
        this.lastCoverageReport = null;
        this.stripAllEditorDecorations();
    }

    clearUri(uri: vscode.Uri): void {
        this.cancelEditInvalidateTimer(uri.toString());
        this.decoratedDocumentUris.delete(uri.toString());
        this.clearDecorationsForDocumentUri(uri);
    }

    private cancelEditInvalidateTimer(uriKey: string): void {
        const t = this.editInvalidateTimers.get(uriKey);
        if (t !== undefined) {
            clearTimeout(t);
            this.editInvalidateTimers.delete(uriKey);
        }
    }

    private cancelAllEditInvalidateTimers(): void {
        for (const t of this.editInvalidateTimers.values()) {
            clearTimeout(t);
        }
        this.editInvalidateTimers.clear();
    }

    private onDocumentChanged(doc: vscode.TextDocument): void {
        if (doc.languageId !== 'isl') {
            return;
        }
        const uriKey = doc.uri.toString();
        if (!this.decoratedDocumentUris.has(uriKey)) {
            return;
        }
        if (!this.clearWhenDocumentEdited()) {
            return;
        }
        const existing = this.editInvalidateTimers.get(uriKey);
        if (existing !== undefined) {
            clearTimeout(existing);
        }
        const timer = setTimeout(() => {
            this.editInvalidateTimers.delete(uriKey);
            if (this.decoratedDocumentUris.has(uriKey)) {
                this.clearUri(doc.uri);
            }
        }, IslCoverageDecorationManager.EDIT_IDLE_CLEAR_MS);
        this.editInvalidateTimers.set(uriKey, timer);
    }

    private refreshAllVisible(): void {
        if (!this.coverageEnabled()) {
            this.stripAllEditorDecorations();
            return;
        }
        if (!this.lastCoverageReport) {
            return;
        }
        const report = this.lastCoverageReport;
        for (const ed of vscode.window.visibleTextEditors) {
            if (ed.document.languageId !== 'isl' || ed.document.uri.scheme !== 'file') {
                continue;
            }
            this.applyReportToSingleDocument(ed.document, report);
        }
    }

    private clampRange(
        document: vscode.TextDocument,
        r: IslCoverageStatementV1['range']
    ): vscode.Range | undefined {
        let sl = r.startLineNumber;
        let sc = r.startCharacter;
        let el = r.endLineNumber;
        let ec = r.endCharacter;
        const maxL = document.lineCount - 1;
        if (sl > maxL || el < 0) {
            return undefined;
        }
        sl = Math.max(0, Math.min(sl, maxL));
        el = Math.max(0, Math.min(el, maxL));
        const lineS = document.lineAt(sl);
        const lineE = document.lineAt(el);
        const maxSc = lineS.text.length;
        const maxEc = lineE.text.length;
        sc = Math.max(0, Math.min(sc, maxSc));
        ec = Math.max(0, Math.min(ec, maxEc));
        if (sl === el && ec <= sc) {
            ec = Math.min(maxEc, sc + 1);
        }
        return new vscode.Range(sl, sc, el, ec);
    }
}
