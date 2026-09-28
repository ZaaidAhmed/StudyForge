package ca.yorku.eecs3311.coursepilot.infrastructure.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfDocumentExtractor implements DocumentTextExtractor {
    @Override public boolean supports(String extension) { return extension.equals("pdf"); }

    @Override
    public ExtractedText extract(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedPage> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(new ExtractedPage(page, stripper.getText(document)));
            }
            return new ExtractedText(pages);
        } catch (IOException | RuntimeException exception) {
            throw new DocumentImportException("The PDF could not be read.", exception);
        }
    }
}
