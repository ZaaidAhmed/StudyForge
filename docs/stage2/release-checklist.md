# Stage 2 Release Checklist

Updated November 7, 2026. Check items only after the final evidence exists.

## Implementation and traceability

- [x] F01-F14 Stage 1 features and F15 addition mapped to actual implementation, tests, interface availability, and demos.
- [x] At least five implemented design patterns explained with participants, roles, files/methods, and rationale.
- [x] Stage 1 changes are recorded as original design → implementation → reason → impact.
- [x] Stage 2 implementation-specific class, use-case, and affected sequence diagrams added under `docs/stage2/uml/`.
- [ ] Recheck every crosswalk claim against the final release commit.

## Verification

- [x] `mvnd clean test` on Java 21: 207 tests, 0 failures, 0 errors, 0 skipped (Nov 7, 2026).
- [x] `mvnd package` on Java 21; 207 tests, 0 failures, 0 errors, 0 skipped (Nov 7, 2026).
- [x] Fresh-JAR smoke: `/`, `/courses`, `/ai/status` returned HTTP 200 on port 18080; packaged CLI help checked for `quiz`, `recommend`, and `agent`; read-only quiz list, recommendation, and history commands completed for EECS3221.
- [x] GitHub Actions `Verify` passed on Ubuntu for `fa47c4b` ([run 42416941967](https://github.com/ZaaidAhmed/StudyForge/actions/runs/42416941967)): 207 tests, 0 failures/errors/skips; package and fresh-JAR smoke passed.
- [x] Clean-clone README walkthrough on Java 21: `sf --help`, `sf course list`, and `sf material list` passed; `.env.example` is present and contains placeholders only (Nov 7, 2026).
- [x] Course isolation and restart persistence in an isolated clean-clone H2 database: course A persisted across separate CLI process runs; course B could not list or open course A's material (Nov 7, 2026).

## Demonstrations and report

- [x] URL response checked without authentication: 15 YouTube links and 14 release assets returned HTTP 200 (Nov 7, 2026).
- [x] F08 and F13 published MP4s sampled in Chrome. F08 CLI segment at 01:35 demonstrates quiz generation/listing; F13's 03:05 CLI walkthrough does not show `recommend`/`agent`.
- [ ] Review remaining feature recordings and record verified timestamps. F13 `recommend`/`agent` evidence in either F13 or F14 recording remains unverified.
- [x] Three reporting periods each list five meaningful code commits.
- [x] All 15 weekly commit links returned HTTP 200 to unauthenticated HEAD checks (Nov 7, 2026).
- [x] AI tools/models/tasks/human review are documented from retained evidence; unknown model names are identified as unknown.
- [x] Missing token/money totals are explicitly reported as unavailable in retained project records, with authoritative provider exports identified as the source needed for numeric totals.
- [x] The existing 8-page `stage2-report.pdf` was previously regenerated and visually inspected; it predates the latest Markdown report changes and must be regenerated before final submission.
- [x] Push the report, diagrams, README links, compliance matrix, and evidence docs to public `StudyForge/master`; key GitHub pages and PDF returned HTTP 200.
- [ ] Confirm instructor access to the submitted revision.
- [x] Audit the published clean-clone tracked paths and key formats: no tracked local databases, uploads, logs, build output, local-only files, or matching provider/GitHub/OpenAI credential formats; `.gitignore` excludes local runtime paths (Nov 7, 2026).
- [ ] Regenerate and visually inspect `stage2-report.pdf` from the updated Markdown; the checked-in PDF still contains the earlier `c30308c` verification reference.
