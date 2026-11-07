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
- [ ] Course/document isolation and restart persistence checks recorded for final release.
- [ ] README commands and safe `.env.example` verified from clean checkout.

## Demonstrations and report

- [x] URL response checked without authentication: 15 YouTube links and 14 release assets returned HTTP 200 (Nov 7, 2026).
- [ ] Playback/content and exact segment timestamps reviewed; F08 and F13 CLI video recordings are explicitly marked pending in the index.
- [x] Three reporting periods each list five meaningful code commits.
- [x] All 15 weekly commit links returned HTTP 200 to unauthenticated HEAD checks (Nov 7, 2026).
- [x] AI tools/models/tasks/human review are documented from retained evidence; unknown model names are identified as unknown.
- [x] Missing token/money totals are explicitly reported as unavailable in retained project records, with authoritative provider exports identified as the source needed for numeric totals.
- [x] Regenerated and visually inspected the 8-page `stage2-report.pdf`; crosswalk tables use landscape pages and the UML page is readable.
- [ ] Confirm complete source and required report/docs are pushed to the same public repository.
- [ ] Check tracked files for credentials, local databases/uploads, logs, or generated runtime artifacts.
