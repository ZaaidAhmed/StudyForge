# Stage 2 Implementation and Evidence Status

Last checked: November 7, 2026. The Stage 1 proposal is the baseline; see the Stage 2 compliance crosswalk and report for feature-level details.

| Requirement | Status | Evidence / remaining work |
|---|---|---|
| Stage 1 features F01-F14 | Implemented in code | Shared services, GUI/CLI as specified, tests, feature mappings, and demos are listed in `design-to-implementation-traceability.md`; hosted video contents still need final playback verification. |
| New Stage 2 feature F15 | Implemented in code | GUI provider/model selection and process-environment CLI selection with `ai status`; detailed in report/crosswalk. |
| GUI and CLI | Implemented | Spring MVC/Thymeleaf and Picocli share application services through `StudyAgentFacade`; F09/F10 are GUI-only. |
| Five meaningful patterns | Implemented | MVC, Facade, Adapter, Strategy, Command; explained with roles and code evidence in the report. Repository is supplementary. |
| LLM and bounded agent | Implemented | Ollama/Gemini adapters; retrieval, structured validation, bounded planning, allowlisted tools, and ten-event memory. Provider-dependent quality remains a limitation. |
| Stage 2 automated verification | Passed locally | Java 21 `mvnd clean test` and `mvnd package`, Nov 7: each ran 207 tests, 0 failures, 0 errors, 0 skipped. Fresh-JAR smoke returned HTTP 200 for `/`, `/courses`, `/ai/status`; CLI help checked for `quiz`, `recommend`, and `agent`; read-only quiz list, recommendation, and history commands completed for EECS3221. |
| Stage 2 videos | URLs checked; content review incomplete | All 15 YouTube and 14 GitHub release asset URLs returned HTTP 200 to unauthenticated HEAD requests. `00:00` links to the start of each standalone recording. F08/F13 CLI video evidence is still pending; playback/content has not been manually checked. |
| Weekly code commits | Listed; links response-checked | Five meaningful code commits are listed for each required period in `weekly-commit-summary.md`; all 15 commit URLs returned HTTP 200 to unauthenticated HEAD checks. |
| AI collaboration and usage costs | Partial evidence | Representative work and known model details are recorded in `ai-collaboration-log.md`. Complete token/money totals are not present in the retained repository records and must come from authoritative platform usage/billing exports or be reported as unavailable. |
| Final report PDF | Generated; visual review in progress | `stage2-report.pdf` was regenerated from `stage2-report.md` using Chrome. It is 7 pages, Letter size; inspect the final rendered pages before submission. |
| Public repository submission | Published; partially verified | Compliance matrix, report, status page, and PDF are pushed to `StudyForge/master` and returned HTTP 200. Tracked/staged release paths were checked for excluded local data; README clean-clone and instructor-access verification remain. |

## Scope and limitations

Image-only PDF OCR, embeddings/vector retrieval, authentication, cloud hosting, and multi-user identity are outside the Stage 1 proposal scope. Stage 2 basic implementation verification is included; full deterministic test planning and KUMA agent evaluation belong to Stage 3.
