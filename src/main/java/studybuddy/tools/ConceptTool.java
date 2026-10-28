package studybuddy.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import studybuddy.domain.Concept;
import studybuddy.domain.Material;
import studybuddy.llm.LLMException;
import studybuddy.llm.PromptBuilder;

/** Extracts key concepts from a material (F04). */
public final class ConceptTool implements Tool {

    @Override
    public String name() {
        return "concepts";
    }

    @Override
    public Result execute(ToolContext ctx) {
        String materialId = ctx.requireString("materialId");
        Material material = ctx.getStore().findMaterial(materialId).orElse(null);
        if (material == null) {
            return Result.fail("Material not found: " + materialId);
        }

        String prompt = buildConceptPrompt(material.getExtractedText());
        try {
            String raw = ctx.getLlm().generate(prompt);
            List<Concept> concepts = parseConcepts(materialId, raw);
            ctx.getStore().saveConcepts(materialId, concepts);
            return Result.ok(
                    "Extracted " + concepts.size() + " concepts",
                    Map.of("concepts", concepts, "raw", raw));
        } catch (LLMException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    public String buildConceptPrompt(String text) {
        return PromptBuilder.buildConceptPrompt(text);
    }

    private List<Concept> parseConcepts(String materialId, String raw) {
        List<Concept> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return list;
        }
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("\\s*[—\\-:]\\s+", 2);
            String name = parts[0].replaceAll("^[0-9]+[.)]\\s*", "").trim();
            String gloss = parts.length > 1 ? parts[1].trim() : "";
            if (!name.isBlank()) {
                list.add(new Concept(materialId, name, gloss));
            }
        }
        return list;
    }
}
