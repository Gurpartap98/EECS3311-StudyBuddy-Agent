package studybuddy.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import studybuddy.domain.Material;

/**
 * Imports PDF/TXT/MD into a course (F02).
 */
public final class MaterialImportTool implements Tool {

    @Override
    public String name() {
        return "import";
    }

    @Override
    public Result execute(ToolContext ctx) {
        String courseId = ctx.requireString("courseId");
        String path = ctx.requireString("path");
        String title = ctx.optionalString("title", Path.of(path).getFileName().toString());

        if (ctx.getStore().findCourse(courseId).isEmpty()) {
            return Result.fail("Course not found: " + courseId);
        }

        try {
            String text = extractText(path);
            if (text.isBlank()) {
                return Result.fail("Extracted text is empty for: " + path);
            }
            Material material = new Material(courseId, title, path, text);
            ctx.getStore().save(material);
            return Result.ok(
                    "Imported material " + material.getId(),
                    Map.of(
                            "materialId", material.getId(),
                            "chars", text.length(),
                            "source", path));
        } catch (IOException ex) {
            return Result.fail("Failed to import: " + ex.getMessage());
        }
    }

    /** Helper used by {@link #execute(ToolContext)}. */
    public String extractText(String path) throws IOException {
        Path file = Path.of(path);
        if (!Files.isRegularFile(file)) {
            throw new IOException("File not found: " + path);
        }
        String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        if (lower.endsWith(".pdf")) {
            return extractPdf(file);
        }
        throw new IOException("Unsupported file type (use .pdf, .txt, or .md): " + path);
    }

    private String extractPdf(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            return text == null ? "" : text.trim();
        }
    }
}
