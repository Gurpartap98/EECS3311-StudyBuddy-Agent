package studybuddy.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import studybuddy.domain.Material;

/**
 * Imports PDF/TXT/MD into a course (F02). PDF extraction is completed in a later commit.
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
                    Map.of("materialId", material.getId(), "chars", text.length()));
        } catch (IOException ex) {
            return Result.fail("Failed to import: " + ex.getMessage());
        }
    }

    /** Helper used by {@link #execute(ToolContext)}. */
    public String extractText(String path) throws IOException {
        Path file = Path.of(path);
        String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        if (lower.endsWith(".pdf")) {
            throw new IOException(
                    "PDF extraction will be wired with PDFBox in a later commit: " + path);
        }
        throw new IOException("Unsupported file type: " + path);
    }
}
