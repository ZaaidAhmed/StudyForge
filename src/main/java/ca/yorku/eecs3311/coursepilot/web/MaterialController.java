package ca.yorku.eecs3311.coursepilot.web;

import ca.yorku.eecs3311.coursepilot.application.StudyAgentFacade;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

@Controller
@RequestMapping("/courses/{courseId}/materials")
public class MaterialController {
    private final StudyAgentFacade facade;
    public MaterialController(StudyAgentFacade facade) { this.facade = facade; }

    @GetMapping
    public String list(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("materials", facade.listMaterials(courseId));
        model.addAttribute("searchPerformed", false);
        model.addAttribute("activeNav", "materials");
        model.addAttribute("searchResults", java.util.List.of());
        model.addAttribute("searchQuery", "");
        return "materials/list";
    }

    @GetMapping("/{documentId}")
    public String detail(@PathVariable Long courseId, @PathVariable Long documentId, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        var material = facade.getMaterial(courseId, documentId);
        model.addAttribute("material", material);
        model.addAttribute("materials", facade.listMaterials(courseId).stream()
                .filter(candidate -> "READY".equals(candidate.processingStatus()) || candidate.id().equals(documentId))
                .toList());
        model.addAttribute("readerPage", 1);
        model.addAttribute("activeNav", "studio");
        return "materials/detail";
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> file(@PathVariable Long courseId, @PathVariable Long documentId) {
        var material = facade.getMaterial(courseId, documentId);
        Resource resource = facade.loadMaterialFile(courseId, documentId);
        MediaType mediaType;
        try { mediaType = material.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(material.contentType()); }
        catch (IllegalArgumentException exception) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        String downloadName = material.originalFilename() == null ? "material" : material.originalFilename().replaceAll("[\\r\\n\"]", "_");
        return ResponseEntity.ok().contentType(mediaType).header("Content-Disposition", "inline; filename=\"" + downloadName + "\"").body(resource);
    }

    /** Render one PDF page as an image so the application owns the reader UI. */
    @GetMapping("/{documentId}/page")
    public ResponseEntity<byte[]> page(@PathVariable Long courseId, @PathVariable Long documentId,
                                       @RequestParam(defaultValue = "1") int page) {
        var material = facade.getMaterial(courseId, documentId);
        if (!"pdf".equalsIgnoreCase(material.fileType())) return ResponseEntity.notFound().build();
        if (page < 1 || (material.pageCount() != null && page > material.pageCount())) {
            return ResponseEntity.badRequest().build();
        }
        try (var input = facade.loadMaterialFile(courseId, documentId).getInputStream();
             PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            if (page > document.getNumberOfPages()) return ResponseEntity.badRequest().build();
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage renderedPage = renderer.renderImageWithDPI(page - 1, 120, ImageType.RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(renderedPage, "png", output);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                    .body(output.toByteArray());
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search")
    public String search(@PathVariable Long courseId, @RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "5") Integer limit, Model model) {
        model.addAttribute("course", facade.getCourse(courseId));
        model.addAttribute("materials", facade.listMaterials(courseId));
        model.addAttribute("searchQuery", q);
        model.addAttribute("limit", limit);
        model.addAttribute("activeNav", "materials");
        model.addAttribute("searchPerformed", true);
        try {
            model.addAttribute("searchResults", facade.searchMaterials(courseId, q, limit));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("searchError", exception.getMessage());
            model.addAttribute("searchResults", java.util.List.of());
        }
        return "materials/list";
    }

    @PostMapping
    public String upload(@PathVariable Long courseId, @RequestParam("file") MultipartFile file,
                         RedirectAttributes redirectAttributes) {
        try {
            facade.importMaterial(courseId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
            redirectAttributes.addFlashAttribute("successMessage", "Material was uploaded and processed successfully.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage() == null
                    ? "The material could not be processed." : exception.getMessage());
        }
        return "redirect:/courses/" + courseId + "/materials";
    }
}
