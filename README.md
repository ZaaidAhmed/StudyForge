# StudyForge AI

StudyForge is a course-scoped study workspace for importing materials, asking grounded questions, generating practice content, tracking learning activity, and planning study sessions. The web application uses Spring MVC and Thymeleaf; the CLI uses Picocli. Both share the same services and local H2 database.

## Contents

- [Quick evaluation guide](#quick-evaluation-guide)
- [Start here](#start-here)
- [Run the web application](#run-the-web-application)
- [Run the CLI](#run-the-cli)
- [F01-F15 feature and CLI guide](#f01-f15-feature-and-cli-guide)
- [Choose a local or hosted AI provider](#choose-a-local-or-hosted-ai-provider)
- [Project map](#project-map)
- [Design and demonstration documentation](#design-and-demonstration-documentation)
- [Configuration and data](#configuration-and-data)
- [Limitations](#limitations)

## Quick evaluation guide

For a fast review of the submitted project:

1. Read the [Stage 2 report](docs/stage2/stage2-report.md) for feature mapping, architecture, design changes, and verification results.
2. Use the [F01-F15 demonstration index](docs/stage2/demo-index.md) to open the feature videos. The index identifies GUI-only workflows and CLI coverage.
3. To run StudyForge locally, follow [Run the web application](#run-the-web-application). The course workspace opens at [http://localhost:8080](http://localhost:8080).
4. To inspect the command-line workflows, follow [Run the CLI](#run-the-cli), then use the [F01-F15 command guide](#f01-f15-feature-and-cli-guide).

The [PDF report](stage2-report.pdf) is the submission copy; the Markdown report is its editable source.

## Start here

### Requirements

- Java 21
- Maven 3.9 or newer
- Ollama is optional for local AI. Gemini requires an API key for hosted AI.

No Node.js, Docker, or external database server is needed to run the application. Node.js is only needed for the optional browser demo recorder in `scripts/`.

### Windows PowerShell

Run these commands from the repository root. If `java` is not on `PATH`, set `JAVA_HOME` to your Java 21 installation first:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

```powershell
java -version
mvn -version
mvn clean test
mvn package
```

### macOS or Linux

```bash
java -version
mvn -version
mvn clean test
mvn package
```

`mvn package` builds the executable JAR at `target/studyforge-ai-0.1.0-SNAPSHOT.jar` and runs the tests as part of the Maven package lifecycle.
If you use Maven Daemon, substitute `mvnd` for `mvn` in these commands.

## Run the web application

From the repository root:

```text
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080), choose a course, and use its materials and study tools. Stop the server with `Ctrl+C`.

To run the packaged JAR instead:

```text
java -jar target/studyforge-ai-0.1.0-SNAPSHOT.jar
```

## Run the CLI

Build the JAR first with `mvn package`. Run CLI commands from the repository root so the CLI uses the same `.env`, H2 database, and uploaded files as the web app.

### Windows PowerShell launcher

Define this function once in the current PowerShell window:

```powershell
function sf {
    & java '-Dspring.main.web-application-type=none' '-Dspring.profiles.active=cli' `
        -jar '.\target\studyforge-ai-0.1.0-SNAPSHOT.jar' @args
}
```

### macOS/Linux launcher

Define this function once in the current terminal:

```bash
sf() {
  java -Dspring.main.web-application-type=none \
    -Dspring.profiles.active=cli \
    -jar target/studyforge-ai-0.1.0-SNAPSHOT.jar "$@"
}
```

Check the available commands and options:

```text
sf --help
sf course --help
sf material --help
sf study-plan create --help
```

Start by checking your course code and the material IDs in that course:

```text
sf course list
sf course show --course EECS3221
sf material list --course EECS3221
```

Use a document ID returned by `material list` for commands that take `--document`. Replace the sample course code and IDs below with values from your own course.

## F01-F15 feature and CLI guide

These are the CLI commands implemented by the application. F09 and F10 are GUI-only workflows; F15 uses environment configuration because the CLI has no provider-switch subcommand.

| Feature | CLI commands | Notes |
|---|---|---|
| F01 - Course management | `sf course list`; `sf course create --code EECS3311 --name "Software Design"`; `sf course show --course EECS3311` | Create, list, and show courses. |
| F02 - Material import and processing | `sf material add --course EECS3221 --file .\notes.md`; `sf material list --course EECS3221`; `sf material show --course EECS3221 --id 8` | Add accepts PDF, TXT, and Markdown files. Use the ID shown by `material list`. |
| F03 - Material search | `sf material search --course EECS3221 --query "CPU scheduling" --limit 5` | `--limit` is optional; the default is 5 and the maximum is 20. |
| F04 - Grounded course Q&A | `sf ask --course EECS3221 --question "What is a thread?"` | Answers from the selected course's available material. |
| F05 - Document and topic summaries | `sf summary document --course EECS3221 --document 8`; `sf summary topic --course EECS3221 --topic "CPU scheduling"` | Document IDs come from `material list`. |
| F06 - Concept extraction | `sf concepts extract --course EECS3221 --document 8` | Extracts concepts from one ready document. |
| F07 - Flashcards | `sf flashcards generate --course EECS3221 --document 8 --count 10`; `sf flashcards list --course EECS3221`; `sf flashcards review --course EECS3221 --card 1 --rating GOOD` | Ratings: `INCORRECT`, `HARD`, `GOOD`, or `EASY`. |
| F08 - Quiz generation | `sf quiz generate --course EECS3221 --document 8 --count 10`; `sf quiz list --course EECS3221` | CLI generates and lists quizzes. Quiz-taking is F09. |
| F09 - Quiz taking and grading | No CLI command | Take and submit quizzes in the web application. |
| F10 - Wrong-answer explanation | No CLI command | Request an explanation from an incorrect answer in the web quiz results. |
| F11 - Progress and weak topics | `sf progress --course EECS3221` | Reports the course progress summary and topic results. |
| F12 - Study plans | `sf study-plan create --course EECS3221 --start 2026-11-05 --target 2026-11-19 --minutes 120 --strategy BALANCED`; `sf study-plan list --course EECS3221` | Strategies: `BALANCED`, `WEAK_TOPIC_FOCUSED`, or `EXAM_CRAM` (shown as Exam focused in the UI). |
| F13 - Recommendations and agent assistance | `sf recommend --course EECS3221`; `sf agent --course EECS3221 --request "Help me study what I am weakest at"` | The agent is limited to supported course-study actions. |
| F14 - Learning history and bounded memory | `sf history --course EECS3221` | Shows course-scoped learning activity. |
| F15 - Local/cloud AI provider selection | Set provider environment variables, then run `sf ai status` | There is no `ai switch` CLI command. See [provider setup](#choose-a-local-or-hosted-ai-provider). |

The same CLI commands work in PowerShell and Bash after defining the shell-specific `sf` function above. In PowerShell, quote file paths containing spaces, for example `--file '.\class notes.pdf'`.

## Choose a local or hosted AI provider

AI features require a reachable configured provider. Deterministic course, material, and progress features remain available without AI. The AI status command reports which provider and model the CLI process is using:

```text
sf ai status
```

### Local Ollama

Install and start Ollama separately, then download one of the supported models if it is not already installed:

```text
ollama list
ollama pull qwen3:4b
```

PowerShell configuration for the current terminal:

```powershell
$env:COURSEPILOT_AI_PROVIDER = 'ollama'
$env:COURSEPILOT_AI_MODEL = 'qwen3:4b'
$env:COURSEPILOT_AI_BASE_URL = 'http://localhost:11434'
sf ai status
```

macOS/Linux configuration for the current terminal:

```bash
export COURSEPILOT_AI_PROVIDER=ollama
export COURSEPILOT_AI_MODEL=qwen3:4b
export COURSEPILOT_AI_BASE_URL=http://localhost:11434
sf ai status
```

The other supported Ollama model is `qwen-claude-14b:latest`.

### Hosted Gemini

Set the key in your shell environment; do not commit it or put a real key in a shared file.

PowerShell:

```powershell
$env:COURSEPILOT_AI_PROVIDER = 'gemini'
$env:COURSEPILOT_AI_MODEL = 'gemini-3.5-flash-lite'
$env:COURSEPILOT_AI_BASE_URL = 'https://generativelanguage.googleapis.com'
$env:GEMINI_API_KEY = '<your-gemini-api-key>'
sf ai status
```

macOS/Linux:

```bash
export COURSEPILOT_AI_PROVIDER=gemini
export COURSEPILOT_AI_MODEL=gemini-3.5-flash-lite
export COURSEPILOT_AI_BASE_URL=https://generativelanguage.googleapis.com
export GEMINI_API_KEY='<your-gemini-api-key>'
sf ai status
```

Other supported Gemini models are `gemini-3.8-flash` and `gemini-2.5-flash`. Shell environment variables apply to processes launched from that terminal. For a persistent local setting, copy `.env.example` to `.env`, set the provider/model and any required key there, then restart the application. The local `.env` file is ignored by Git.

## Project map

| Path | What it contains |
|---|---|
| `src/main/java/.../application/` | Use cases, services, and application DTOs shared by web and CLI. |
| `src/main/java/.../domain/` | Course and learning entities, repository contracts, and domain rules. |
| `src/main/java/.../infrastructure/` | H2 repositories, document extraction/storage, AI adapters, and provider routing. |
| `src/main/java/.../web/` | Spring MVC controllers and web view models. |
| `src/main/java/.../cli/` | Picocli commands and the non-web CLI entry point. |
| `src/main/resources/templates/` | Thymeleaf pages and layouts. |
| `src/main/resources/static/` | Application CSS and JavaScript. |
| `src/test/` | Unit, service, and MVC tests. |
| `docs/stage1/` and `docs/stage2/` | Feature requirements, architecture, traceability, demos, and delivery records. |
| `scripts/` | Verification and optional local demo-recording tools. |
| `data/` | Local H2 database and uploaded material files; generated at runtime and not committed. |

## Design and demonstration documentation

- [Stage 1 feature specifications](docs/stage1/02-feature-specifications.md)
- [Stage 1 architecture](docs/stage1/07-architecture.md)
- [Stage 2 implementation report (Markdown)](docs/stage2/stage2-report.md) | [PDF](stage2-report.pdf)
- [F01-F15 demo index](docs/stage2/demo-index.md)
- [Weekly commit summary](docs/stage2/weekly-commit-summary.md)
- [AI collaboration and usage record](docs/stage2/ai-collaboration-log.md)
- [Local demo recorder instructions](scripts/README.md)

## Configuration and data

- `.env.example` documents local AI settings. Copy it to `.env` for local overrides; never commit API keys.
- Real process environment variables take precedence over `.env` values.
- H2 data is stored under `./data/coursepilot`; uploaded documents are stored under `./data/uploads`.
- Default upload limit is 10 MB. Text-based PDF, TXT, and Markdown files are supported.

## Limitations

- OCR for image-only/scanned PDFs is not implemented.
- Material retrieval is deterministic lexical search; there are no embeddings or vector database.
- AI output quality depends on the configured provider/model. Grounding and source validation reduce, but cannot eliminate, model errors.
- F09 quiz taking/grading and F10 wrong-answer explanations are GUI workflows; the CLI does not expose those operations.
