package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizTakingState;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizReviewRow;
import ca.yorku.eecs3311.coursepilot.application.dto.QuizAttemptResult;
import ca.yorku.eecs3311.coursepilot.application.dto.GroundedExplanation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller @RequestMapping("/courses/{courseId}/quizzes")
public class QuizController {
    private final StudyAgentFacade facade;
    public QuizController(StudyAgentFacade facade) { this.facade = facade; }
    @GetMapping public String index(@PathVariable Long courseId, Model model) { populate(courseId, model); return "quizzes/index"; }
    @GetMapping("/{quizId}/attempts") public String attempts(@PathVariable Long courseId, @PathVariable Long quizId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("quiz", facade.getQuiz(courseId, quizId));
        model.addAttribute("attemptHistory", facade.listQuizAttempts(courseId, quizId));
        model.addAttribute("activeNav", "quizzes");
        return "quizzes/attempts";
    }
    @PostMapping("/generate") public String generate(@PathVariable Long courseId, @RequestParam(required = false) Long documentId, @RequestParam(required = false) String count, @RequestParam(required = false, name = "questionTypes") Set<String> questionTypes, Model model) {
        populate(courseId, model);
        model.addAttribute("selectedDocumentId", documentId);
        model.addAttribute("requestedCount", count);
        try {
            if (documentId == null) throw new IllegalArgumentException("Please select a ready document.");
            if (count == null || count.isBlank()) throw new IllegalArgumentException("Please select a quiz count.");
            int parsedCount;
            try { parsedCount = Integer.parseInt(count); }
            catch (NumberFormatException exception) { throw new IllegalArgumentException("Quiz count must be a number."); }
            if (parsedCount < 1 || parsedCount > 20) throw new IllegalArgumentException("Quiz count must be between 1 and 20.");
            Set<String> selectedTypes = questionTypes == null || questionTypes.isEmpty()
                    ? new LinkedHashSet<>(Set.of("MULTIPLE_CHOICE", "TRUE_FALSE"))
                    : questionTypes.stream().map(value -> value.trim().toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (selectedTypes.stream().anyMatch(value -> !Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER").contains(value))) {
                throw new IllegalArgumentException("Choose only multiple-choice, true/false, or short-answer questions.");
            }
            var generation = facade.generateQuiz(courseId, documentId, parsedCount, selectedTypes);
            model.addAttribute("generation", generation);
            model.addAttribute("quizzes", facade.listQuizzes(courseId));
        } catch (RuntimeException e) { model.addAttribute("quizError", e.getMessage()); }
        return "quizzes/index";
    }
    @GetMapping("/{quizId}") public String take(@PathVariable Long courseId, @PathVariable Long quizId,
                                                   @RequestParam(defaultValue = "false") boolean retake,
                                                   @RequestParam(required = false) Long attemptId,
                                                   Model model, HttpSession session) {
        model.addAttribute("course", facade.getCourse(courseId));
        var quiz = facade.getQuiz(courseId, quizId);
        var state = state(session, courseId, quizId);
        if (retake) state.reset();
        if (!retake && attemptId != null) {
            state.submittedAttempt(facade.getQuizAttempt(courseId, quizId, attemptId));
            state.answers().clear();
        }
        model.addAttribute("activeNav", "quizzes");
        model.addAttribute("attemptHistory", facade.listQuizAttempts(courseId, quizId));
        populateQuizModel(model, quiz, state);
        return "quizzes/take";
    }

    @PostMapping("/{quizId}/answer")
    public String answer(@PathVariable Long courseId, @PathVariable Long quizId,
                         @RequestParam Long questionId, @RequestParam(required = false, defaultValue = "") String answer,
                         @RequestParam String action, Model model, HttpSession session) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("activeNav", "quizzes");
        var quiz = facade.getQuiz(courseId, quizId);
        var state = state(session, courseId, quizId);
        int index = indexOfQuestion(quiz, questionId);
        if (index < 0 || index != state.currentQuestionIndex()) {
            model.addAttribute("quizError", "That question is not part of the current quiz step.");
            populateQuizModel(model, quiz, state);
            return "quizzes/take";
        }
        state.answers().put(questionId, answer == null ? "" : answer.trim());
        if ("submit".equals(action)) {
            if (answer.isBlank()) {
                model.addAttribute("quizError", "Choose an answer before submitting, or choose Skip for now.");
                populateQuizModel(model, quiz, state);
                return "quizzes/take";
            }
            var attempt = facade.submitQuiz(courseId, quizId, new HashMap<>(state.answers()));
            state.submittedAttempt(attempt);
            state.answers().clear();
            model.addAttribute("attempt", attempt);
            model.addAttribute("savedAttemptUrl", "/courses/" + courseId + "/quizzes/" + quizId + "?attemptId=" + attempt.id());
            model.addAttribute("quiz", quiz);
            model.addAttribute("reviewRows", reviewRows(quiz, attempt, null));
            model.addAttribute("explainedQuestionId", null);
            return "quizzes/take";
        }
        if ("previous".equals(action)) {
            state.currentQuestionIndex(Math.max(0, index - 1));
        } else if ("next".equals(action) || "skip".equals(action)) {
            if ("next".equals(action) && answer.isBlank()) {
                model.addAttribute("quizError", "Choose an answer before continuing, or choose Skip for now.");
                populateQuizModel(model, quiz, state);
                return "quizzes/take";
            }
            state.currentQuestionIndex(Math.min(quiz.questions().size() - 1, index + 1));
        }
        return "redirect:/courses/" + courseId + "/quizzes/" + quizId + "?question=" + state.currentQuestionIndex();
    }
    @PostMapping("/{quizId}/delete") public String delete(@PathVariable Long courseId, @PathVariable Long quizId, RedirectAttributes redirectAttributes) { facade.deleteQuiz(courseId, quizId); redirectAttributes.addFlashAttribute("successMessage", "Quiz was deleted."); return "redirect:/courses/" + courseId + "/quizzes"; }
    @PostMapping("/{quizId}/submit") public String submit(@PathVariable Long courseId, @PathVariable Long quizId, @RequestParam Map<String,String> params, Model model, HttpSession session) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("activeNav", "quizzes");
        var quiz = facade.getQuiz(courseId, quizId);
        var state = state(session, courseId, quizId);
        Map<Long,String> parsedAnswers = new HashMap<>();
        try {
            params.forEach((key, value) -> {
                if (!key.startsWith("q-")) return;
                String questionId = key.substring(2);
                if (!questionId.matches("\\d+")) {
                    throw new IllegalArgumentException("Quiz answers contain an invalid question identifier.");
                }
                parsedAnswers.put(Long.valueOf(questionId), value == null ? "" : value);
            });
            state.answers().putAll(parsedAnswers);
            Map<Long, String> answers = new HashMap<>(state.answers());
            validateQuestionIds(quiz, answers.keySet());
            var attempt = facade.submitQuiz(courseId, quizId, answers);
            model.addAttribute("attempt", attempt);
            state.answers().clear();
            state.submittedAttempt(attempt);
            model.addAttribute("quiz", quiz);
            model.addAttribute("reviewRows", reviewRows(quiz, attempt, null));
            model.addAttribute("explainedQuestionId", null);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("quiz", quiz);
            model.addAttribute("quizError", exception.getMessage());
            populateQuizModel(model, quiz, state);
        }
        return "quizzes/take";
    }
    @PostMapping("/{quizId}/questions/{questionId}/explain") public String explain(@PathVariable Long courseId,@PathVariable Long quizId,@PathVariable Long questionId,@RequestParam(required = false) Long attemptId,Model model,HttpSession session){
        model.addAttribute("course",facade.getCourse(courseId));
        var quiz = facade.getQuiz(courseId,quizId);
        var state = state(session, courseId, quizId);
        if (attemptId != null) {
            state.submittedAttempt(facade.getQuizAttempt(courseId, quizId, attemptId));
            state.answers().clear();
        }
        GroundedExplanation explanation = null;
        if (indexOfQuestion(quiz, questionId) < 0) {
            model.addAttribute("quizError", "That question does not belong to this quiz.");
        } else if (state.submittedAttempt() == null) {
            model.addAttribute("quizError", "Submit the quiz before requesting an explanation.");
        }
        if (!model.containsAttribute("quizError")) {
            Long persistedAttemptId = attemptId != null ? attemptId : state.submittedAttempt().id();
            try { explanation = facade.explainWrongAnswer(courseId,quizId,persistedAttemptId,questionId); }
            catch (RuntimeException exception) { model.addAttribute("quizError", "The explanation service is unavailable. Try again or check AI status."); }
        } else {
            // Keep the submitted result visible when the request is invalid.
        }
        model.addAttribute("activeNav", "quizzes");
        model.addAttribute("quiz",quiz);
        model.addAttribute("attemptHistory", facade.listQuizAttempts(courseId, quizId));
        if (state.submittedAttempt() != null) {
            model.addAttribute("attempt", state.submittedAttempt());
            model.addAttribute("reviewRows", reviewRows(quiz, state.submittedAttempt(), explanation, questionId));
            model.addAttribute("explainedQuestionId", explanation == null ? null : questionId);
        }
        return "quizzes/take";
    }
    private void populate(Long courseId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("materials", facade.listMaterials(courseId));
        var quizzes = facade.listQuizzes(courseId);
        Map<Long, Integer> attemptCounts = new LinkedHashMap<>();
        quizzes.forEach(quiz -> attemptCounts.put(quiz.id(), facade.listQuizAttempts(courseId, quiz.id()).size()));
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("attemptCounts", attemptCounts);
        model.addAttribute("activeNav", "quizzes");
    }

    private void populateQuizModel(Model model, ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto quiz, QuizTakingState state) {
        model.addAttribute("quiz", quiz);
        model.addAttribute("submittedAnswers", state.answers());
        if (state.submittedAttempt() != null) {
            model.addAttribute("attempt", state.submittedAttempt());
            model.addAttribute("reviewRows", reviewRows(quiz, state.submittedAttempt(), null));
            model.addAttribute("explainedQuestionId", null);
        }
    }

    private List<QuizReviewRow> reviewRows(ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto quiz,
                                           QuizAttemptResult attempt, GroundedExplanation explanation) {
        return reviewRows(quiz, attempt, explanation, null);
    }

    private List<QuizReviewRow> reviewRows(ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto quiz,
                                           QuizAttemptResult attempt, GroundedExplanation explanation, Long explanationQuestionId) {
        Map<Long, QuizAttemptResult.AnswerResult> byQuestion = new HashMap<>();
        attempt.answers().forEach(answer -> byQuestion.put(answer.questionId(), answer));
        return java.util.stream.IntStream.range(0, quiz.questions().size()).mapToObj(index -> {
            var question = quiz.questions().get(index);
            var result = byQuestion.get(question.id());
            boolean unanswered = result == null || result.answer() == null || result.answer().isBlank();
            String correctAnswer = answerText(question);
            boolean isCorrect = result != null && result.correct();
            GroundedExplanation selectedExplanation = explanationQuestionId != null && Objects.equals(question.id(), explanationQuestionId) ? explanation : null;
            String storedExplanation = cleanFeedback(
                    question.explanation() == null || question.explanation().isBlank()
                            ? question.referenceAnswer()
                            : question.explanation());
            String explanationText = selectedExplanation != null
                    ? cleanFeedback(selectedExplanation.explanation())
                    : (!storedExplanation.isBlank() && !storedExplanation.equals("Review the correct answer and the concept being tested.")
                        ? storedExplanation
                        : (isCorrect ? "Your answer matches the expected answer." : cleanFeedback(result == null ? "" : result.feedback())));
            return new QuizReviewRow(index + 1, question, result,
                    question.correctAnswer(), question.referenceAnswer(), unanswered,
                    new ca.yorku.eecs3311.coursepilot.application.dto.QuizReviewFeedback(isCorrect, correctAnswer, explanationText));
        }).toList();
    }

    private String answerText(ca.yorku.eecs3311.coursepilot.application.dto.QuizQuestionResponse question) {
        return question.type().name().equals("SHORT_ANSWER") ? question.referenceAnswer() : question.correctAnswer();
    }

    private String cleanFeedback(String value) {
        String text = HtmlUtils.htmlUnescape(Objects.toString(value, ""))
                .replaceAll("(?is)<[^>]*>", " ")
                .replaceAll("(?i)\\[(?:source|citation)[^]]*]", " ")
                .replaceAll("\\s+", " ").trim();
        if (text.isBlank()) return "Review the correct answer and the concept being tested.";
        if (text.length() <= 200) return text;
        int cut = text.lastIndexOf(' ', 200);
        return text.substring(0, cut > 80 ? cut : 200).trim() + "…";
    }

    private QuizTakingState state(HttpSession session, Long courseId, Long quizId) {
        String key = "studyforge.quiz." + courseId + "." + quizId;
        Object existing = session.getAttribute(key);
        if (existing instanceof QuizTakingState value) return value;
        QuizTakingState created = new QuizTakingState();
        session.setAttribute(key, created);
        return created;
    }

    private int indexOfQuestion(ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto quiz, Long questionId) {
        for (int i = 0; i < quiz.questions().size(); i++) if (Objects.equals(quiz.questions().get(i).id(), questionId)) return i;
        return -1;
    }

    private void validateQuestionIds(ca.yorku.eecs3311.coursepilot.application.dto.QuizResponseDto quiz, Set<Long> questionIds) {
        Set<Long> allowed = quiz.questions().stream().map(question -> question.id()).collect(java.util.stream.Collectors.toSet());
        if (!allowed.containsAll(questionIds)) throw new IllegalArgumentException("Quiz answers contain a question that does not belong to this quiz.");
    }
}
