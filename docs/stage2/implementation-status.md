# Stage 2 Implementation and Evidence Status

Last checked: November 7, 2026. The Stage 1 proposal is the baseline; see the Stage 2 compliance crosswalk and report for feature-level details.

| Requirement | Status | Evidence / remaining work |
|---|---|---|
| Stage 1 features F01-F14 | Implemented in code | Shared services, GUI/CLI as specified, tests, feature mappings, and demos are listed in `design-to-implementation-traceability.md`; hosted video contents still need final playback verification. |
| New Stage 2 feature F15 | Implemented in code | GUI provider/model selection and process-environment CLI selection with `ai status`; detailed in report/crosswalk. |
| GUI and CLI | Implemented | Spring MVC/Thymeleaf and Picocli share application services through `StudyAgentFacade`; F09/F10 are GUI-only. |
| Five meaningful patterns | Implemented | MVC, Facade, Adapter, Strategy, Command; explained with roles and code evidence in the report. Repository is supplementary. |
| LLM and bounded agent | Implemented | Ollama/Gemini adapters; retrieval, structured validation, bounded planning, allowlisted tools, and ten-event memory. Provider-dependent quality remains a limitation. |
| Stage 2 automated verification | Passed locally, from clean clone, and in GitHub Actions | Java 21 `mvnd clean test package` on the published `fa47c4b` checkout: 207 tests, 0 failures, 0 errors, 0 skipped. The same command passed from a clean clone; fresh-JAR smoke returned HTTP 200 for `/`, `/courses`, and `/ai/status`. GitHub Actions `Verify` passed on `fa47c4b` ([run 42416941967](https://github.com/ZaaidAhmed/StudyForge/actions/runs/42416941967)). README CLI walkthrough, isolated course/document access, and persistence across separate CLI process runs were verified Nov 7, 2026. |
| Stage 2 videos | URLs checked; partial playback review | All 15 YouTube and 14 GitHub release asset URLs returned HTTP 200 to unauthenticated HEAD requests. F08 MP4 review confirms its CLI `quiz generate/list` segment begins at 01:35. F13's 03:05 segment is a general CLI walkthrough and does not show `recommend` or `agent`; whether F14 demonstrates either command has not been verified. Most `00:00` timestamps in the public index remain unverified, and the other feature recordings still need playback review. |
| Weekly code commits | Listed; links response-checked | Five meaningful code commits are listed for each required period in `weekly-commit-summary.md`; all 15 commit URLs returned HTTP 200 to unauthenticated HEAD checks. |
| AI collaboration and usage costs | Partial evidence | Representative work and known model details are recorded in `ai-collaboration-log.md`. Complete token/money totals are not present in the retained repository records and must come from authoritative platform usage/billing exports or be reported as unavailable. |
| Final report PDF | Existing 8-page PDF; refresh pending | The PDF was previously rendered and inspected, but it predates the latest report-source updates and still needs regeneration and visual review. |
| Public repository submission | Published; partially verified | Current `master` is `fa47c4b`; its GitHub Actions run passed. The clean-clone README walkthrough, safe sample configuration, tracked-path/key-format audit, and isolated course/document/persistence checks passed Nov 7, 2026. Instructor access remains unconfirmed. The Markdown report now cites the current CI run; the PDF must be regenerated to match. |

## Scope and limitations

Image-only PDF OCR, embeddings/vector retrieval, authentication, cloud hosting, and multi-user identity are outside the Stage 1 proposal scope. Stage 2 basic implementation verification is included; full deterministic test planning and KUMA agent evaluation belong to Stage 3.
