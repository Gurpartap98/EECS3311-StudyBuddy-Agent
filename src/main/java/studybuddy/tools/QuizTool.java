package studybuddy.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import studybuddy.domain.Material;
import studybuddy.domain.Question;
import studybuddy.domain.QuestionSpec;
import studybuddy.domain.Quiz;
import studybuddy.domain.QuizSpec;
import studybuddy.llm.LLMException;
import studybuddy.quiz.MCQQuestionFactory;
import studybuddy.quiz.QuizFactory;
import studybuddy.quiz.ShortAnswerQuestionFactory;

/**
 * Generates a quiz using Factory Method creators (F08).
 * Does not {@code new} concrete Question products directly.
 */
public final class QuizTool implements Tool {

    private final QuizFactory mcqFactory = new MCQQuestionFactory();
    private final QuizFactory shortAnswerFactory = new ShortAnswerQuestionFactory();

    @Override
    public String name() {
        return "quiz";
    }

    @Override
    public Result execute(ToolContext ctx) {
        String courseId = ctx.requireString("courseId");
        String materialId = ctx.optionalString("materialId", "");
        int count = ctx.optionalInt("count", 5);
        String difficulty = ctx.optionalString("difficulty", "medium");
        String kindName = ctx.optionalString("kind", "MCQ");
        QuestionSpec.Kind kind = "SHORT_ANSWER".equalsIgnoreCase(kindName)
                ? QuestionSpec.Kind.SHORT_ANSWER
                : QuestionSpec.Kind.MCQ;

        String sourceText = "";
        if (!materialId.isBlank()) {
            Material material = ctx.getStore().findMaterial(materialId).orElse(null);
            if (material == null) {
                return Result.fail("Material not found: " + materialId);
            }
            sourceText = material.getExtractedText();
        } else {
            List<Material> materials = ctx.getStore().listMaterials(courseId);
            if (materials.isEmpty()) {
                return Result.fail("No materials for course: " + courseId);
            }
            sourceText = materials.get(0).getExtractedText();
        }

        QuizSpec spec = new QuizSpec(courseId, materialId, count, difficulty, kind, "");
        String prompt = """
                Generate %d %s study questions at %s difficulty from the notes.
                For MCQ use lines: Q: ... | CHOICES: a) .. ; b) .. ; c) .. ; d) .. | ANSWER: a
                For SHORT_ANSWER use lines: Q: ... | A: ...

                NOTES:
                %s
                """.formatted(spec.getQuestionCount(), kind, difficulty, truncate(sourceText, 6000));

        try {
            String raw = ctx.getLlm().generate(prompt);
            List<QuestionSpec> specs = parseSpecs(raw, kind, difficulty);
            QuizFactory factory = kind == QuestionSpec.Kind.MCQ ? mcqFactory : shortAnswerFactory;
            List<Question> questions = factory.createQuestions(specs);
            if (questions.isEmpty()) {
                return Result.fail("LLM returned no parseable questions. Raw: " + truncate(raw, 400));
            }
            Quiz quiz = new Quiz(courseId, "Generated quiz", questions);
            ctx.getStore().save(quiz);
            return Result.ok(
                    "Quiz " + quiz.getId() + " with " + quiz.size() + " questions",
                    Map.of("quiz", quiz, "raw", raw));
        } catch (LLMException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    private List<QuestionSpec> parseSpecs(String raw, QuestionSpec.Kind kind, String difficulty) {
        List<QuestionSpec> specs = new ArrayList<>();
        if (raw == null) {
            return specs;
        }
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.toUpperCase().contains("Q:")) {
                continue;
            }
            if (kind == QuestionSpec.Kind.SHORT_ANSWER) {
                int q = indexOfIgnoreCase(trimmed, "Q:");
                int a = indexOfIgnoreCase(trimmed, "A:");
                if (q < 0 || a <= q) {
                    continue;
                }
                String prompt = trimmed.substring(q + 2, a).replace("|", "").trim();
                String answer = trimmed.substring(a + 2).trim();
                if (!prompt.isBlank() && !answer.isBlank()) {
                    specs.add(QuestionSpec.shortAnswer(prompt, "", difficulty, answer));
                }
            } else {
                int q = indexOfIgnoreCase(trimmed, "Q:");
                int c = indexOfIgnoreCase(trimmed, "CHOICES:");
                int a = indexOfIgnoreCase(trimmed, "ANSWER:");
                if (q < 0 || c <= q || a <= c) {
                    continue;
                }
                String prompt = trimmed.substring(q + 2, c).replace("|", "").trim();
                String choicesPart = trimmed.substring(c + 8, a).replace("|", "").trim();
                String answer = trimmed.substring(a + 7).trim();
                List<String> choices = new ArrayList<>();
                for (String part : choicesPart.split(";")) {
                    String choice = part.replaceFirst("^[a-dA-D][).:]\\s*", "").trim();
                    if (!choice.isBlank()) {
                        choices.add(choice);
                    }
                }
                int correct = Math.max(0, "abcd".indexOf(Character.toLowerCase(answer.charAt(0))));
                if (!prompt.isBlank() && choices.size() >= 2) {
                    specs.add(QuestionSpec.mcq(prompt, "", difficulty, choices, Math.min(correct, choices.size() - 1)));
                }
            }
        }
        return specs;
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toUpperCase().indexOf(needle.toUpperCase());
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "\n...[truncated]";
    }
}
