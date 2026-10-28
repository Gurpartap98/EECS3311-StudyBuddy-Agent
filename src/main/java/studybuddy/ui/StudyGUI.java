package studybuddy.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import studybuddy.agent.AgentController;
import studybuddy.domain.Course;
import studybuddy.domain.Material;
import studybuddy.domain.PlanSpec;
import studybuddy.domain.QuestionSpec;
import studybuddy.domain.Quiz;
import studybuddy.domain.QuizSpec;
import studybuddy.tools.Result;

/**
 * Swing GUI view (MVC). All actions call {@link AgentController} facade methods.
 */
public final class StudyGUI {

    private final AgentController agent;
    private final ProgressView progressView = new ProgressView();

    private JFrame frame;
    private final DefaultListModel<String> courseModel = new DefaultListModel<>();
    private final DefaultListModel<String> materialModel = new DefaultListModel<>();
    private JList<String> courseList;
    private JList<String> materialList;
    private JTextArea output;
    private JTextField input;

    public StudyGUI(AgentController agent) {
        this.agent = agent;
        this.agent.getProgressTracker().attach(progressView);
    }

    public void show() {
        SwingUtilities.invokeLater(this::buildAndShow);
    }

    private void buildAndShow() {
        frame = new JFrame("StudyBuddy Agent");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(960, 640));

        courseList = new JList<>(courseModel);
        courseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshMaterials();
            }
        });

        materialList = new JList<>(materialModel);
        materialList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        left.add(new JLabel("Courses"), BorderLayout.NORTH);
        left.add(new JScrollPane(courseList), BorderLayout.CENTER);

        JPanel courseButtons = new JPanel(new GridLayout(0, 1, 4, 4));
        courseButtons.add(button("New Course", this::onNewCourse));
        courseButtons.add(button("Upload Material", this::onUpload));
        courseButtons.add(button("Refresh", this::refreshCourses));
        left.add(courseButtons, BorderLayout.SOUTH);

        JPanel mid = new JPanel(new BorderLayout(6, 6));
        mid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        mid.add(new JLabel("Materials"), BorderLayout.NORTH);
        mid.add(new JScrollPane(materialList), BorderLayout.CENTER);

        JPanel materialButtons = new JPanel(new GridLayout(0, 2, 4, 4));
        materialButtons.add(button("Summarize", this::onSummarize));
        materialButtons.add(button("Concepts", this::onConcepts));
        materialButtons.add(button("Flashcards", this::onFlashcards));
        materialButtons.add(button("Quiz (AI)", this::onQuiz));
        materialButtons.add(button("Demo Quiz", this::onDemoQuiz));
        materialButtons.add(button("Study Plan", this::onPlan));
        materialButtons.add(button("Weak Topics", this::onWeak));
        mid.add(materialButtons, BorderLayout.SOUTH);

        output = new JTextArea();
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        input = new JTextField();
        input.addActionListener(e -> onAskOrNl());
        JButton send = button("Ask / NL", this::onAskOrNl);

        JPanel chatSouth = new JPanel(new BorderLayout(6, 6));
        chatSouth.add(input, BorderLayout.CENTER);
        chatSouth.add(send, BorderLayout.EAST);

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        right.add(new JLabel("Agent output · LLM: " + agent.getLlm().describe()), BorderLayout.NORTH);
        right.add(new JScrollPane(output), BorderLayout.CENTER);
        right.add(chatSouth, BorderLayout.SOUTH);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, mid);
        leftSplit.setResizeWeight(0.4);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, right);
        mainSplit.setResizeWeight(0.45);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Progress (Observer)"));
        bottom.setPreferredSize(new Dimension(100, 120));
        bottom.add(progressView.getComponent(), BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(mainSplit, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        refreshCourses();
        append("StudyBuddy GUI ready. Create a course, upload sample notes, then try Ask / NL.");
    }

    private JButton button(String label, Runnable action) {
        JButton b = new JButton(label);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void onNewCourse() {
        String name = JOptionPane.showInputDialog(frame, "Course name:");
        if (name == null || name.isBlank()) {
            return;
        }
        String code = JOptionPane.showInputDialog(frame, "Course code (optional):");
        String term = JOptionPane.showInputDialog(frame, "Term (optional):");
        showResult(agent.createCourse(name, code == null ? "" : code, term == null ? "" : term));
        refreshCourses();
    }

    private void onUpload() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        String path = JOptionPane.showInputDialog(
                frame,
                "Path to TXT/MD file:",
                "data/samples/os-scheduling.txt");
        if (path == null || path.isBlank()) {
            return;
        }
        showResult(agent.upload(course.getId(), path));
        refreshMaterials();
    }

    private void onSummarize() {
        Material material = selectedMaterial();
        if (material == null) {
            return;
        }
        showResult(agent.summarize(material.getId()));
    }

    private void onConcepts() {
        Material material = selectedMaterial();
        if (material == null) {
            return;
        }
        showResult(agent.extractConcepts(material.getId()));
    }

    private void onFlashcards() {
        Material material = selectedMaterial();
        if (material == null) {
            return;
        }
        showResult(agent.generateFlashcards(material.getId(), 8));
    }

    private void onQuiz() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        Material material = selectedMaterial();
        String materialId = material == null ? "" : material.getId();
        QuizSpec spec = new QuizSpec(course.getId(), materialId, 5, "medium", QuestionSpec.Kind.MCQ, "");
        Result result = agent.generateQuiz(spec);
        showResult(result);
        if (result.isSuccess() && result.get("quiz") instanceof Quiz quiz) {
            new QuizView(frame).open(quiz);
        }
    }

    private void onDemoQuiz() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        Result result = agent.createDemoQuiz(course.getId());
        showResult(result);
        if (result.isSuccess() && result.get("quiz") instanceof Quiz quiz) {
            new QuizView(frame).open(quiz);
            // Auto-grade a mixed attempt so ProgressView / weak topics update (F09–F11 demo)
            java.util.Map<String, String> answers = new java.util.LinkedHashMap<>();
            var questions = quiz.getQuestions();
            for (int i = 0; i < questions.size(); i++) {
                answers.put(questions.get(i).getId(), i == 0 ? "b" : "a");
            }
            showResult(agent.gradeQuiz(quiz.getId(), answers));
        }
    }

    private void onPlan() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        showResult(agent.generatePlan(new PlanSpec(course.getId(), 2.0, LocalDate.now().plusWeeks(2), "")));
    }

    private void onWeak() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        append("Weak topics: " + agent.getProgressTracker().weakTopics(course.getId()));
    }

    private void onAskOrNl() {
        String text = input.getText().trim();
        if (text.isBlank()) {
            return;
        }
        input.setText("");
        showResult(agent.handleRequest(text));
    }

    private Course selectedCourse() {
        int index = courseList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(frame, "Select a course first.");
            return null;
        }
        String id = courseModel.get(index).split(" ", 2)[0];
        return agent.getStore().findCourse(id).orElse(null);
    }

    private Material selectedMaterial() {
        int index = materialList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(frame, "Select a material first.");
            return null;
        }
        String id = materialModel.get(index).split(" ", 2)[0];
        return agent.getStore().findMaterial(id).orElse(null);
    }

    private void refreshCourses() {
        courseModel.clear();
        for (Course c : agent.listCourses()) {
            courseModel.addElement(c.getId() + "  " + c.getName());
        }
        refreshMaterials();
    }

    private void refreshMaterials() {
        materialModel.clear();
        Course course = null;
        int index = courseList.getSelectedIndex();
        if (index >= 0) {
            String id = courseModel.get(index).split(" ", 2)[0];
            course = agent.getStore().findCourse(id).orElse(null);
        }
        if (course == null) {
            return;
        }
        for (Material m : agent.getStore().listMaterials(course.getId())) {
            materialModel.addElement(m.getId() + "  " + m.getTitle());
        }
    }

    private void showResult(Result result) {
        append((result.isSuccess() ? "OK: " : "FAIL: ") + result.getMessage());
    }

    private void append(String text) {
        output.append(text + "\n\n");
        output.setCaretPosition(output.getDocument().getLength());
    }
}
