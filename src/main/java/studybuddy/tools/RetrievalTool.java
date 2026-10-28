package studybuddy.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import studybuddy.domain.Material;
import studybuddy.llm.LLMException;
import studybuddy.llm.PromptBuilder;

/**
 * Retrieves relevant material chunks then answers (F05).
 * {@link #retrieve(String, java.util.List)} is a helper called from {@link #execute(ToolContext)}.
 */
public final class RetrievalTool implements Tool {

    @Override
    public String name() {
        return "retrieve";
    }

    @Override
    public Result execute(ToolContext ctx) {
        String question = ctx.requireString("question");
        String courseId = ctx.optionalString("courseId", "");
        List<Material> scope = courseId.isBlank()
                ? ctx.getStore().listCourses().stream()
                        .flatMap(c -> ctx.getStore().listMaterials(c.getId()).stream())
                        .toList()
                : ctx.getStore().listMaterials(courseId);

        if (scope.isEmpty()) {
            return Result.fail("No materials available. Upload notes first (F02).");
        }

        List<Material> hits = retrieve(question, scope);
        if (hits.isEmpty()) {
            return Result.fail("No relevant material found for that question.");
        }

        StringBuilder context = new StringBuilder();
        for (Material m : hits) {
            context.append("### ").append(m.getTitle()).append('\n')
                    .append(truncate(m.getExtractedText(), 2000)).append("\n\n");
        }
        String prompt = PromptBuilder.buildQA(question, context.toString());

        try {
            String answer = ctx.getLlm().generate(prompt);
            return Result.ok(
                    answer,
                    Map.of(
                            "answer", answer,
                            "sources", hits.stream().map(Material::getTitle).toList()));
        } catch (LLMException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    /** Helper used by {@link #execute(ToolContext)} — simple keyword overlap retrieval. */
    public List<Material> retrieve(String question, List<Material> materials) {
        String q = question.toLowerCase(Locale.ROOT);
        String[] tokens = q.split("\\W+");
        List<Material> ranked = new ArrayList<>();
        for (Material material : materials) {
            String text = material.getExtractedText().toLowerCase(Locale.ROOT);
            int score = 0;
            for (String token : tokens) {
                if (token.length() < 3) {
                    continue;
                }
                if (text.contains(token) || material.getTitle().toLowerCase(Locale.ROOT).contains(token)) {
                    score++;
                }
            }
            if (score > 0) {
                ranked.add(material);
            }
        }
        if (ranked.isEmpty() && !materials.isEmpty()) {
            ranked.add(materials.get(0));
        }
        return ranked.size() > 3 ? ranked.subList(0, 3) : ranked;
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "\n...[truncated]";
    }
}
