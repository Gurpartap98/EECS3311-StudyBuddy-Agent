package studybuddy.tools;

import java.util.Map;
import studybuddy.domain.Material;
import studybuddy.llm.LLMException;
import studybuddy.llm.PromptBuilder;

/** Summarizes a material via LLMClient (F03). */
public final class SummarizeTool implements Tool {

    @Override
    public String name() {
        return "summarize";
    }

    @Override
    public Result execute(ToolContext ctx) {
        String materialId = ctx.requireString("materialId");
        Material material = ctx.getStore().findMaterial(materialId).orElse(null);
        if (material == null) {
            return Result.fail("Material not found: " + materialId);
        }
        if (material.getExtractedText().isBlank()) {
            return Result.fail("Material has no extracted text");
        }

        String prompt = PromptBuilder.buildSummaryPrompt(material.getExtractedText());
        try {
            String summary = ctx.getLlm().generate(prompt);
            material.setSummary(summary);
            ctx.getStore().save(material);
            return Result.ok("Summary saved", Map.of("summary", summary));
        } catch (LLMException ex) {
            return Result.fail(ex.getMessage());
        }
    }
}
