# Local demonstration recorder

The recorder creates local WebM videos and screenshots for F01–F14. It never uploads recordings and never changes `docs/stage2/demo-index.md`.

Install the isolated browser dependency once:

```powershell
npm install --prefix scripts
npx --prefix scripts playwright install chromium
```

Run a recovery-mode recording without using the normal database:

```powershell
node scripts/record-demos.mjs --ai-mode recovery
```

Run live AI recordings with Ollama available:

```powershell
node scripts/record-demos.mjs --ai-mode live
```

Use `--skip-build` when `target/studyforge-ai-0.1.0-SNAPSHOT.jar` is already fresh. Use `--headed` when reviewing the workflow interactively. Outputs are written under `tmp/stage2-demos/`; the local companion index is `docs/stage2/demo-index.local.md`. Public URLs remain `Pending upload` until the videos are reviewed and uploaded manually.
