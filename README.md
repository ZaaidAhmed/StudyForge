# StudyForge

[![Ollama](https://img.shields.io/badge/Ollama-local%20LLM%20experimentation-000000?style=flat&logo=ollama&logoColor=white)](https://ollama.com/)
[![Course project](https://img.shields.io/badge/Project-EECS%203311-6A1B9A?style=flat)](https://www.eecs.yorku.ca/)
[![Status](https://img.shields.io/badge/Status-design%20stage-blue?style=flat)](https://github.com/ZaaidAhmed/StudyForge)

StudyForge is a design-stage study-support project created for an EECS 3311 course project. The repository currently contains the system design proposal and supporting documentation rather than a production deployment.

## Current focus

- Exploring how locally hosted language models can support study workflows.
- Using Ollama for local AI experimentation and learning.
- Documenting product scope, system structure, and implementation decisions before expanding the application.

## Why this project matters

StudyForge reflects an interest in building practical AI tools with clear boundaries, understandable architecture, and a useful end-user purpose. The design-first workflow helps connect user needs, technical decisions, and future implementation work.

## Repository contents

- [Stage 1 design proposal](docs/StudyForge_Stage_1_Design_Proposal.pdf)
- Design and planning artifacts for the course project

## Project status

This is an active course project. The repository is currently documentation-led; implementation and evaluation details will be added as the project progresses.

## Author

Zaaid Ahmed — [GitHub](https://github.com/ZaaidAhmed) · [Portfolio](https://zaaidahmed.netlify.app/)

## Proposed architecture

StudyForge is currently design-stage, so this describes the intended workflow rather than a deployed production system.

    User
      |
      v
    StudyForge study workflow
      |
      +--> Notes and study-material ingestion
      |
      +--> Prompt and context assembly
      |
      v
    Ollama local language model
      |
      v
    Generated explanations, questions, and study feedback

## How it would work

1. A learner provides study material or selects a study task.
2. The application prepares relevant context and a structured prompt.
3. Ollama runs the language model locally for experimentation.
4. StudyForge returns a study-oriented response such as a question, explanation, or review prompt.
5. The design can be evaluated and refined before implementation details are added.
