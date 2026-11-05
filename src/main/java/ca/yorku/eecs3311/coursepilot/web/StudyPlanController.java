package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanResponse;
import ca.yorku.eecs3311.coursepilot.application.dto.ProgressSummary;
import ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanPageSummary;
import ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanCardView;
import ca.yorku.eecs3311.coursepilot.application.service.ProgressService;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanService;
import ca.yorku.eecs3311.coursepilot.application.service.StudyPlanReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/courses/{courseId}/study-plan")
public class StudyPlanController {
    private final StudyAgentFacade facade;
    private final StudyPlanService plans;
    private final ProgressService progress;
    private final StudyPlanReviewService reviewQueue;
    public StudyPlanController(StudyAgentFacade facade, StudyPlanService plans, ProgressService progress,
                               StudyPlanReviewService reviewQueue) {
        this.facade = facade;
        this.plans = plans;
        this.progress = progress;
        this.reviewQueue = reviewQueue;
    }
    @GetMapping
    public String page(@PathVariable Long courseId, Model model) {
        var course = facade.getCourse(courseId);
        var savedPlans = plans.list(courseId);
        var progressSummary = progress.summarize(courseId);
        var reviewItems = reviewQueue.build(courseId, course.name());
        var readyMaterialCount = facade.listMaterials(courseId).stream()
                .filter(material -> "READY".equals(material.processingStatus())).count();
        Map<Long, ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanReviewItem> sessionRecommendations = new HashMap<>();
        savedPlans.forEach(plan -> plan.items().forEach(item -> {
            var match = matchingReviewItem(item.activity(), reviewItems);
            if (match != null && item.id() != null) sessionRecommendations.put(item.id(), match);
        }));
        model.addAttribute("course", course);
        model.addAttribute("plans", savedPlans);
        model.addAttribute("planCards", buildPlanCards(courseId, savedPlans, sessionRecommendations));
        model.addAttribute("planCount", savedPlans.size());
        model.addAttribute("latestPlan", savedPlans.isEmpty() ? null : savedPlans.getLast());
        model.addAttribute("scheduledMinutes", savedPlans.stream().flatMap(plan -> plan.items().stream()).mapToInt(StudyPlanResponse.Item::minutes).sum());
        model.addAttribute("pageSummary", summarize(savedPlans, progressSummary, course, reviewItems));
        model.addAttribute("reviewItems", reviewItems);
        model.addAttribute("readyMaterialCount", readyMaterialCount);
        model.addAttribute("sessionRecommendations", sessionRecommendations);
        model.addAttribute("activeNav", "study-plan");
        return "study-plan/index";
    }

    private List<StudyPlanCardView> buildPlanCards(
            Long courseId,
            List<StudyPlanResponse> savedPlans,
            Map<Long, ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanReviewItem> recommendations) {
        return savedPlans.stream().map(plan -> {
            var sessions = plan.items().stream().map(item -> {
                var recommendation = recommendations.get(item.id());
                String actionUrl = recommendation == null
                        ? "/courses/" + courseId + "/studio"
                        : recommendation.actionUrl();
                String actionLabel = recommendation == null ? "Open Study Studio" : recommendation.actionLabel();
                String kind = item.completed() ? "completed"
                        : recommendation != null && recommendation.priority() <= 10 ? "priority"
                        : recommendation != null && "QUIZ".equals(recommendation.type()) ? "practice" : "review";
                String title = recommendation == null ? item.activity() : recommendation.title();
                String topic = recommendation != null && recommendation.documentName() != null
                        ? recommendation.documentName()
                        : item.activity();
                String status = item.completed() ? "Completed"
                        : recommendation == null ? "Scheduled"
                        : recommendation.status().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
                String priority = item.completed() ? "Done"
                        : recommendation != null && recommendation.priority() <= 10 ? "Focus first"
                        : recommendation != null && recommendation.priority() <= 20 ? "Recommended" : "On schedule";
                String reason = item.completed() ? "Session marked complete"
                        : recommendation == null ? "Planned for this course"
                        : recommendation.reason();
                return new StudyPlanCardView.Session(item,
                        item.date().format(DateTimeFormatter.ofPattern("MMM d")), title, topic, kind,
                        status, priority, reason, actionUrl, actionLabel);
            }).toList();
            int completedSessions = (int) plan.items().stream().filter(StudyPlanResponse.Item::completed).count();
            int completedMinutes = plan.items().stream().filter(StudyPlanResponse.Item::completed)
                    .mapToInt(item -> item.actualMinutes() > 0 ? item.actualMinutes() : item.minutes()).sum();
            int remainingMinutes = plan.items().stream().filter(item -> !item.completed())
                    .mapToInt(StudyPlanResponse.Item::minutes).sum();
            int progressPercent = plan.items().isEmpty() ? 0 : Math.round(completedSessions * 100f / plan.items().size());
            var next = plan.items().stream().filter(item -> !item.completed())
                    .min(Comparator.comparing(StudyPlanResponse.Item::date)).orElse(null);
            String actionUrl = next == null ? "/courses/" + courseId + "/progress"
                    : recommendations.containsKey(next.id()) ? recommendations.get(next.id()).actionUrl()
                    : "/courses/" + courseId + "/studio";
            String actionLabel = next == null ? "View course progress"
                    : recommendations.containsKey(next.id()) ? recommendations.get(next.id()).actionLabel()
                    : "Continue studying";
            return new StudyPlanCardView(plan, completedSessions, completedMinutes, remainingMinutes,
                    progressPercent, actionUrl, actionLabel, sessions);
        }).toList();
    }
    @PostMapping
    public String create(@PathVariable Long courseId, @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String targetDate, @RequestParam(required = false) String minutes,
                         @RequestParam(defaultValue = "BALANCED") String strategy, Model model) {
        try {
            plans.create(courseId, parseDate(startDate, "Start date is required."),
                    parseDate(targetDate, "Target date is required."), parseMinutes(minutes), strategy);
            model.addAttribute("planMessage", "Study plan created.");
        } catch (RuntimeException exception) {
            model.addAttribute("planError", exception.getMessage() == null ? "Please check the study plan fields." : exception.getMessage());
            model.addAttribute("startDate", startDate);
            model.addAttribute("targetDate", targetDate);
            model.addAttribute("minutes", minutes);
            model.addAttribute("strategy", strategy);
        }
        return page(courseId, model);
    }
    @PostMapping("/{planId}/items/{itemId}")
    public String updateItem(@PathVariable Long courseId, @PathVariable Long planId, @PathVariable Long itemId,
                             @RequestParam(required = false) String date, @RequestParam(required = false) String activity,
                             @RequestParam(required = false) String minutes, @RequestParam(defaultValue = "false") boolean completed,
                             @RequestParam(required = false) String actualMinutes, Model model) {
        try {
            plans.updateItem(courseId, planId, itemId, parseDate(date, "Session date is required."), activity,
                    parseMinutes(minutes), completed, parseActualMinutes(actualMinutes));
            model.addAttribute("planMessage", "Study session updated.");
        } catch (RuntimeException exception) {
            model.addAttribute("planError", exception.getMessage() == null ? "Please check the study session fields." : exception.getMessage());
        }
        return page(courseId, model);
    }
    private LocalDate parseDate(String value, String missingMessage) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(missingMessage);
        try { return LocalDate.parse(value); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("Dates must use the YYYY-MM-DD format."); }
    }
    private Integer parseMinutes(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Available study time is required.");
        try { int parsed = Integer.parseInt(value); if (parsed < 1) throw new IllegalArgumentException("Available study time must be positive."); return parsed; }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Available study time must be a whole number."); }
    }
    private Integer parseActualMinutes(String value) {
        if (value == null || value.isBlank()) return 0;
        try { int parsed = Integer.parseInt(value); if (parsed < 0) throw new IllegalArgumentException("Actual minutes cannot be negative."); return parsed; }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Actual minutes must be a whole number."); }
    }

    private StudyPlanPageSummary summarize(List<StudyPlanResponse> savedPlans, ProgressSummary progressSummary,
                                           ca.yorku.eecs3311.coursepilot.application.dto.CourseResponse course,
                                           List<ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanReviewItem> reviewItems) {
        var latest = savedPlans.isEmpty() ? null : savedPlans.getLast();
        var items = latest == null ? List.<StudyPlanResponse.Item>of() : latest.items();
        var next = items.stream()
                .filter(item -> !item.date().isBefore(LocalDate.now()))
                .min(Comparator.comparing(StudyPlanResponse.Item::date))
                .map(item -> new StudyPlanPageSummary.NextSession(item.date(), item.activity(), item.minutes()))
                .orElse(null);
        var weekly = weeklyWorkload(items);
        return new StudyPlanPageSummary(
                savedPlans.size(),
                items.stream().mapToInt(StudyPlanResponse.Item::minutes).sum(),
                items.size(),
                latest,
                next,
                progressSummary.hasActivity(),
                progressSummary.overallScore(),
                progressSummary.overallStatus(),
                progressSummary.topics().size(),
                weekly,
                course.name(),
                course.courseCode(),
                reviewItems);
    }

    private List<StudyPlanPageSummary.WeeklyWorkload> weeklyWorkload(List<StudyPlanResponse.Item> items) {
        Map<LocalDate, List<StudyPlanResponse.Item>> byWeek = new LinkedHashMap<>();
        for (var item : items) {
            LocalDate weekStart = item.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            byWeek.computeIfAbsent(weekStart, ignored -> new ArrayList<>()).add(item);
        }
        int maximum = byWeek.values().stream()
                .mapToInt(week -> week.stream().mapToInt(StudyPlanResponse.Item::minutes).sum())
                .max().orElse(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");
        return byWeek.entrySet().stream().map(entry -> {
            int minutes = entry.getValue().stream().mapToInt(StudyPlanResponse.Item::minutes).sum();
            int percent = Math.max(8, Math.round(minutes * 100f / maximum));
            return new StudyPlanPageSummary.WeeklyWorkload(
                    "Week of " + entry.getKey().format(formatter), minutes, entry.getValue().size(), percent);
        }).toList();
    }

    private ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanReviewItem matchingReviewItem(
            String activity, List<ca.yorku.eecs3311.coursepilot.application.dto.StudyPlanReviewItem> reviewItems) {
        String normalizedActivity = compact(activity);
        var documentMatch = reviewItems.stream()
                .filter(item -> item.documentName() != null && normalizedActivity.contains(compact(item.documentName())))
                .findFirst();
        if (documentMatch.isPresent()) return documentMatch.get();
        if (normalizedActivity.contains("quiz") || normalizedActivity.contains("performance")) {
            return reviewItems.stream().filter(item -> "QUIZ".equals(item.type())).findFirst().orElse(null);
        }
        return reviewItems.stream().findFirst().orElse(null);
    }

    private String compact(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\.(pdf|txt|md)$", "").replaceAll("[^a-z0-9]", "");
    }
}
