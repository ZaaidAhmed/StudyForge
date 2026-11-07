"""Render the Stage 2 Markdown report to PDF using installed Google Chrome.

Requires only Python's standard library and Chrome/Chromium. Run with:
    py scripts/build_stage2_report.py
"""

from __future__ import annotations

import html
import os
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
from urllib.parse import quote


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "stage2" / "stage2-report.md"
OUTPUT = ROOT / "stage2-report.pdf"


def inline_markup(value: str) -> str:
    value = html.escape(value, quote=False)
    value = re.sub(r"\[([^\]]+)\]\((https?://[^ )]+|[^ )]+)\)", r'<a href="\2">\1</a>', value)
    value = re.sub(r"`([^`]+)`", r"<code>\1</code>", value)
    value = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", value)
    value = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<em>\1</em>", value)
    return value


def table_cells(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def uml_svg() -> str:
    groups = [
        (24, 25, 300, 158, "Presentation", ["Course/Material/Quiz/Agent controllers", "AiStatusController", "Picocli command objects", "Thymeleaf views · shared forms"]),
        (350, 25, 300, 158, "Application boundary + agent", ["StudyAgentFacade", "AgentCoordinator.run", "IntentAnalyzer · TaskPlanner", "PlanValidator · ToolManager", "MemoryManager · ResponseSynthesizer"]),
        (676, 25, 300, 158, "AI provider boundary", ["LlmClient", "RoutingLlmClient", "OllamaLlmAdapter", "GeminiLlmAdapter", "PromptBuilder · Parser · Grounding"]),
        (24, 225, 462, 270, "Learning services", ["CourseService · CourseMaterialService", "CourseSearchService · GroundedQuestionService", "SummarizationService · ConceptExtractionService", "FlashcardGenerationService · FlashcardReviewService", "QuizGenerationService · QuizAttemptService", "WrongAnswerExplanationService", "ProgressService · StudyPlanService", "RecommendationService · LearningHistoryService"]),
        (512, 225, 220, 270, "Domain model", ["Course · CourseDocument", "DocumentChunk", "Flashcard · FlashcardReview", "Quiz · QuizQuestion", "QuizAttempt · QuizResponse", "StudyPlan · StudyPlanItem"]),
        (758, 225, 218, 270, "Repositories", ["CourseRepository", "CourseDocumentRepository", "DocumentChunkRepository", "FlashcardRepository", "QuizRepository", "QuizAttemptRepository", "StudyPlanRepository"]),
    ]
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" width="1000" height="530" viewBox="0 0 1000 530" role="img" aria-label="StudyForge implemented UML package and class overview">',
             '<defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 z" fill="#64748b"/></marker></defs>',
             '<rect width="1000" height="530" rx="16" fill="#f8fafc"/>']
    # Dependencies are drawn behind the UML class/package boxes.
    for x1, y1, x2, y2 in [(324,104,350,104),(650,104,676,104),(500,183,270,225),(500,183,870,225),(650,104,430,225),(650,104,650,225)]:
        parts.append(f'<path d="M{x1} {y1} L{x2} {y2}" stroke="#64748b" stroke-width="2" fill="none" marker-end="url(#arrow)"/>')
    for x, y, width, height, title, lines in groups:
        parts.append(f'<rect x="{x}" y="{y}" width="{width}" height="{height}" rx="10" fill="#fff" stroke="#cbd5e1" stroke-width="2"/>')
        parts.append(f'<path d="M{x+1} {y+39} H{x+width-1}" stroke="#cbd5e1" stroke-width="1.5"/>')
        parts.append(f'<text x="{x+16}" y="{y+26}" font-family="Arial,sans-serif" font-size="17" font-weight="700" fill="#312e81">{html.escape(title)}</text>')
        for index, line in enumerate(lines):
            parts.append(f'<text x="{x+16}" y="{y+62+index*24}" font-family="Arial,sans-serif" font-size="13" fill="#0f172a">{html.escape(line)}</text>')
    parts.append('<text x="24" y="517" font-family="Arial,sans-serif" font-size="12" fill="#64748b">Solid arrows indicate implemented dependencies; domain records are persisted through course-scoped repositories.</text>')
    parts.append('</svg>')
    return "".join(parts)


def markdown_to_html(markdown: str) -> str:
    lines = markdown.splitlines()
    output: list[str] = []
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line:
            index += 1
            continue
        if line == "<!-- UML_CLASS_DIAGRAM -->":
            output.append('<figure class="uml">' + uml_svg() + '<figcaption>Implemented classes and dependencies (overview).</figcaption></figure>')
            index += 1
            continue
        if line.startswith("```"):
            code: list[str] = []
            index += 1
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code.append(lines[index])
                index += 1
            output.append("<pre><code>" + html.escape("\n".join(code)) + "</code></pre>")
            index += 1
            continue
        if line.startswith("|"):
            rows: list[list[str]] = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                row = table_cells(lines[index])
                if not all(re.fullmatch(r":?-{3,}:?", cell.replace(" ", "")) for cell in row):
                    rows.append(row)
                index += 1
            if rows:
                header, body = rows[0], rows[1:]
                table_class = ' class="wide"' if len(header) >= 5 else ""
                output.append("<table" + table_class + "><thead><tr>" + "".join(f"<th>{inline_markup(c)}</th>" for c in header) + "</tr></thead><tbody>")
                for row in body:
                    output.append("<tr>" + "".join(f"<td>{inline_markup(c)}</td>" for c in row) + "</tr>")
                output.append("</tbody></table>")
            continue
        if line.startswith("#"):
            level = min(len(line) - len(line.lstrip("#")), 3)
            content = line[level:].strip()
            output.append(f"<h{level}>{inline_markup(content)}</h{level}>")
            index += 1
            continue
        if line.startswith("- "):
            output.append("<ul>")
            while index < len(lines) and lines[index].strip().startswith("- "):
                output.append(f"<li>{inline_markup(lines[index].strip()[2:])}</li>")
                index += 1
            output.append("</ul>")
            continue
        output.append(f"<p>{inline_markup(line)}</p>")
        index += 1
    return "\n".join(output)


def find_chrome() -> str:
    candidates = [
        shutil.which("chrome"), shutil.which("chrome.exe"), shutil.which("msedge"),
        str(Path(os.environ.get("PROGRAMFILES", "C:/Program Files")) / "Google/Chrome/Application/chrome.exe"),
        str(Path(os.environ.get("LOCALAPPDATA", "")) / "Google/Chrome/Application/chrome.exe"),
        str(Path(os.environ.get("PROGRAMFILES(X86)", "C:/Program Files (x86)")) / "Microsoft/Edge/Application/msedge.exe"),
    ]
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return candidate
    raise RuntimeError("Chrome or Edge is required to render stage2-report.pdf.")


def build() -> Path:
    if not SOURCE.is_file():
        raise FileNotFoundError(f"Report source not found: {SOURCE}")
    body = markdown_to_html(SOURCE.read_text(encoding="utf-8"))
    document = f'''<!doctype html><html><head><meta charset="utf-8"><style>
@page {{ size: Letter; margin: 16mm 15mm 18mm; }}
@page wide {{ size: Letter landscape; margin: 12mm; }}
body {{ color:#172033; font: 10pt/1.42 Arial, sans-serif; }}
h1 {{ color:#1e1b4b; font-size:25pt; line-height:1.1; margin:0 0 18pt; }}
h2 {{ color:#312e81; font-size:16pt; margin:22pt 0 8pt; page-break-after:avoid; }}
h3 {{ color:#334155; font-size:12pt; margin:14pt 0 5pt; page-break-after:avoid; }}
p {{ margin:4pt 0 8pt; }}
strong {{ color:#111827; }}
a {{ color:#4f46e5; text-decoration:underline; }}
code {{ font:8.5pt Consolas,monospace; color:#3730a3; }}
ul {{ margin:4pt 0 9pt; padding-left:18pt; }}
li {{ margin:2pt 0; }}
table {{ width:100%; border-collapse:collapse; margin:7pt 0 14pt; font-size:7.3pt; page-break-inside:auto; }}
table.wide {{ page:wide; font-size:8pt; }}
thead {{ display:table-header-group; }}
tr {{ page-break-inside:avoid; }}
th {{ background:#eef2ff; color:#25215b; text-align:left; }}
th,td {{ border:0.6pt solid #cbd5e1; padding:4pt; vertical-align:top; overflow-wrap:anywhere; }}
tr:nth-child(even) td {{ background:#f8fafc; }}
.uml {{ margin:10pt 0; page-break-inside:avoid; }}
.uml svg {{ width:100%; height:auto; }}
figcaption {{ color:#64748b; font-size:8pt; margin-top:4pt; }}
@media print {{ h2,h3 {{ break-after:avoid; }} table {{ break-inside:auto; }} }}
</style></head><body>{body}</body></html>'''
    chrome = find_chrome()
    with tempfile.TemporaryDirectory(prefix="studyforge-report-") as folder:
        html_file = Path(folder) / "report.html"
        html_file.write_text(document, encoding="utf-8")
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        url = "file:///" + quote(str(html_file).replace("\\", "/"), safe=":/")
        result = subprocess.run(
            [chrome, "--headless=new", "--disable-gpu", "--no-pdf-header-footer", f"--print-to-pdf={OUTPUT}", url],
            capture_output=True, text=True, timeout=120, check=False,
        )
        if result.returncode != 0 or not OUTPUT.is_file() or OUTPUT.stat().st_size < 20_000:
            raise RuntimeError(f"Chrome PDF generation failed. stderr={result.stderr[-2000:]} stdout={result.stdout[-1000:]}")
    print(f"Created {OUTPUT} ({OUTPUT.stat().st_size} bytes)")
    return OUTPUT


if __name__ == "__main__":
    build()
