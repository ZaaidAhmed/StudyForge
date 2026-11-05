package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.AgentChatEntry;
import ca.yorku.eecs3311.coursepilot.application.dto.HistoryEvent;
import ca.yorku.eecs3311.coursepilot.application.service.LearningHistoryService;
import ca.yorku.eecs3311.coursepilot.application.service.HistoryActivitySummarizer;
import ca.yorku.eecs3311.coursepilot.application.service.ProgressService;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/courses/{courseId}/agent")
public class AgentController {
    private static final String HISTORY_PREFIX = "studyforge.agent.history.";
    private static final String PENDING_ACTION_PREFIX = "studyforge.agent.pending-action.";
    private final StudyAgentFacade facade;
    private final ProgressService progress;
    private final StudyPlanReviewService reviewQueue;
    private final LearningHistoryService learningHistory;

    public AgentController(StudyAgentFacade facade, ProgressService progress,
                          StudyPlanReviewService reviewQueue, LearningHistoryService learningHistory) {
        this.facade = facade;
        this.progress = progress;
        this.reviewQueue = reviewQueue;
        this.learningHistory = learningHistory;
    }

    @GetMapping
    public String page(@PathVariable Long courseId, HttpSession session, Model model) {
        addPageData(courseId, session, model);
        return "agent/index";
    }

    @PostMapping("/clear")
    public String clearChat(@PathVariable Long courseId, HttpSession session) {
        facade.getCourse(courseId);
        session.removeAttribute(HISTORY_PREFIX + courseId);
        session.removeAttribute(PENDING_ACTION_PREFIX + courseId);
        return "redirect:/courses/" + courseId + "/agent";
    }

    @PostMapping("/delete-generated")
    public String deleteAssistantGenerated(@PathVariable Long courseId, RedirectAttributes redirectAttributes) {
        int deleted = facade.deleteAssistantCreatedStudyContent(courseId);
        redirectAttributes.addFlashAttribute("successMessage", deleted == 0
                ? "There are no assistant-created quizzes or flashcards to remove."
                : "Removed " + deleted + " assistant-created study item(s).");
        return "redirect:/courses/" + courseId + "/agent";
    }

    @PostMapping
    public String ask(@PathVariable Long courseId,
                      @RequestParam(required = false, defaultValue = "") String request,
                      HttpSession session, Model model) {
        List<AgentChatEntry> messages = history(session, courseId);
        model.addAttribute("request", request);
        try {
            if (request.isBlank()) throw new IllegalArgumentException("Write a study request or choose a quick action.");
            String submittedRequest = request.trim();
            String result;
            PendingAssistantAction pending = pendingAction(session, courseId);
            if (pending != null && isCancellation(submittedRequest)) {
                session.removeAttribute(PENDING_ACTION_PREFIX + courseId);
                result = "Okay, cancelled. Tell me what you'd like to do instead.";
            } else if (pending != null && "SUMMARY".equals(pending.kind())) {
                SummaryChoice choice = resolveSummaryChoice(submittedRequest, readyMaterials(courseId));
                if (choice == null || choice.clarify()) result = summaryScopeQuestion(readyMaterials(courseId));
                else if (choice.cancelled()) {
                    session.removeAttribute(PENDING_ACTION_PREFIX + courseId);
                    result = "Okay, I won't create a summary. Tell me what you'd like to do instead.";
                } else {
                    session.removeAttribute(PENDING_ACTION_PREFIX + courseId);
                    result = facade.agentAssist(courseId, choice.agentRequest());
                }
            } else if (pending != null && "GENERATE".equals(pending.kind())) {
                result = continueGeneration(courseId, submittedRequest, pending, session);
            } else {
                String generationType = generationRequestType(submittedRequest);
                if (generationType != null) {
                    List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready = readyMaterials(courseId);
                    if (ready.isEmpty()) result = "There are no ready materials in this course yet. Upload a document before generating study content.";
                    else {
                        var selected = selectReadyMaterial(submittedRequest, ready);
                        int count = generationType.equals("quiz") ? requestedQuizCount(submittedRequest) : requestedFlashcardCount(submittedRequest);
                        result = advanceGeneration(courseId, new PendingAssistantAction("GENERATE", generationType,
                                selected == null ? null : selected.id(), count == 0 ? null : count, "DETAILS"), ready, session);
                    }
                } else {
                    List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready = readyMaterials(courseId);
                    if (isSummaryRequest(submittedRequest) && ready.size() > 1 && !explicitlyRequestsAll(submittedRequest)) {
                        var selectedDocument = findNamedMaterial(submittedRequest, ready);
                        if (selectedDocument == null) {
                            session.setAttribute(PENDING_ACTION_PREFIX + courseId, new PendingAssistantAction("SUMMARY", "summary", null, null, "SCOPE"));
                            result = summaryScopeQuestion(ready);
                        } else result = facade.agentAssist(courseId, "Summarize document: " + selectedDocument.originalFilename());
                    } else result = facade.agentAssist(courseId, submittedRequest);
                }
            }
            messages.add(toChatEntry(courseId, request.trim(), result));
            if (messages.size() > 8) messages.removeFirst();
            session.setAttribute(HISTORY_PREFIX + courseId, messages);
            model.addAttribute("request", "");
        } catch (RuntimeException exception) {
            model.addAttribute("agentError", exception.getMessage() == null
                    ? "The study assistant could not complete that request. Try another course task." : exception.getMessage());
        }
        addPageData(courseId, session, model);
        return "agent/index";
    }

    private List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> readyMaterials(Long courseId) {
        return facade.listMaterials(courseId).stream().filter(document -> "READY".equals(document.processingStatus())).toList();
    }

    private PendingAssistantAction pendingAction(HttpSession session, Long courseId) {
        Object value = session.getAttribute(PENDING_ACTION_PREFIX + courseId);
        return value instanceof PendingAssistantAction action ? action : null;
    }

    private String continueGeneration(Long courseId, String reply, PendingAssistantAction pending, HttpSession session) {
        List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready = readyMaterials(courseId);
        if (ready.isEmpty()) {
            session.removeAttribute(PENDING_ACTION_PREFIX + courseId);
            return "There are no ready materials in this course now. Upload or reprocess a document before generating study content.";
        }
        if ("CONFIRM".equals(pending.stage()) && isConfirmation(reply)) {
            var selected = ready.stream().filter(document -> document.id().equals(pending.documentId())).findFirst().orElse(null);
            if (selected == null) {
                session.removeAttribute(PENDING_ACTION_PREFIX + courseId);
                return "That material is no longer ready. Start again and choose a ready document.";
            }
            if (pending.count() == null || pending.count() < 1 || pending.count() > 20)
                return generationCountQuestion(pending.generationType(), false, selected.originalFilename());
            String result = generateFromMaterial(courseId, pending.generationType(), selected.originalFilename(), pending.count());
            session.removeAttribute(PENDING_ACTION_PREFIX + courseId);
            return result;
        }

        Long documentId = pending.documentId();
        Integer count = pending.count();
        if ("CONFIRM".equals(pending.stage())) {
            int revisedCount = requestedCountInReply(reply, pending.generationType());
            boolean countOnly = reply.trim().matches("\\d{1,2}") || normalizeChoice(reply).matches(
                    "(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty)");
            if (!countOnly && revisedCount == 0) {
                var alternate = selectReadyMaterial(reply, ready);
                if (alternate != null) documentId = alternate.id();
            }
            if (revisedCount != 0) count = revisedCount > 0 && revisedCount <= 20 ? revisedCount : null;
        }
        Long originalDocumentId = pending.documentId();
        if (documentId == null) {
            var selected = selectReadyMaterial(reply, ready);
            if (selected != null) documentId = selected.id();
        }
        if (count == null && (originalDocumentId != null || reply.matches("(?is).*\\b(?:quiz|questions?|flashcards?)\\b.*"))) {
            int parsed = requestedCountInReply(reply, pending.generationType());
            if (parsed > 0 && parsed <= 20) count = parsed;
            else if (parsed < 0) return generationCountQuestion(pending.generationType(), true, selectedName(ready, documentId));
        }
        PendingAssistantAction updated = new PendingAssistantAction("GENERATE", pending.generationType(), documentId, count, "DETAILS");
        return advanceGeneration(courseId, updated, ready, session);
    }

    private String advanceGeneration(Long courseId, PendingAssistantAction action,
                                     List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready,
                                     HttpSession session) {
        Long documentId = action.documentId();
        var selected = ready.stream().filter(document -> document.id().equals(documentId)).findFirst().orElse(null);
        Integer count = action.count();
        if (selected == null) {
            session.setAttribute(PENDING_ACTION_PREFIX + courseId,
                    new PendingAssistantAction("GENERATE", action.generationType(), null, count, "DETAILS"));
            return materialSelectionQuestion(action.generationType(), ready);
        }
        if (count == null || count < 1 || count > 20) {
            session.setAttribute(PENDING_ACTION_PREFIX + courseId,
                    new PendingAssistantAction("GENERATE", action.generationType(), selected.id(), null, "DETAILS"));
            return generationCountQuestion(action.generationType(), count != null, selected.originalFilename());
        }
        PendingAssistantAction confirmation = new PendingAssistantAction("GENERATE", action.generationType(), selected.id(), count, "CONFIRM");
        session.setAttribute(PENDING_ACTION_PREFIX + courseId, confirmation);
        String quantity = action.generationType().equals("quiz") ? count + " quiz questions" : count + " flashcards";
        return "Ready to create " + quantity + " from " + selected.originalFilename() + ". Reply 'yes' to create them, or choose a different material/count.";
    }

    private int requestedCountInReply(String reply, String type) {
        int explicit = type.equals("quiz") ? requestedQuizCount(reply) : requestedFlashcardCount(reply);
        if (explicit != 0) return explicit;
        if (reply.trim().matches("\\d{1,2}")) {
            int value = Integer.parseInt(reply.trim());
            return value >= 1 && value <= 20 ? value : -1;
        }
        if (normalizeChoice(reply).matches("(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty)"))
            return pendingReplyCount(reply);
        return 0;
    }

    private boolean isConfirmation(String reply) {
        return java.util.Set.of("yes", "y", "confirm", "go ahead", "create", "generate", "do it")
                .contains(normalizeChoice(reply));
    }

    private String selectedName(List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready, Long documentId) {
        return ready.stream().filter(document -> document.id().equals(documentId))
                .map(ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse::originalFilename)
                .findFirst().orElse("the selected material");
    }

    private record PendingAssistantAction(String kind, String generationType, Long documentId, Integer count,
                                          String stage) implements java.io.Serializable { }

    private boolean isSummaryRequest(String request) {
        return request.toLowerCase(java.util.Locale.ROOT).contains("summar");
    }

    private boolean isFlashcardGenerationRequest(String request) {
        String normalized = request.toLowerCase(java.util.Locale.ROOT);
        return (normalized.contains("flashcard") || normalized.contains("flash card"))
                && java.util.List.of("create", "build", "make", "generate").stream().anyMatch(normalized::contains);
    }

    private boolean isQuizGenerationRequest(String request) {
        String normalized = request.toLowerCase(java.util.Locale.ROOT);
        return (normalized.contains("quiz") || normalized.contains("test me"))
                && java.util.List.of("create", "build", "make", "generate", "quiz me", "test me")
                .stream().anyMatch(normalized::contains);
    }

    private String generationRequestType(String request) {
        if (isQuizGenerationRequest(request)) return "quiz";
        if (isFlashcardGenerationRequest(request)) return "flashcards";
        return null;
    }

    private ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse selectReadyMaterial(
            String request, List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready) {
        var named = findNamedMaterial(request, ready);
        if (named != null) return named;
        return ordinalSelection(normalizeChoice(request), ready);
    }

    private String materialSelectionQuestion(String type,
            List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready) {
        if (ready.isEmpty()) return "This course has no ready materials. Upload a document, then ask me to create " + type + ".";
        String options = java.util.stream.IntStream.range(0, ready.size()).mapToObj(index ->
                (index + 1) + ". " + ready.get(index).originalFilename())
                .collect(java.util.stream.Collectors.joining("\n"));
        return "Which course material should I use to generate " + (type.equals("quiz") ? "a quiz" : "flashcards")
                + "? Reply with a number or document name.\n\n" + options;
    }

    private String generationCountQuestion(String type, boolean invalid, String documentName) {
        String unit = type.equals("quiz") ? "questions" : "flashcards";
        return (invalid ? "Choose a count from 1 to 20. " : "")
                + "How many " + unit + " should I generate from " + documentName + "? Choose 1 to 20.";
    }

    private int pendingReplyCount(String request) {
        java.util.regex.Matcher digits = java.util.regex.Pattern.compile("\\b(\\d{1,2})\\b").matcher(request);
        if (digits.find()) {
            int count = Integer.parseInt(digits.group(1));
            return count >= 1 && count <= 20 ? count : -1;
        }
        return numberWordCount(request, false);
    }

    private String generateFromMaterial(Long courseId, String type, String documentName, int count) {
        String action = type.equals("quiz") ? "quiz with " + count + " questions" : count + " flashcards";
        return facade.agentAssist(courseId, "Create a " + action + " from document " + documentName);
    }

    /** Returns 0 when no count was supplied and -1 when a supplied count is out of range. */
    private int requestedQuizCount(String request) {
        java.util.regex.Matcher digits = java.util.regex.Pattern.compile(
                "(?i)\\b(\\d{1,2})\\s*[- ]?\\s*(?:quiz(?:zes)?|questions?)\\b|\\b(?:quiz(?:zes)?|questions?)\\D{0,12}(\\d{1,2})\\b").matcher(request);
        if (digits.find()) return validatedQuizCount(digits.group(1) == null ? digits.group(2) : digits.group(1));
        return numberWordCountForQuiz(request, true);
    }

    private int pendingQuizCount(String request) {
        java.util.regex.Matcher digits = java.util.regex.Pattern.compile("\\b(\\d{1,2})\\b").matcher(request);
        if (digits.find()) return validatedQuizCount(digits.group(1));
        return numberWordCountForQuiz(request, false);
    }

    private int validatedQuizCount(String value) {
        int count = Integer.parseInt(value);
        return count >= 1 && count <= 20 ? count : -1;
    }

    private int numberWordCountForQuiz(String request, boolean requireQuizContext) {
        String normalized = request.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]+", " ").trim();
        java.util.Map<String, Integer> words = java.util.Map.ofEntries(
                java.util.Map.entry("one", 1), java.util.Map.entry("two", 2), java.util.Map.entry("three", 3),
                java.util.Map.entry("four", 4), java.util.Map.entry("five", 5), java.util.Map.entry("six", 6),
                java.util.Map.entry("seven", 7), java.util.Map.entry("eight", 8), java.util.Map.entry("nine", 9),
                java.util.Map.entry("ten", 10), java.util.Map.entry("eleven", 11), java.util.Map.entry("twelve", 12),
                java.util.Map.entry("thirteen", 13), java.util.Map.entry("fourteen", 14), java.util.Map.entry("fifteen", 15),
                java.util.Map.entry("sixteen", 16), java.util.Map.entry("seventeen", 17), java.util.Map.entry("eighteen", 18),
                java.util.Map.entry("nineteen", 19), java.util.Map.entry("twenty", 20));
        String[] tokens = normalized.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if (!words.containsKey(tokens[i])) continue;
            boolean nearQuiz = i + 1 < tokens.length && java.util.Set.of("quiz", "quizzes", "question", "questions").contains(tokens[i + 1])
                    || i > 0 && java.util.Set.of("quiz", "quizzes", "question", "questions").contains(tokens[i - 1]);
            if (!requireQuizContext || nearQuiz) return words.get(tokens[i]);
        }
        return 0;
    }

    private String quizCountQuestion(boolean invalidCount) {
        return (invalidCount ? "Quiz generation supports 1 to 20 questions. " : "")
                + "How many questions would you like in the quiz? Choose 1 to 20, for example 5, 10, 15, or 20.";
    }

    /** Returns 0 when no count was supplied and -1 when a supplied count is out of range. */
    private int requestedFlashcardCount(String request) {
        java.util.regex.Matcher digits = java.util.regex.Pattern.compile(
                "(?i)\\b(\\d{1,2})\\s*flash\\s*cards?\\b|\\bflash\\s*cards?\\D{0,12}(\\d{1,2})\\b").matcher(request);
        if (digits.find()) {
            String number = digits.group(1) == null ? digits.group(2) : digits.group(1);
            int count = Integer.parseInt(number);
            return count >= 1 && count <= 20 ? count : -1;
        }
        return numberWordCount(request, true);
    }

    private int pendingFlashcardCount(String request) {
        java.util.regex.Matcher digits = java.util.regex.Pattern.compile("\\b(\\d{1,2})\\b").matcher(request);
        if (digits.find()) {
            int count = Integer.parseInt(digits.group(1));
            return count >= 1 && count <= 20 ? count : -1;
        }
        return numberWordCount(request, false);
    }

    private int numberWordCount(String request, boolean requireFlashcardContext) {
        String normalized = request.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]+", " ").trim();
        java.util.Map<String, Integer> numberWords = java.util.Map.ofEntries(
                java.util.Map.entry("one", 1), java.util.Map.entry("two", 2), java.util.Map.entry("three", 3),
                java.util.Map.entry("four", 4), java.util.Map.entry("five", 5), java.util.Map.entry("six", 6),
                java.util.Map.entry("seven", 7), java.util.Map.entry("eight", 8), java.util.Map.entry("nine", 9),
                java.util.Map.entry("ten", 10), java.util.Map.entry("eleven", 11), java.util.Map.entry("twelve", 12),
                java.util.Map.entry("thirteen", 13), java.util.Map.entry("fourteen", 14), java.util.Map.entry("fifteen", 15),
                java.util.Map.entry("sixteen", 16), java.util.Map.entry("seventeen", 17),
                java.util.Map.entry("eighteen", 18), java.util.Map.entry("nineteen", 19), java.util.Map.entry("twenty", 20));
        String[] words = normalized.split(" ");
        for (int index = 0; index < words.length; index++) {
            if (!numberWords.containsKey(words[index])) continue;
            boolean followsFlashcards = index + 1 < words.length && words[index + 1].equals("flashcards")
                    || index + 2 < words.length && words[index + 1].equals("flash") && words[index + 2].equals("cards");
            if (!requireFlashcardContext || followsFlashcards)
                return numberWords.get(words[index]);
        }
        return 0;
    }

    private String flashcardCountQuestion(boolean invalidCount) {
        return (invalidCount ? "Flashcard generation supports 1 to 20 cards. " : "")
                + "How many flashcards would you like? Choose a number from 1 to 20, such as 5, 10, 15, or 20.";
    }

    private boolean isCancellation(String request) {
        return java.util.Set.of("cancel", "cancel flashcards", "cancel quiz", "cancel the quiz", "never mind", "nevermind", "stop")
                .contains(normalizeChoice(request));
    }

    private boolean explicitlyRequestsAll(String request) {
        String normalized = normalizeChoice(request);
        return java.util.List.of(normalized.split(" ")).stream()
                .anyMatch(word -> java.util.Set.of("all", "every", "both", "entire").contains(word))
                || normalized.contains("course wide") || normalized.contains("across the course");
    }

    private String summaryScopeQuestion(List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready) {
        String options = java.util.stream.IntStream.range(0, ready.size())
                .mapToObj(index -> (index + 1) + ". " + ready.get(index).originalFilename())
                .collect(java.util.stream.Collectors.joining("\n"));
        return "I found " + ready.size() + " ready course materials. Which should I summarize?\n\n"
                + options + "\n\nReply with a number or material name, or say 'all materials' for a combined course summary.";
    }

    private SummaryChoice resolveSummaryChoice(String request,
            List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready) {
        String normalized = normalizeChoice(request);
        if (java.util.Set.of("cancel", "cancel summary", "never mind", "nevermind", "stop").contains(normalized))
            return new SummaryChoice(null, false, true);
        if (explicitlyRequestsAll(request)) return new SummaryChoice("Summarize all my course materials", false, false);

        ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse selected = ordinalSelection(normalized, ready);
        if (selected == null) selected = findNamedMaterial(request, ready);
        if (selected != null) return new SummaryChoice("Summarize document: " + selected.originalFilename(), false, false);

        if (normalized.startsWith("summar") || normalized.contains("material") || normalized.contains("document"))
            return new SummaryChoice(null, true, false);
        if (normalized.matches(".*\\b(what|how|why|when|where|who|can|could|explain|tell)\\b.*") || request.contains("?"))
            return null;
        return new SummaryChoice(null, true, false);
    }

    private ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse ordinalSelection(String normalized,
            List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready) {
        int index = -1;
        if (normalized.matches(".*\\b(second|2|two)\\b.*")) index = 1;
        else if (normalized.matches(".*\\b(third|3|three)\\b.*")) index = 2;
        else if (normalized.matches(".*\\b(fourth|4|four)\\b.*")) index = 3;
        else if (normalized.matches(".*\\b(fifth|5|five)\\b.*")) index = 4;
        else if (normalized.matches(".*\\b(first|1|one)\\b.*")) index = 0;
        return index >= 0 && index < ready.size() ? ready.get(index) : null;
    }

    private ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse findNamedMaterial(String request,
            List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> ready) {
        java.util.Set<String> requestedTerms = materialSelectionTerms(request);
        if (requestedTerms.isEmpty()) return null;
        List<ca.yorku.eecs3311.coursepilot.application.dto.DocumentResponse> matches = ready.stream().filter(document -> {
            String name = normalizeChoice(document.originalFilename());
            String stem = normalizeChoice(document.originalFilename().replaceFirst("(?i)\\.[a-z0-9]+$", ""));
            java.util.Set<String> fileTerms = java.util.Arrays.stream(stem.split(" "))
                    .filter(term -> !java.util.Set.of("the", "a", "an", "and", "course", "notes", "lecture").contains(term))
                    .collect(java.util.stream.Collectors.toSet());
            return (!name.isBlank() && normalizeChoice(request).contains(name))
                    || (!fileTerms.isEmpty() && fileTerms.containsAll(requestedTerms));
        }).toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }

    private java.util.Set<String> materialSelectionTerms(String request) {
        String normalized = normalizeChoice(request).replaceAll(
                "\\b(summarize|summary|summarise|please|the|a|an|and|my|me|course|materials?|document|file|notes?|lecture|about|for|of|from|this|that|create|build|make|generate|quiz|quizzes|question|questions|flashcards?|flash|cards?|with|using|use|number|count|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty)\\b", " ")
                .replaceAll("\\b\\d+\\b", " ");
        return java.util.Arrays.stream(normalized.trim().replaceAll("\\s+", " ").split(" "))
                .filter(term -> term.length() > 1)
                .collect(java.util.stream.Collectors.toSet());
    }

    private String normalizeChoice(String value) {
        return java.util.Objects.toString(value, "").toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private record SummaryChoice(String agentRequest, boolean clarify, boolean cancelled) { }

    private AgentChatEntry toChatEntry(Long courseId, String prompt, String rawResult) {
        String result = java.util.Objects.toString(rawResult, "");
        String actionMarker = ca.yorku.eecs3311.coursepilot.application.agent.AgentToolManager.ACTION_MARKER;
        String actionLabel = "";
        String actionUrl = "";
        int actionStart = result.indexOf(actionMarker);
        if (actionStart >= 0) {
            int valueStart = actionStart + actionMarker.length();
            int separator = result.indexOf('|', valueStart);
            int actionEnd = result.indexOf("]]", separator < 0 ? valueStart : separator);
            if (separator > valueStart && actionEnd > separator) {
                actionLabel = result.substring(valueStart, separator).trim();
                actionUrl = result.substring(separator + 1, actionEnd).trim();
                result = result.substring(0, actionStart) + result.substring(actionEnd + 2);
            }
        }
        String conceptsMarker = ca.yorku.eecs3311.coursepilot.application.agent.AgentToolManager.CONCEPTS_MARKER;
        String sourcesMarker = ca.yorku.eecs3311.coursepilot.application.agent.AgentToolManager.SOURCES_MARKER;
        int conceptStart = result.indexOf(conceptsMarker);
        int sourceStart = result.indexOf(sourcesMarker);
        int metadataStart = conceptStart < 0 ? sourceStart : sourceStart < 0 ? conceptStart : Math.min(conceptStart, sourceStart);
        String answer = metadataStart < 0 ? result.trim() : result.substring(0, metadataStart).trim();
        List<String> concepts = sectionLines(result, conceptsMarker, sourceStart);
        List<String> sources = sectionLines(result, sourcesMarker, -1);
        List<HistoryEvent> historyEvents = List.of();
        if (answer.startsWith("Here is your recent learning activity.")) {
            historyEvents = HistoryActivitySummarizer.summarize(learningHistory.list(courseId)).stream()
                    .limit(10).toList();
            if (!historyEvents.isEmpty()) {
                int activityCount = historyEvents.size();
                answer = "Here are your " + activityCount + " latest course activit"
                        + (activityCount == 1 ? "y" : "ies") + ". Flashcard reviews are grouped by material.";
            }
        }
        return new AgentChatEntry(prompt, answer, Instant.now(), concepts, sources, actionLabel, actionUrl, historyEvents);
    }

    private List<String> sectionLines(String text, String marker, int nextMarker) {
        int start = text.indexOf(marker);
        if (start < 0) return List.of();
        start += marker.length();
        int end = nextMarker > start ? nextMarker : text.length();
        return text.substring(start, end).lines().map(String::trim)
                .filter(line -> !line.isBlank()).limit(8).toList();
    }

    private void addPageData(Long courseId, HttpSession session, Model model) {
        var course = facade.getCourse(courseId);
        var materials = facade.listMaterials(courseId);
        var readyMaterials = materials.stream().filter(item -> "READY".equals(item.processingStatus())).toList();
        var quizzes = facade.listQuizzes(courseId);
        var flashcards = facade.listFlashcards(courseId);
        var progressSummary = progress.summarize(courseId);
        var courseTopics = progressSummary.topics().stream()
                .filter(topic -> !"Quiz performance".equalsIgnoreCase(topic.topic())).toList();
        var recommendations = reviewQueue.build(courseId, course.name());
        var activity = learningHistory.recent(courseId, 1).stream().findFirst().orElse(null);

        model.addAttribute("course", course);
        model.addAttribute("activeNav", "agent");
        model.addAttribute("progressSummary", progressSummary);
        model.addAttribute("weakTopicCount", courseTopics.stream()
                .filter(topic -> !"STRONG".equalsIgnoreCase(topic.status())).count());
        model.addAttribute("readyMaterialCount", readyMaterials.size());
        model.addAttribute("totalMaterialCount", materials.size());
        model.addAttribute("quizCount", quizzes.size());
        model.addAttribute("flashcardCount", flashcards.size());
        model.addAttribute("weakestTopic", courseTopics.stream()
                .min(java.util.Comparator.comparingDouble(topic -> topic.score())).orElse(null));
        model.addAttribute("quizPerformance", progressSummary.topics().stream()
                .filter(topic -> "Quiz performance".equalsIgnoreCase(topic.topic())).findFirst().orElse(null));
        model.addAttribute("nextRecommendation", recommendations.stream().findFirst().orElse(null));
        model.addAttribute("lastActivity", activity);
        model.addAttribute("lastActivityLabel", activity == null ? "No activity yet" : relativeTime(activity));
        model.addAttribute("assistantMessages", history(session, courseId));
        model.addAttribute("assistantQuizzes", quizzes.stream()
                .filter(quiz -> quiz.creationOrigin() == ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.ASSISTANT).toList());
        model.addAttribute("assistantFlashcards", flashcards.stream()
                .filter(card -> card.creationOrigin() == ca.yorku.eecs3311.coursepilot.domain.model.CreationOrigin.ASSISTANT).toList());
    }

    @SuppressWarnings("unchecked")
    private List<AgentChatEntry> history(HttpSession session, Long courseId) {
        Object stored = session.getAttribute(HISTORY_PREFIX + courseId);
        if (stored instanceof List<?> list) return (List<AgentChatEntry>) list;
        List<AgentChatEntry> messages = new ArrayList<>();
        session.setAttribute(HISTORY_PREFIX + courseId, messages);
        return messages;
    }

    private String relativeTime(HistoryEvent activity) {
        if (activity.occurredAt() == null) return "Time unavailable";
        long minutes = Math.max(0, Duration.between(activity.occurredAt(), Instant.now()).toMinutes());
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        return days == 1 ? "Yesterday" : days + " days ago";
    }
}
