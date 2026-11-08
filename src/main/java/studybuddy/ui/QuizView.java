package studybuddy.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import studybuddy.domain.MCQQuestion;
import studybuddy.domain.Question;
import studybuddy.domain.Quiz;
import studybuddy.domain.ShortAnswerQuestion;

/**
 * Interactive Swing dialog to take a quiz and submit answers (F08/F09).
 */
public final class QuizView {

    private final JFrame owner;

    public QuizView(JFrame owner) {
        this.owner = owner;
    }

    /** Read-only preview of questions. */
    public void open(Quiz quiz) {
        open(quiz, null);
    }

    /**
     * @param onSubmit optional callback (quizId, answersByQuestionId)
     */
    public void open(Quiz quiz, BiConsumer<String, Map<String, String>> onSubmit) {
        JDialog dialog = new JDialog(owner, quiz.getTitle(), false);
        dialog.setSize(620, 520);
        dialog.setLocationRelativeTo(owner);

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        List<AnswerCapture> captures = new ArrayList<>();

        int i = 1;
        for (Question q : quiz.getQuestions()) {
            JPanel block = new JPanel(new BorderLayout(4, 4));
            block.setBorder(BorderFactory.createTitledBorder(i++ + ". [" + q.getType() + "]"));
            JTextArea prompt = new JTextArea(q.getPrompt());
            prompt.setLineWrap(true);
            prompt.setWrapStyleWord(true);
            prompt.setEditable(false);
            prompt.setOpaque(false);
            block.add(prompt, BorderLayout.NORTH);

            if (q instanceof MCQQuestion mcq) {
                ButtonGroup group = new ButtonGroup();
                JPanel choices = new JPanel(new GridLayout(0, 1));
                List<JRadioButton> buttons = new ArrayList<>();
                char label = 'a';
                for (String choice : mcq.getChoices()) {
                    JRadioButton rb = new JRadioButton(label + ") " + choice);
                    rb.setActionCommand(String.valueOf(label));
                    group.add(rb);
                    choices.add(rb);
                    buttons.add(rb);
                    label++;
                }
                block.add(choices, BorderLayout.CENTER);
                captures.add(new AnswerCapture(q.getId(), () -> {
                    for (JRadioButton rb : buttons) {
                        if (rb.isSelected()) {
                            return rb.getActionCommand();
                        }
                    }
                    return "";
                }));
            } else if (q instanceof ShortAnswerQuestion) {
                JTextField field = new JTextField();
                block.add(field, BorderLayout.CENTER);
                captures.add(new AnswerCapture(q.getId(), field::getText));
            } else {
                JTextField field = new JTextField();
                block.add(field, BorderLayout.CENTER);
                captures.add(new AnswerCapture(q.getId(), field::getText));
            }
            form.add(block);
        }

        JPanel south = new JPanel(new GridLayout(1, 2, 8, 8));
        JLabel meta = new JLabel(quiz.size() + " questions · id=" + quiz.getId());
        JButton submit = new JButton(onSubmit == null ? "Close" : "Submit & Grade");
        submit.addActionListener(e -> {
            if (onSubmit != null) {
                Map<String, String> answers = new LinkedHashMap<>();
                for (AnswerCapture capture : captures) {
                    String value = capture.read().get();
                    answers.put(capture.questionId(), value == null ? "" : value.trim());
                }
                onSubmit.accept(quiz.getId(), answers);
            }
            dialog.dispose();
        });
        south.add(meta);
        south.add(submit);

        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private record AnswerCapture(String questionId, java.util.function.Supplier<String> read) {
    }
}
