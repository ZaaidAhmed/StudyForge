package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import ca.yorku.eecs3311.coursepilot.config.AiModelCatalog;
import ca.yorku.eecs3311.coursepilot.config.AiProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AiStatusController {
    private final StudyAgentFacade facade;
    private final AiProperties properties;
    public AiStatusController(StudyAgentFacade facade, AiProperties properties) {
        this.facade = facade;
        this.properties = properties;
    }

    @GetMapping("/ai/status")
    public String status(Model model) {
        model.addAttribute("availability", facade.aiStatus());
        model.addAttribute("providers", AiModelCatalog.providers());
        model.addAttribute("modelOptions", AiModelCatalog.modelsFor(properties.getProvider()));
        model.addAttribute("geminiModels", AiModelCatalog.modelsFor("gemini"));
        model.addAttribute("ollamaModels", AiModelCatalog.modelsFor("ollama"));
        model.addAttribute("activeNav", "ai");
        return "ai/status";
    }

    @PostMapping("/ai/status/model")
    public String changeModel(@RequestParam(defaultValue = "") String model,
                              RedirectAttributes redirectAttributes) {
        if (!AiModelCatalog.supports(properties.getProvider(), model)) {
            redirectAttributes.addFlashAttribute("modelError",
                    "Choose a supported model for the configured " + properties.getProvider() + " provider.");
            return "redirect:/ai/status";
        }
        properties.setModel(model);
        redirectAttributes.addFlashAttribute("modelMessage",
                "Model changed to " + model + " for this running session.");
        return "redirect:/ai/status";
    }

    @PostMapping("/ai/status/config")
    public String changeProviderAndModel(@RequestParam(defaultValue = "") String provider,
                                         @RequestParam(defaultValue = "") String model,
                                         RedirectAttributes redirectAttributes) {
        if (!AiModelCatalog.providers().contains(provider.toLowerCase(java.util.Locale.ROOT))) {
            redirectAttributes.addFlashAttribute("modelError", "Choose Gemini or Ollama as the AI provider.");
            return "redirect:/ai/status";
        }
        if (!AiModelCatalog.supports(provider, model)) {
            redirectAttributes.addFlashAttribute("modelError", "Choose a supported model for the selected provider.");
            return "redirect:/ai/status";
        }
        properties.setProvider(provider.toLowerCase(java.util.Locale.ROOT));
        properties.setModel(model);
        redirectAttributes.addFlashAttribute("modelMessage", "Switched to " + provider + " · " + model + ".");
        return "redirect:/ai/status";
    }
}
