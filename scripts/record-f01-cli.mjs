import fs from 'node:fs/promises';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const stamp = new Date().toISOString().replaceAll(':', '-').replaceAll('.', '-');
const output = path.join(root, 'tmp', 'stage2-demos', stamp, 'videos');
const runtime = path.join(root, 'tmp', 'stage2-demos', stamp, 'runtime');
await fs.mkdir(output, { recursive: true });
await fs.mkdir(runtime, { recursive: true });

const java = process.env.JAVA_HOME
  ? path.join(process.env.JAVA_HOME, 'bin', 'java.exe')
  : 'C:\\Users\\zaaid\\Downloads\\OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1\\jdk-21.0.12.1+1\\bin\\java.exe';
const jar = path.join(root, 'target', 'studyforge-ai-0.1.0-SNAPSHOT.jar');
const db = path.join(runtime, 'coursepilot').replaceAll('\\', '/');
const uploads = path.join(runtime, 'uploads').replaceAll('\\', '/');
const cliPrefix = 'java -Dspring.main.web-application-type=none -Dspring.profiles.active=cli -jar target\\studyforge-ai-0.1.0-SNAPSHOT.jar';

function run(args) {
  return new Promise((resolve) => {
    const child = spawn(java, [
      '-Dspring.main.web-application-type=none', '-Dspring.profiles.active=cli',
      `-Dspring.datasource.url=jdbc:h2:file:${db};DB_CLOSE_DELAY=-1`,
      `-Dcoursepilot.storage.upload-dir=${uploads}`, '-Dcoursepilot.ai.enabled=false',
      '-jar', jar, ...args
    ], { cwd: root, windowsHide: true });
    let stdout = '', stderr = '';
    child.stdout.on('data', chunk => stdout += chunk);
    child.stderr.on('data', chunk => stderr += chunk);
    child.on('close', code => resolve({ code: code ?? 1, stdout, stderr }));
  });
}

const create = await run(['course', 'create', '--code', 'DEMO3311', '--name', 'Software Design']);
const list = await run(['course', 'list']);
const duplicate = await run(['course', 'create', '--code', 'DEMO3311', '--name', 'Duplicate Course']);
function meaningful(result) {
  const text = `${result.stdout}\n${result.stderr}`
    .replace(/\x1b\[[0-?]*[ -\/]*[@-~]/g, '')
    .split(/\r?\n/)
    .filter(line => /Created course|DEMO3311|already exists|duplicate|No courses/i.test(line));
  return text.slice(-8).join('\n') || '(no concise CLI result returned)';
}
const commandOutput = [
  `$ ${cliPrefix} course create --code DEMO3311 --name "Software Design"`,
  `exit code: ${create.code}`,
  meaningful(create),
  `$ ${cliPrefix} course list`,
  `exit code: ${list.code}`,
  meaningful(list),
  `$ ${cliPrefix} course create --code DEMO3311 --name "Duplicate Course"`,
  `exit code: ${duplicate.code}`,
  meaningful(duplicate),
].join('\n\n');

const context = await chromium.launch({ headless: true });
const browser = await context.newContext({ viewport: { width: 1440, height: 900 }, recordVideo: { dir: output, size: { width: 1440, height: 900 } } });
const page = await browser.newPage();
await page.setContent(`<!doctype html><html><head><style>
body{margin:0;background:#0b1020;color:#d6e4ff;font:17px/1.45 Consolas,"Cascadia Mono",monospace}main{padding:52px 72px}h1{color:#fff;font:700 32px system-ui;margin:0 0 8px}.meta{color:#8fb2df;font:16px system-ui;margin-bottom:28px}pre{max-height:690px;white-space:pre-wrap;overflow:hidden;overflow-wrap:anywhere;background:#111a2e;border:1px solid #30466d;border-radius:12px;padding:28px;box-shadow:0 12px 30px #0005} .badge{display:inline-block;color:#b8eadf;background:#123d43;border:1px solid #277d78;border-radius:999px;padding:5px 12px;font:700 14px system-ui;margin-bottom:22px}
</style></head><body><main><div class="badge">F01 · CLI demonstration · local only</div><h1>Course management</h1><div class="meta">Create · list · duplicate-code validation</div><pre id="output"></pre></main></body></html>`);
await page.locator('#output').evaluate((node, value) => { node.textContent = value; }, commandOutput);
await page.locator('#output').evaluate((node) => { node.textContent = ''; });
await page.evaluate(async (text) => {
  const node = document.querySelector('#output');
  for (const character of text) {
    node.textContent += character;
    await new Promise(resolve => setTimeout(resolve, character === '\n' ? 35 : 3));
  }
}, commandOutput);
await page.screenshot({ path: path.join(output, 'F01-cli.png'), fullPage: true });
await new Promise(resolve => setTimeout(resolve, 2500));
const video = page.video();
await browser.close();
await context.close();
const videoPath = await video.path();
await fs.writeFile(path.join(path.dirname(output), 'F01-cli-manifest.json'), JSON.stringify({
  feature: 'F01 Course management',
  commands: ['course create', 'course list', 'duplicate course create'],
  video: path.relative(root, videoPath),
  screenshot: path.relative(root, path.join(output, 'F01-cli.png')),
  note: 'Local recording only; not uploaded.'
}, null, 2));
console.log(path.relative(root, videoPath));
