package studybuddy.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import studybuddy.domain.Flashcard;
import studybuddy.domain.Material;
import studybuddy.llm.LLMException;

/** Generates flashcards from a material (F07). */
public final class FlashcardTool implements Tool {

    @Override
    public String name() {
        return "flashcards";
    }

    @Override
    public Result execute(ToolContext ctx) {
        String materialId = ctx.requireString("materialId");
        int n = ctx.optionalInt("count", 8);
        Material material = ctx.getStore().findMaterial(materialId).orElse(null);
        if (material == null) {
            return Result.fail("Material not found: " + materialId);
        }

        String prompt = buildFlashcardPrompt(material.getExtractedText(), n);
        try {
            String raw = ctx.getLlm().generate(prompt);
            List<Flashcard> cards = parseCards(materialId, raw);
            ctx.getStore().saveFlashcards(materialId, cards);
            return Result.ok(
                    "Generated " + cards.size() + " flashcards",
                    Map.of("flashcards", cards, "raw", raw));
        } catch (LLMException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    public String buildFlashcardPrompt(String text, int n) {
        return """
                Create %d flashcards from these notes.
                Format each card as: Q: ... | A: ...

                NOTES:
                %s
                """.formatted(n, truncate(text, 6000));
    }

    private List<Flashcard> parseCards(String materialId, String raw) {
        List<Flashcard> cards = new ArrayList<>();
        if (raw == null) {
            return cards;
        }
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.toUpperCase().contains("Q:") || !trimmed.toUpperCase().contains("A:")) {
                continue;
            }
            int qIdx = trimmed.toUpperCase().indexOf("Q:");
            int aIdx = trimmed.toUpperCase().indexOf("A:");
            if (aIdx <= qIdx) {
                continue;
            }
            String front = trimmed.substring(qIdx + 2, aIdx).replace("|", "").trim();
            String back = trimmed.substring(aIdx + 2).trim();
            if (!front.isBlank() && !back.isBlank()) {
                cards.add(new Flashcard(materialId, front, back));
            }
        }
        return cards;
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "\n...[truncated]";
    }
}
