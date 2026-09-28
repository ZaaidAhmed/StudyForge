# Stage 1 - Project Overview

## Project

StudyForge AI - Adaptive Study & Learning Agent.

## Problem

Students have course material, activity results, and study decisions spread across disconnected resources. StudyForge AI organizes those resources and combines deterministic learning software with an LLM-backed agent that retrieves evidence, uses progress memory, selects tools, and produces bounded study actions.

The target users are university or college students studying from lecture slides, notes, study guides, PDFs, Markdown, and text files. The agent can answer grounded questions, summarize material, extract concepts, generate flashcards and quizzes, explain mistakes, identify weak topics, recommend the next action, and build an exam plan within time constraints.

An AI agent is appropriate because a student request often requires a sequence of decisions: determine intent, retrieve relevant material, inspect progress and history, choose tools, generate a structured learning activity, validate it, persist it, and adapt future recommendations. Deterministic services retain authority over storage, retrieval, grading, validation, permissions, and scheduling.

## Goals

- Provide a usable web GUI and equivalent major CLI operations.
- Demonstrate object-oriented design, clear boundaries, and real design patterns.
- Keep retrieval, grading, progress, validation, and persistence deterministic.
- Use Ollama through an `LlmClient` abstraction for AI capabilities.
- Make each feature traceable to a use case, class, method, test, and UML artifact.

The planned model integration is Ollama through its local HTTP API. `OllamaLlmAdapter` implements `LlmClient`; `PromptBuilder` creates bounded grounded prompts, while structured response parsers validate model output before application services persist it. The application remains usable when Ollama is unavailable because deterministic functions do not depend on the model.

## Non-goals

Authentication, external integrations, OCR, speech, vector search, distributed deployment, and mobile applications are outside the first release.

## Primary demonstration

Create EECS3311, import notes, ask a grounded question, generate and complete a quiz, update weak-topic progress, request a next step, and create a time-constrained exam plan. The same application layer is reachable from the CLI.
