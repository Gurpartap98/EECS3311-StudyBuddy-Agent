package studybuddy.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import studybuddy.domain.Flashcard;

/** Simple GUI flashcard flipper for F07 review. */
public final class FlashcardReview {

    private final JFrame owner;

    public FlashcardReview(JFrame owner) {
        this.owner = owner;
    }

    public void open(List<Flashcard> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        JDialog dialog = new JDialog(owner, "Flashcard review", false);
        dialog.setSize(480, 280);
        dialog.setLocationRelativeTo(owner);

        final int[] index = {0};
        final boolean[] showBack = {false};

        JLabel counter = new JLabel();
        JTextArea body = new JTextArea();
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        Runnable render = () -> {
            Flashcard card = cards.get(index[0]);
            counter.setText("Card " + (index[0] + 1) + " / " + cards.size()
                    + (showBack[0] ? " (answer)" : " (question)"));
            body.setText(showBack[0] ? card.getBack() : card.getFront());
        };

        JButton flip = new JButton("Flip");
        flip.addActionListener(e -> {
            showBack[0] = !showBack[0];
            render.run();
        });
        JButton next = new JButton("Next");
        next.addActionListener(e -> {
            index[0] = (index[0] + 1) % cards.size();
            showBack[0] = false;
            render.run();
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());

        JPanel south = new JPanel();
        south.add(flip);
        south.add(next);
        south.add(close);

        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(counter, BorderLayout.NORTH);
        dialog.add(body, BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        render.run();
        dialog.setVisible(true);
    }
}
