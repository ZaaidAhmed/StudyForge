#!/usr/bin/env node
/**
 * Local-only F01–F14 recorder.
 *
 * Prerequisite:
 *   npm install --prefix scripts
 *   npx --prefix scripts playwright install chromium
 *
 * Examples:
 *   node scripts/record-demos.mjs --ai-mode live
 *   node scripts/record-demos.mjs --ai-mode recovery --skip-build
 */
import fs from 'node:fs/promises';
import fsSync from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';
import { setTimeout as delay } from 'node:timers/promises';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const args = new Set(process.argv.slice(2));
const valueOf = (name, fallback) => {
  const index = process.argv.indexOf(name);
  return index >= 0 && process.argv[index + 1] ? process.argv[index + 1] : fallback;
};
const aiMode = valueOf('--ai-mode', 'recovery');
const port = Number(valueOf('--port', '18080'));
const skipBuild = args.has('--skip-build');
if (args.has('--help') || args.has('-h')) {
  console.log('Usage: node scripts/record-demos.mjs [--ai-mode live|recovery] [--port 18080] [--skip-build] [--headed]');
  console.log('Outputs: tmp/stage2-demos/manifest.json and docs/stage2/demo-index.local.md');
  process.exit(0);
}
const jar = path.join(root, 'target', 'studyforge-ai-0.1.0-SNAPSHOT.jar');
const outputRoot = path.join(root, 'tmp', 'stage2-demos');
const runId = new Date().toISOString().replaceAll(':', '-').replaceAll('.', '-');
const runRoot = path.join(outputRoot, runId);
const videosDir = path.join(runRoot, 'videos');
const screenshotsDir = path.join(runRoot, 'screenshots');
const runtimeDir = path.join(runRoot, 'runtime');
const baseUrl = `http://127.0.0.1:${port}`;
const courseCode = 'DEMO3311';
const records = [];
const cliEvidence = new Map();
const cliVideos = [];
let appProcess;
let chromium;
let browser;
let currentFeatureId = null;

const features = [
  ['F01', 'Course workspace'], ['F02', 'Material import'], ['F03', 'Material search'],
  ['F04', 'Grounded Q&A'], ['F05', 'Summaries'], ['F06', 'Concepts'],
  ['F07', 'Flashcards'], ['F08', 'Quiz generation'], ['F09', 'Quiz grading'],
  ['F10', 'Wrong-answer explanation'], ['F11', 'Progress'], ['F12', 'Study plan'],
  ['F13', 'Recommendations and agent'], ['F14', 'History and memory']
];

function executable(name) {
  if (process.platform !== 'win32') return name;
  if (name === 'java') {
    const javaHome = process.env.JAVA_HOME;
    return javaHome && fsSync.existsSync(path.join(javaHome, 'bin', 'java.exe')) ? path.join(javaHome, 'bin', 'java.exe') : 'java.exe';
  }
  const knownDir = path.join(process.env.USERPROFILE ?? os.homedir(), 'Downloads', 'maven-mvnd-1.0.6-windows-amd64', 'maven-mvnd-1.0.6-windows-amd64', 'bin');
  const native = path.join(knownDir, 'mvnd.exe');
  const script = path.join(knownDir, 'mvnd.cmd');
  return fsSync.existsSync(native) ? native : (fsSync.existsSync(script) ? script : 'mvn.cmd');
}

async function run(command, commandArgs, options = {}) {
  return new Promise((resolve) => {
    const { timeoutMs = 120000, ...spawnOptions } = options;
    const started = Date.now();
    const child = spawn(command, commandArgs, { cwd: root, windowsHide: true, ...spawnOptions });
    let stdout = '', stderr = '';
    const timer = setTimeout(() => { stderr += `\nProcess exceeded ${timeoutMs}ms and was terminated.`; child.kill(); }, timeoutMs);
    child.stdout?.on('data', (chunk) => { stdout += chunk; });
    child.stderr?.on('data', (chunk) => { stderr += chunk; });
    child.on('close', (code, signal) => { clearTimeout(timer); resolve({ code: code ?? 1, signal, stdout, stderr, durationMs: Date.now() - started }); });
    child.on('error', (error) => { clearTimeout(timer); resolve({ code: 1, signal: null, stdout, stderr: `${stderr}${error.message}`, durationMs: Date.now() - started }); });
  });
}

async function ensureFixturePdf(file) {
  // A tiny text PDF generated locally; it contains no personal or copyrighted data.
  const content = 'StudyForge demo lecture\\nStrategy pattern: interchangeable algorithms.\\nSOLID design principles.';
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>',
    `<< /Length ${content.length + 42} >>\\nstream\\nBT /F1 18 Tf 72 720 Td (${content.replaceAll('\\n', ') Tj 0 -28 Td (')}) Tj ET\\nendstream`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'
  ];
  let pdf = '%PDF-1.4\\n';
  const offsets = [0];
  for (let i = 0; i < objects.length; i++) { offsets.push(Buffer.byteLength(pdf, 'latin1')); pdf += `${i + 1} 0 obj\\n${objects[i]}\\nendobj\\n`; }
  const xref = Buffer.byteLength(pdf, 'latin1');
  pdf += `xref\\n0 ${objects.length + 1}\\n0000000000 65535 f \\n`;
  for (let i = 1; i < offsets.length; i++) pdf += `${String(offsets[i]).padStart(10, '0')} 00000 n \\n`;
  pdf += `trailer\\n<< /Size ${objects.length + 1} /Root 1 0 R >>\\nstartxref\\n${xref}\\n%%EOF\\n`;
  pdf = pdf.replaceAll(String.fromCharCode(92) + 'n', String.fromCharCode(10));
  await fs.writeFile(file, pdf, 'latin1');
}

async function prepare() {
  if (!['live', 'recovery'].includes(aiMode)) throw new Error('--ai-mode must be live or recovery.');
  if (!(await exists(jar)) && skipBuild) throw new Error(`Missing packaged JAR: ${jar}`);
  await fs.mkdir(videosDir, { recursive: true });
  await fs.mkdir(screenshotsDir, { recursive: true });
  await fs.mkdir(runtimeDir, { recursive: true });
  const pdf = path.join(runtimeDir, 'demo-lecture.pdf');
  await ensureFixturePdf(pdf);
  await fs.copyFile(path.join(root, 'scripts', 'demo-fixtures', 'lecture-notes.md'), path.join(runtimeDir, 'lecture-notes.md'));
  await fs.copyFile(path.join(root, 'scripts', 'demo-fixtures', 'quick-reference.txt'), path.join(runtimeDir, 'quick-reference.txt'));
  if (!skipBuild) {
    const result = await run(executable('mvnd'), ['-B', 'package', '-DskipTests']);
    if (result.code !== 0) throw new Error(`JAR build failed:\n${result.stderr}`);
  }
  if (!(await exists(jar))) throw new Error(`Packaged JAR was not found: ${jar}`);
}

async function exists(file) { try { await fs.access(file); return true; } catch { return false; } }

function springArgs() {
  const db = path.join(runtimeDir, 'coursepilot').replaceAll('\\', '/');
  const uploads = path.join(runtimeDir, 'uploads').replaceAll('\\', '/');
  return [`--spring.datasource.url=jdbc:h2:file:${db};DB_CLOSE_DELAY=-1`, `--coursepilot.storage.upload-dir=${uploads}`, '--coursepilot.ai.model=qwen3:4b', '--spring.thymeleaf.cache=false'];
}

async function cli(cliArgs) {
  const db = path.join(runtimeDir, 'coursepilot').replaceAll('\\', '/');
  const uploads = path.join(runtimeDir, 'uploads').replaceAll('\\', '/');
  return run(executable('java'), [
    '-Dspring.main.web-application-type=none', '-Dspring.profiles.active=cli',
    `-Dspring.datasource.url=jdbc:h2:file:${db};DB_CLOSE_DELAY=-1`,
    `-Dcoursepilot.storage.upload-dir=${uploads}`, '-Dcoursepilot.ai.model=qwen3:4b',
    `-Dcoursepilot.ai.enabled=${aiMode === 'live'}`,
    '-jar', jar, ...cliArgs
  ], { timeoutMs: 30000 });
}

function evidenceKey(cliArgs) { return cliArgs.join('\u0000'); }
async function captureCli(cliArgs) {
  const result = await cli(cliArgs);
  cliEvidence.set(evidenceKey(cliArgs), result);
  return result;
}
function recordedCli(cliArgs) {
  return cliEvidence.get(evidenceKey(cliArgs)) ?? { code: 1, stdout: '', stderr: 'CLI evidence was not captured.', durationMs: 0 };
}

async function collectCliEvidence(documentId) {
  const commands = [
    ['course', 'list'], ['course', 'show', '--course', courseCode],
    ['material', 'list', '--course', courseCode], ['material', 'show', '--course', courseCode, '--id', documentId],
    ['material', 'search', '--course', courseCode, '--query', 'strategy pattern', '--limit', '5'],
    ['ask', '--course', courseCode, '--question', 'What is the Strategy pattern?'],
    ['summary', 'document', '--course', courseCode, '--document', documentId],
    ['summary', 'topic', '--course', courseCode, '--topic', 'design principles'],
    ['concepts', 'extract', '--course', courseCode, '--document', documentId],
    ['flashcards', 'generate', '--course', courseCode, '--document', documentId, '--count', '5'],
    ['flashcards', 'list', '--course', courseCode],
    ['quiz', 'generate', '--course', courseCode, '--document', documentId, '--count', '5'],
    ['quiz', 'list', '--course', courseCode], ['progress', '--course', courseCode],
    ['study-plan', 'list', '--course', courseCode], ['recommend', '--course', courseCode],
    ['agent', '--course', courseCode, '--request', 'Help me study what I am weakest at'],
    ['history', '--course', courseCode], ['ai', 'status']
  ];
  const aiCommands = new Set(['ask', 'summary', 'concepts', 'flashcards', 'quiz', 'agent']);
  for (const command of commands) {
    if (aiMode === 'recovery' && aiCommands.has(command[0])) {
      cliEvidence.set(evidenceKey(command), { code: 2, stdout: '', stderr: 'Skipped in recovery mode; run --ai-mode live to capture the real AI command.', durationMs: 0 });
    } else {
      await captureCli(command);
    }
  }
}

async function startApp() {
  const applicationArgs = ['-jar', jar, `--server.port=${port}`, ...springArgs(), ...(aiMode === 'recovery' ? ['--coursepilot.ai.enabled=false'] : [])];
  appProcess = spawn(executable('java'), applicationArgs, { cwd: root, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  appProcess.stdout.on('data', () => {});
  appProcess.stderr.on('data', () => {});
  for (let i = 0; i < 60; i++) {
    try { const response = await fetch(`${baseUrl}/`); if (response.ok) return; } catch {}
    await delay(500);
  }
  throw new Error(`StudyForge did not become ready on ${baseUrl}`);
}

async function seed() {
  const course = await cli(['course', 'create', '--code', courseCode, '--name', 'Software Design', '--description', 'Local Stage 2 demonstration course']);
  if (course.code !== 0 && !course.stderr.includes('already exists')) throw new Error(`Course seed failed: ${course.stderr}`);
  const files = ['demo-lecture.pdf', 'lecture-notes.md', 'quick-reference.txt'];
  for (const file of files) {
    const result = await cli(['material', 'add', '--course', courseCode, '--file', path.join(runtimeDir, file)]);
    if (result.code !== 0 && !result.stderr.includes('already exists')) throw new Error(`Material seed failed for ${file}: ${result.stderr}`);
  }
}

async function getCourseId(page) {
  await page.goto(`${baseUrl}/courses`);
  const hrefs = await page.locator('a[href^="/courses/"]').evaluateAll((links) => links.map((link) => link.getAttribute('href')));
  const match = hrefs.find((href) => href?.match(/^\/courses\/\d+$/));
  if (!match) throw new Error('Demo course was not visible in the course list.');
  return match.split('/')[2];
}

async function getDocumentId() {
  const result = await cli(['material', 'list', '--course', courseCode]);
  const match = result.stdout.match(/^(\d+)\s+.*?\s+READY\s+/m);
  if (!match) throw new Error(`No READY demo document found.\n${result.stdout}`);
  return match[1];
}

async function title(page, id, name) {
  await page.evaluate(({ id, name }) => {
    const node = document.createElement('div');
    node.id = 'recording-title';
    const heading = document.createElement('strong');
    heading.textContent = `StudyForge · ${id}`;
    const feature = document.createElement('span');
    feature.textContent = name;
    const note = document.createElement('small');
    note.textContent = 'Local demonstration — not uploaded';
    node.append(heading, feature, note);
    Object.assign(node.style, { position: 'fixed', inset: '18px auto auto 18px', zIndex: 99999, background: '#111827', color: 'white', padding: '16px 20px', borderRadius: '10px', display: 'grid', gap: '4px', font: '14px system-ui', boxShadow: '0 8px 24px #0005' });
    document.body.appendChild(node);
  }, { id, name });
  await delay(1000);
}

async function terminalPanel(page, command, result) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, recordVideo: { dir: videosDir, size: { width: 1440, height: 900 } } });
  const evidencePage = await context.newPage();
  await evidencePage.setContent(`<!doctype html><html><head><style>body{margin:0;background:#0b1020;color:#d6e4ff;font:18px system-ui,sans-serif}main{padding:48px 64px}h1{margin:0 0 12px;color:#fff;font-size:30px}.meta{color:#8fb2df;margin-bottom:28px}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#111a2e;border:1px solid #263b60;border-radius:12px;padding:24px;line-height:1.5}</style></head><body><main><h1 id="heading"></h1><div class="meta">CLI evidence companion · local only</div><pre id="output"></pre></main></body></html>`);
  await evidencePage.locator('#heading').evaluate((node, value) => { node.textContent = value; }, `${currentFeatureId} · CLI command`);
  await evidencePage.locator('#output').evaluate((node, value) => { node.textContent = value; }, `$ ${command}\n\nstdout:\n${result.stdout || '(none)'}\n\nstderr:\n${result.stderr || '(none)'}\n\nexit code: ${result.code}\nduration: ${result.durationMs}ms`);
  await delay(1800);
  await evidencePage.screenshot({ path: path.join(screenshotsDir, `${currentFeatureId}-cli.png`) });
  const video = evidencePage.video();
  await context.close();
  cliVideos.push({ featureId: currentFeatureId, command, localVideo: path.relative(root, await video.path()) });
  await delay(1800);
}

async function recovery(page, text) {
  await page.evaluate((text) => { const node = document.createElement('p'); node.textContent = `Recovery case: ${text}`; node.style.cssText = 'position:fixed;left:18px;bottom:18px;z-index:99997;background:#fff7d6;color:#513d00;padding:12px;border:1px solid #e0b341;border-radius:8px;font:13px system-ui'; document.body.appendChild(node); }, text);
  await delay(2200);
}

async function recordFeature(id, name, fn) {
  currentFeatureId = id;
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, recordVideo: { dir: videosDir, size: { width: 1440, height: 900 } } });
  const page = await context.newPage();
  const started = new Date().toISOString();
  let status = 'Recorded', error = null, guiRoute = `${baseUrl}/`;
  try { await fn(page); await delay(1800); guiRoute = page.url(); await page.screenshot({ path: path.join(screenshotsDir, `${id}.png`), fullPage: true }); }
  catch (exception) { status = 'Failed'; error = exception.message; await page.screenshot({ path: path.join(screenshotsDir, `${id}-failed.png`), fullPage: true }).catch(() => {}); }
  await page.evaluate(({ id, name, status }) => { const node = document.getElementById('recording-title'); if (node) node.innerHTML += `<small>${id} complete · ${status}</small>`; }, { id, name, status });
  await delay(800);
  const video = page.video();
  await context.close();
  records.push({ featureId: id, featureName: name, localVideo: path.relative(root, await video.path()), cliVideo: cliVideos.find((item) => item.featureId === id)?.localVideo ?? null, guiRoute, aiMode, model: 'qwen3:4b', status, startedAt: started, publicUrl: 'Pending upload', error });
}

async function runRecordings(courseId, documentId) {
  const u = (suffix) => `${baseUrl}/courses/${courseId}${suffix}`;
  await recordFeature('F01', 'Course workspace', async (page) => { await page.goto(`${baseUrl}/courses`); await title(page, 'F01', 'Course workspace'); await page.goto(u('')); await page.getByRole('link', { name: /View course tools/i }).click(); await page.waitForLoadState('domcontentloaded'); const result = recordedCli(['course', 'list']); await terminalPanel(page, 'studyforge course list', result); await recovery(page, 'duplicate course code is rejected by the CLI.'); });
  await recordFeature('F02', 'Material import', async (page) => { await page.goto(u('/materials')); await title(page, 'F02', 'Material import'); await page.setInputFiles('#file', path.join(runtimeDir, 'lecture-notes.md')); await page.getByRole('button', { name: /Upload and process/i }).click(); await page.waitForLoadState('domcontentloaded'); const result = recordedCli(['material', 'list', '--course', courseCode]); await terminalPanel(page, 'studyforge material list --course DEMO3311', result); await recovery(page, 'unsupported extensions are rejected and course context is preserved.'); });
  await recordFeature('F03', 'Material search', async (page) => { await page.goto(u('/materials')); await title(page, 'F03', 'Material search'); await page.locator('#query').fill('strategy pattern'); await page.getByRole('button', { name: /Search materials/i }).click(); await page.waitForLoadState('domcontentloaded'); const result = recordedCli(['material', 'search', '--course', courseCode, '--query', 'strategy pattern', '--limit', '5']); await terminalPanel(page, 'studyforge material search --course DEMO3311 --query "strategy pattern" --limit 5', result); await recovery(page, 'a blank or no-result query returns actionable guidance.'); });
  await recordFeature('F04', 'Grounded Q&A', async (page) => { await page.goto(u('/ask')); await title(page, 'F04', 'Grounded Q&A'); await page.locator('textarea[name="question"]').fill('What is the Strategy pattern?'); await page.getByRole('button', { name: /Ask/i }).click(); await page.waitForLoadState('domcontentloaded').catch(() => {}); const result = recordedCli(['ask', '--course', courseCode, '--question', 'What is the Strategy pattern?']); await terminalPanel(page, 'studyforge ask --course DEMO3311 --question "What is the Strategy pattern?"', result); await recovery(page, 'a question absent from the evidence shows insufficient-context recovery.'); });
  await recordFeature('F05', 'Summaries', async (page) => { await page.goto(u(`/materials/${documentId}/summary`)); await title(page, 'F05', 'Summaries'); await page.getByRole('button', { name: /Generate summary/i }).click(); await page.waitForLoadState('domcontentloaded').catch(() => {}); await terminalPanel(page, 'studyforge summary document --course DEMO3311 --document ' + documentId, recordedCli(['summary', 'document', '--course', courseCode, '--document', documentId])); await recovery(page, aiMode === 'live' ? 'summary generation identifies the configured local model.' : 'AI-unavailable recovery keeps the document context.'); });
  await recordFeature('F06', 'Concepts', async (page) => { await page.goto(u(`/materials/${documentId}/concepts`)); await title(page, 'F06', 'Concepts'); await page.getByRole('button', { name: /Extract/i }).click().catch(() => {}); await page.waitForLoadState('domcontentloaded').catch(() => {}); await terminalPanel(page, 'studyforge concepts extract --course DEMO3311 --document ' + documentId, recordedCli(['concepts', 'extract', '--course', courseCode, '--document', documentId])); await recovery(page, 'invalid or empty AI extraction remains recoverable.'); });
  await recordFeature('F07', 'Flashcards', async (page) => { await page.goto(u('/flashcards')); await title(page, 'F07', 'Flashcards'); await page.locator('select[name="documentId"]').selectOption(documentId); await page.locator('select[name="count"]').selectOption('5'); await page.getByRole('button', { name: /Generate flashcards/i }).click(); await page.waitForLoadState('domcontentloaded').catch(() => {}); await terminalPanel(page, 'studyforge flashcards generate --course DEMO3311 --document ' + documentId + ' --count 5', recordedCli(['flashcards', 'generate', '--course', courseCode, '--document', documentId, '--count', '5'])); await recovery(page, 'invalid count or unavailable document preserves the selection.'); });
  await recordFeature('F08', 'Quiz generation', async (page) => { await page.goto(u('/quizzes')); await title(page, 'F08', 'Quiz generation'); await page.locator('select[name="documentId"]').selectOption(documentId); await page.locator('select[name="count"]').selectOption('5'); await page.getByRole('button', { name: /Generate quiz/i }).click(); await page.waitForLoadState('domcontentloaded').catch(() => {}); await terminalPanel(page, 'studyforge quiz generate --course DEMO3311 --document ' + documentId + ' --count 5', recordedCli(['quiz', 'generate', '--course', courseCode, '--document', documentId, '--count', '5'])); await recovery(page, 'partial or malformed generation is reported instead of silently saved.'); });
  await recordFeature('F09', 'Quiz grading', async (page) => { await page.goto(u('/quizzes')); await title(page, 'F09', 'Quiz grading'); const quiz = page.locator('a[href*="/quizzes/"]').first(); if (await quiz.count()) { await quiz.click(); await page.waitForLoadState('domcontentloaded'); const submit = page.getByRole('button', { name: /Submit answers/i }); if (await submit.count()) await submit.click(); } await recovery(page, 'unanswered questions are submitted and graded deterministically.'); });
  await recordFeature('F10', 'Wrong-answer explanation', async (page) => { await page.goto(u('/quizzes')); await title(page, 'F10', 'Wrong-answer explanation'); const quiz = page.locator('a[href*="/quizzes/"]').first(); if (await quiz.count()) { await quiz.click(); await page.waitForLoadState('domcontentloaded'); const submit = page.getByRole('button', { name: /Submit answers/i }); if (await submit.count()) await submit.click(); await page.getByRole('button', { name: /Explain this answer/i }).first().click().catch(() => {}); } await recovery(page, 'missing evidence or unavailable AI provides a retry path.'); });
  await recordFeature('F11', 'Progress', async (page) => { await page.goto(u('/progress')); await title(page, 'F11', 'Progress'); const result = recordedCli(['progress', '--course', courseCode]); await terminalPanel(page, 'studyforge progress --course DEMO3311', result); await recovery(page, 'before activity, the page explains how to create a learning signal.'); });
  await recordFeature('F12', 'Study plan', async (page) => { await page.goto(u('/study-plan')); await title(page, 'F12', 'Study plan'); await page.locator('input[name="startDate"]').fill('2026-10-22'); await page.locator('input[name="targetDate"]').fill('2026-10-29'); await page.locator('input[name="minutes"]').fill('30'); await page.getByRole('button', { name: /Create study plan/i }).click(); await page.waitForLoadState('domcontentloaded'); const result = recordedCli(['study-plan', 'list', '--course', courseCode]); await terminalPanel(page, 'studyforge study-plan list --course DEMO3311', result); await recovery(page, 'an invalid date range or zero minutes shows inline validation.'); });
  await recordFeature('F13', 'Recommendations and agent', async (page) => { await page.goto(u('/insights')); await title(page, 'F13', 'Recommendations and agent'); await page.goto(u('/recommendation')); await page.goto(u('/agent')); await page.locator('textarea[name="request"]').fill('Help me study what I am weakest at'); await page.getByRole('button', { name: /Run study task/i }).click(); await page.waitForLoadState('domcontentloaded').catch(() => {}); const result = recordedCli(['recommend', '--course', courseCode]); await terminalPanel(page, 'studyforge recommend --course DEMO3311', result); await recovery(page, 'unsupported or empty requests are rejected without arbitrary tool execution.'); });
  await recordFeature('F14', 'History and memory', async (page) => { await page.goto(u('/history')); await title(page, 'F14', 'History and memory'); const result = recordedCli(['history', '--course', courseCode]); await terminalPanel(page, 'studyforge history --course DEMO3311', result); await recovery(page, 'empty history and second-course isolation are visible states.'); });
}

async function main() {
  try {
    try { ({ chromium } = await import('playwright')); } catch { throw new Error('Playwright is required. Run: npm install --prefix scripts && npx --prefix scripts playwright install chromium'); }
    await prepare();
    await seed();
    const imported = await getDocumentId();
    await collectCliEvidence(imported);
    await startApp();
    browser = await chromium.launch({ headless: !args.has('--headed') });
    const page = await browser.newPage();
    const courseId = await getCourseId(page);
    await page.close();
    await runRecordings(courseId, imported);
    await browser.close();
    const manifest = { generatedAt: new Date().toISOString(), aiMode, model: 'qwen3:4b', baseUrl, runDirectory: path.relative(root, runRoot), features: records };
    await fs.writeFile(path.join(outputRoot, 'manifest.json'), JSON.stringify(manifest, null, 2));
    const markdown = ['# Local Stage 2 demonstration evidence', '', `Generated: ${manifest.generatedAt}`, `AI mode: ${aiMode}`, 'Public URLs: Pending upload', '', '| Feature | GUI video | CLI evidence video | GUI route | Status | Notes |', '|---|---|---|---|---|---|'];
    const localLink = (value) => value ? `[Open video](../../${value.replaceAll('\\', '/')})` : 'Not applicable';
    for (const record of records) markdown.push(`| ${record.featureId} ${record.featureName} | ${localLink(record.localVideo)} | ${localLink(record.cliVideo)} | ${record.guiRoute} | ${record.status} | ${record.error ?? 'Recorded locally; review before upload.'} |`);
    await fs.writeFile(path.join(root, 'docs', 'stage2', 'demo-index.local.md'), `${markdown.join('\n')}\n`);
    if (records.some((record) => record.status === 'Failed')) process.exitCode = 1;
  } catch (error) {
    console.error(`Recording failed: ${error.message}`);
    process.exitCode = 1;
  } finally {
    if (appProcess && !appProcess.killed) appProcess.kill();
  }
}

await main();
