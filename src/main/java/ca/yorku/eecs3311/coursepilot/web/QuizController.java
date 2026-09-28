package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.service.InsightGenerationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller @RequestMapping("/courses/{courseId}/quizzes")
public class QuizController {
    private final StudyAgentFacade facade;
    public QuizController(StudyAgentFacade facade) { this.facade = facade; }
    @GetMapping public String index(@PathVariable Long courseId, Model model) { populate(courseId, model); return "quizzes/index"; }
    @PostMapping("/generate") public String generate(@PathVariable Long courseId, @RequestParam Long documentId, @RequestParam Integer count, Model model) {
        populate(courseId, model); try { model.addAttribute("generation", facade.generateQuiz(courseId, documentId, count, Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER"))); } catch (RuntimeException e) { model.addAttribute("quizError", e.getMessage()); } return "quizzes/index";
    }
    @GetMapping("/{quizId}") public String take(@PathVariable Long courseId, @PathVariable Long quizId, Model model) { model.addAttribute("course", facade.getCourse(courseId)); model.addAttribute("quiz", facade.getQuiz(courseId, quizId)); return "quizzes/take"; }
    @PostMapping("/{quizId}/submit") public String submit(@PathVariable Long courseId, @PathVariable Long quizId, @RequestParam Map<String,String> params, Model model) {
        Map<Long,String> answers = new HashMap<>(); params.forEach((key,value) -> { if (key.startsWith("q-")) answers.put(Long.valueOf(key.substring(2)), value); });
        model.addAttribute("course", facade.getCourse(courseId)); model.addAttribute("quiz", facade.getQuiz(courseId, quizId)); model.addAttribute("attempt", facade.submitQuiz(courseId, quizId, answers)); return "quizzes/take";
    }
    @PostMapping("/{quizId}/questions/{questionId}/explain") public String explain(@PathVariable Long courseId,@PathVariable Long quizId,@PathVariable Long questionId,@RequestParam String answer,Model model){model.addAttribute("course",facade.getCourse(courseId));model.addAttribute("quiz",facade.getQuiz(courseId,quizId));model.addAttribute("explanation",facade.explainWrongAnswer(courseId,quizId,questionId,answer));return "quizzes/take";}
    private void populate(Long courseId, Model model) { model.addAttribute("course", facade.getCourse(courseId)); model.addAttribute("materials", facade.listMaterials(courseId)); model.addAttribute("quizzes", facade.listQuizzes(courseId)); model.addAttribute("activeNav", "quizzes"); }
}
