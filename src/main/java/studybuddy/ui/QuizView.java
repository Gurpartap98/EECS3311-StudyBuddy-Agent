package studybuddy.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import studybuddy.domain.MCQQuestion;
import studybuddy.domain.Question;
import studybuddy.domain.Quiz;

/**
 * Simple Swing dialog to display a generated quiz (F08).
 */
public final class QuizView {

    private final JFrame owner;

    public QuizView(JFrame owner) {
        this.owner = owner;
    }

    public void open(Quiz quiz) {
        JDialog dialog = new JDialog(owner, quiz.getTitle(), false);
        dialog.setSize(560, 420);
        dialog.setLocationRelativeTo(owner);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        StringBuilder body = new StringBuilder();
        int i = 1;
        for (Question q : quiz.getQuestions()) {
            body.append(i++).append(". [").append(q.getType()).append("] ")
                    .append(q.getPrompt()).append('\n');
            if (q instanceof MCQQuestion mcq) {
                char label = 'a';
                for (String choice : mcq.getChoices()) {
                    body.append("   ").append(label++).append(") ").append(choice).append('\n');
                }
            }
            body.append('\n');
        }
        area.setText(body.toString());

        JPanel south = new JPanel(new GridLayout(1, 2));
        JLabel meta = new JLabel(quiz.size() + " questions · id=" + quiz.getId());
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        south.add(meta);
        south.add(close);

        dialog.setLayout(new BorderLayout(8, 8));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
