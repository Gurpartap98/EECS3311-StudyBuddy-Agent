package studybuddy.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import studybuddy.agent.AgentController;
import studybuddy.domain.Course;
import studybuddy.domain.Material;
import studybuddy.domain.PlanSpec;
import studybuddy.domain.QuestionSpec;
import studybuddy.domain.Quiz;
import studybuddy.domain.QuizSpec;
import studybuddy.domain.StudyPlan;
import studybuddy.tools.Result;

/**
 * Swing GUI view (MVC). All actions call {@link AgentController} facade methods.
 * Lists show friendly names only; IDs are kept in parallel lists.
 */
public final class StudyGUI {

    private final AgentController agent;
    private final ProgressView progressView = new ProgressView();

    private JFrame frame;
    private final DefaultListModel<String> courseModel = new DefaultListModel<>();
    private final DefaultListModel<String> materialModel = new DefaultListModel<>();
    private final List<String> courseIds = new ArrayList<>();
    private final List<String> materialIds = new ArrayList<>();
    private JList<String> courseList;
    private JList<String> materialList;
    private JTextArea output;
    private JTextField input;
    /** When true, course selection changes do not clear/rebuild Materials. */
    private boolean suppressCourseListener;

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
            if (suppressCourseListener || e.getValueIsAdjusting()) {
                return;
            }
            Course selected = selectedCourseOrNull();
            if (selected != null) {
                agent.setActiveCourseId(selected.getId());
            }
            refreshMaterials();
        });

        materialList = new JList<>(materialModel);
        materialList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        left.add(new JLabel("Courses"), BorderLayout.NORTH);
        left.add(new JScrollPane(courseList), BorderLayout.CENTER);

        JPanel courseButtons = new JPanel(new GridLayout(0, 1, 4, 4));
        courseButtons.add(button("New Course", this::onNewCourse));
        courseButtons.add(button("Rename Course", this::onRenameCourse));
        courseButtons.add(button("Archive Course", this::onArchiveCourse));
        courseButtons.add(button("Delete Course", this::onDeleteCourse));
        courseButtons.add(button("Upload Material", this::onUpload));
        courseButtons.add(button("Refresh", this::refreshCourses));
        left.add(courseButtons, BorderLayout.SOUTH);

        JPanel mid = new JPanel(new BorderLayout(6, 6));
        mid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        mid.add(new JLabel("Materials"), BorderLayout.NORTH);
        mid.add(new JScrollPane(materialList), BorderLayout.CENTER);

        JPanel materialButtons = new JPanel(new GridLayout(0, 2, 4, 4));
        materialButtons.add(button("View Material", this::onViewMaterial));
        materialButtons.add(button("Summarize", this::onSummarize));
        materialButtons.add(button("Concepts", this::onConcepts));
        materialButtons.add(button("Flashcards", this::onFlashcards));
        materialButtons.add(button("Quiz (AI)", this::onQuiz));
        materialButtons.add(button("Demo Quiz", this::onDemoQuiz));
        materialButtons.add(button("Study Plan", this::onPlan));
        materialButtons.add(button("Weak Topics", this::onWeak));
        materialButtons.add(button("History", this::onHistory));
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
        append("StudyBuddy GUI ready. Create a course, Upload Material (.txt / .md / .pdf), then try Demo Quiz / Study Plan.");
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
        Result created = agent.createCourse(name, code == null ? "" : code, term == null ? "" : term);
        if (created.isSuccess() && created.get("course") instanceof Course course) {
            agent.setActiveCourseId(course.getId());
            showResult(Result.ok("Created course \"" + course.getName() + "\""));
        } else {
            showResult(created);
        }
        refreshCourses();
        selectCourseInList(agent.getActiveCourseId());
    }

    private void onRenameCourse() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        String name = JOptionPane.showInputDialog(frame, "New course name:", course.getName());
        if (name == null || name.isBlank()) {
            return;
        }
        Result renamed = agent.renameCourse(course.getId(), name);
        if (renamed.isSuccess() && renamed.get("course") instanceof Course c) {
            showResult(Result.ok("Renamed course to \"" + c.getName() + "\""));
        } else {
            showResult(renamed);
        }
        refreshCourses();
        selectCourseInList(course.getId());
    }

    private void onArchiveCourse() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Archive \"" + course.getName() + "\"?\n(It will disappear from this list.)",
                "Archive course",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        showResult(Result.ok("Archived course \"" + course.getName() + "\""));
        agent.archiveCourse(course.getId());
        refreshCourses();
    }

    private void onDeleteCourse() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Permanently DELETE \"" + course.getName() + "\" and its materials?",
                "Delete course",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        String name = course.getName();
        showResult(agent.deleteCourse(course.getId()));
        // Prefer friendly message
        append("OK: Deleted course \"" + name + "\"");
        refreshCourses();
    }

    private void onUpload() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        File startDir = resolveSampleDir();
        append("Opening file picker in: " + startDir.getAbsolutePath());
        JFileChooser chooser = new JFileChooser(startDir);
        chooser.setDialogTitle("Upload learning material");
        chooser.setFileFilter(new FileNameExtensionFilter("Notes (PDF, TXT, MD)", "pdf", "txt", "md", "markdown"));
        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            append("Upload cancelled (no file chosen).");
            return;
        }
        File chosen = chooser.getSelectedFile();
        if (chosen == null) {
            append("Upload cancelled: no file selected.");
            return;
        }
        importPath(course, chosen.getAbsolutePath());
    }

    private void importPath(Course course, String path) {
        String courseId = course.getId();
        agent.setActiveCourseId(courseId);
        append("Importing into course \"" + course.getName() + "\" from:\n" + path);
        Result uploaded;
        try {
            uploaded = agent.upload(courseId, path);
        } catch (RuntimeException ex) {
            append("FAIL: upload crashed: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }
        showResult(uploaded);
        if (!uploaded.isSuccess()) {
            JOptionPane.showMessageDialog(
                    frame,
                    uploaded.getMessage(),
                    "Upload failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Object rawId = uploaded.getData().get("materialId");
        Object rawTitle = uploaded.getData().get("title");
        String materialId = rawId == null ? "" : String.valueOf(rawId);
        String title = rawTitle == null ? new File(path).getName() : String.valueOf(rawTitle);

        if (!materialId.isBlank()) {
            agent.setActiveMaterialId(materialId);
        }

        // Force Materials list to show this file even if a selection listener races
        suppressCourseListener = true;
        try {
            selectCourseInList(courseId);
            showMaterialInList(materialId, title, courseId);
        } finally {
            suppressCourseListener = false;
        }

        append("Materials list now has " + materialModel.size() + " item(s): " + materialModel);
        Material imported = materialId.isBlank()
                ? null
                : agent.getStore().findMaterial(materialId).orElse(null);
        if (imported != null) {
            append("Preview:\n" + preview(imported.getExtractedText(), 280));
        } else {
            append("WARN: store.findMaterial returned empty for id=" + materialId
                    + " ; store has " + agent.getStore().listMaterials(courseId).size()
                    + " material(s) for this course.");
        }

        JOptionPane.showMessageDialog(
                frame,
                "Uploaded \"" + title + "\".\nIt should appear under Materials now.",
                "Upload OK",
                JOptionPane.INFORMATION_MESSAGE);

        suppressCourseListener = true;
        try {
            selectCourseInList(courseId);
            showMaterialInList(materialId, title, courseId);
        } finally {
            suppressCourseListener = false;
        }
    }

    /**
     * Put the imported note into the Materials list immediately (by id/title),
     * then sync from the store so all notes for the course appear.
     */
    private void showMaterialInList(String materialId, String title, String courseId) {
        refreshMaterialsForCourse(courseId);
        if (materialModel.isEmpty() && materialId != null && !materialId.isBlank()) {
            // Absolute fallback if store listing failed for any reason
            materialIds.clear();
            materialModel.clear();
            materialIds.add(materialId);
            materialModel.addElement(title == null || title.isBlank() ? "uploaded-notes.txt" : title);
            append("Fallback: forced Materials row for \"" + materialModel.get(0) + "\".");
        }
        selectMaterialInList(materialId);
        materialList.revalidate();
        materialList.repaint();
        if (frame != null) {
            frame.revalidate();
            frame.repaint();
        }
    }

    private File resolveSampleDir() {
        File[] candidates = new File[] {
                new File("data/samples"),
                new File(System.getProperty("user.dir", "."), "data/samples"),
                new File(System.getProperty("user.home", "."), "EECS3311-StudyBuddy-Agent/data/samples")
        };
        for (File dir : candidates) {
            if (dir.isDirectory()) {
                return dir.getAbsoluteFile();
            }
        }
        return new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
    }

    private void onViewMaterial() {
        Material material = selectedMaterial();
        if (material == null) {
            String courseId = agent.getActiveCourseId();
            var list = courseId == null || courseId.isBlank()
                    ? List.<Material>of()
                    : agent.getStore().listMaterials(courseId);
            if (list.isEmpty()) {
                return;
            }
            material = list.get(0);
            selectMaterialInList(material.getId());
        }
        JTextArea area = new JTextArea(material.getExtractedText());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(560, 420));
        JOptionPane.showMessageDialog(
                frame,
                scroll,
                "View: " + material.getTitle(),
                JOptionPane.PLAIN_MESSAGE);
    }

    private static String preview(String text, int max) {
        if (text == null || text.isBlank()) {
            return "(empty)";
        }
        String trimmed = text.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
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
        Result result = agent.generateFlashcards(material.getId(), 8);
        showResult(result);
        var cards = agent.getStore().listFlashcards(material.getId());
        if (!cards.isEmpty()) {
            new FlashcardReview(frame).open(cards);
        }
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
            openInteractiveQuiz(quiz);
        }
    }

    private void onDemoQuiz() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        Result result = agent.createDemoQuiz(course.getId());
        if (result.isSuccess() && result.get("quiz") instanceof Quiz quiz) {
            showResult(Result.ok("Opened demo quiz \"" + quiz.getTitle() + "\" (" + quiz.size() + " questions)"));
            openInteractiveQuiz(quiz);
        } else {
            showResult(result);
        }
    }

    private void openInteractiveQuiz(Quiz quiz) {
        new QuizView(frame).open(quiz, (quizId, answers) -> showResult(agent.gradeQuiz(quizId, answers)));
    }

    private void onPlan() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        Result result = agent.generatePlan(new PlanSpec(course.getId(), 2.0, LocalDate.now().plusWeeks(2), ""));
        if (result.isSuccess() && result.get("plan") instanceof StudyPlan plan) {
            StringBuilder sb = new StringBuilder();
            sb.append("Study plan created for \"").append(course.getName()).append("\"\n");
            sb.append("Sessions: ").append(plan.getSessions().size()).append("\n");
            for (StudyPlan.Session s : plan.getSessions()) {
                sb.append("- ").append(s.date()).append(": ")
                        .append(s.focusTopic()).append(" — ")
                        .append(s.task()).append(" (")
                        .append(s.minutes()).append(" min)\n");
            }
            showResult(Result.ok(sb.toString().trim()));
        } else {
            showResult(result);
        }
    }

    private void onWeak() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        append("Weak topics: " + agent.getProgressTracker().weakTopics(course.getId()));
    }

    private void onHistory() {
        Course course = selectedCourseOrNull();
        var events = course == null
                ? agent.getProgressTracker().history()
                : agent.getProgressTracker().historyForCourse(course.getId());
        if (events.isEmpty()) {
            append("(no progress history yet)");
            return;
        }
        StringBuilder sb = new StringBuilder("Progress history:\n");
        for (var e : events) {
            sb.append(e.getAt()).append(" | ").append(e.getTopic())
                    .append(" | ").append(String.format("%.0f", e.getScorePercent())).append("%\n");
        }
        append(sb.toString().trim());
    }

    private void onAskOrNl() {
        String text = input.getText().trim();
        if (text.isBlank()) {
            return;
        }
        input.setText("");
        // Simple questions → F05 retrieve/answer with active course context.
        // Multi-step phrasing ("then", "and make", …) → F12 planner.
        String lower = text.toLowerCase(Locale.ROOT);
        boolean multiStep = lower.contains(" then ")
                || lower.contains(" and then ")
                || lower.matches(".*\\b(summarize|concepts?|flashcards?|quiz|plan)\\b.*\\b(and|then)\\b.*")
                || lower.matches(".*\\b(and|then)\\b.*\\b(summarize|concepts?|flashcards?|quiz|plan)\\b.*");
        if (multiStep) {
            showResult(agent.handleRequest(text));
            return;
        }
        String courseId = agent.getActiveCourseId();
        if ((courseId == null || courseId.isBlank()) && selectedCourseOrNull() != null) {
            courseId = selectedCourseOrNull().getId();
        }
        showResult(agent.ask(text, courseId == null ? "" : courseId));
    }

    private Course selectedCourse() {
        Course course = selectedCourseOrNull();
        if (course == null) {
            JOptionPane.showMessageDialog(frame, "Select a course first.");
        }
        return course;
    }

    private Course selectedCourseOrNull() {
        int index = courseList.getSelectedIndex();
        if (index < 0 || index >= courseIds.size()) {
            return null;
        }
        return agent.getStore().findCourse(courseIds.get(index)).orElse(null);
    }

    private Material selectedMaterial() {
        Material material = selectedMaterialOrNull();
        if (material == null) {
            JOptionPane.showMessageDialog(frame, "Select a material first (middle list).");
        } else {
            agent.setActiveMaterialId(material.getId());
        }
        return material;
    }

    private Material selectedMaterialOrNull() {
        int index = materialList.getSelectedIndex();
        if (index < 0 || index >= materialIds.size()) {
            return null;
        }
        return agent.getStore().findMaterial(materialIds.get(index)).orElse(null);
    }

    private void refreshCourses() {
        String keepId = selectedCourseOrNull() == null ? agent.getActiveCourseId() : selectedCourseOrNull().getId();
        courseModel.clear();
        courseIds.clear();
        for (Course c : agent.listActiveCourses()) {
            courseIds.add(c.getId());
            courseModel.addElement(c.getName());
        }
        if (keepId != null && !keepId.isBlank()) {
            selectCourseInList(keepId);
            refreshMaterialsForCourse(keepId);
        } else {
            refreshMaterials();
        }
    }

    private void refreshMaterials() {
        Course course = selectedCourseOrNull();
        if (course == null && agent.getActiveCourseId() != null && !agent.getActiveCourseId().isBlank()) {
            refreshMaterialsForCourse(agent.getActiveCourseId());
            return;
        }
        if (course == null) {
            materialModel.clear();
            materialIds.clear();
            return;
        }
        refreshMaterialsForCourse(course.getId());
    }

    private void refreshMaterialsForCourse(String courseId) {
        materialModel.clear();
        materialIds.clear();
        if (courseId == null || courseId.isBlank()) {
            return;
        }
        List<Material> materials = agent.getStore().listMaterials(courseId);
        // Also include any material ids recorded on the Course object
        Course course = agent.getStore().findCourse(courseId).orElse(null);
        if (course != null) {
            for (String mid : course.getMaterialIds()) {
                agent.getStore().findMaterial(mid).ifPresent(m -> {
                    if (materials.stream().noneMatch(existing -> existing.getId().equals(m.getId()))) {
                        // listMaterials missed it — still show it
                        materialIds.add(m.getId());
                        materialModel.addElement(m.getTitle());
                    }
                });
            }
        }
        for (Material m : materials) {
            if (!materialIds.contains(m.getId())) {
                materialIds.add(m.getId());
                materialModel.addElement(m.getTitle());
            }
        }
    }

    private void selectCourseInList(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return;
        }
        int index = courseIds.indexOf(courseId);
        if (index >= 0) {
            courseList.setSelectedIndex(index);
            agent.setActiveCourseId(courseId);
        }
    }

    private void selectMaterialInList(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return;
        }
        int index = materialIds.indexOf(materialId);
        if (index >= 0) {
            materialList.setSelectedIndex(index);
            agent.setActiveMaterialId(materialId);
        }
    }

    private void showResult(Result result) {
        append((result.isSuccess() ? "OK: " : "FAIL: ") + result.getMessage());
        // Extra visibility if a tool put content only in data
        Object summary = result.getData().get("summary");
        Object answer = result.getData().get("answer");
        if (summary != null && !result.getMessage().contains(String.valueOf(summary))) {
            append(String.valueOf(summary));
        }
        if (answer != null && !result.getMessage().contains(String.valueOf(answer))) {
            append(String.valueOf(answer));
        }
    }

    private void append(String text) {
        output.append(text + "\n\n");
        output.setCaretPosition(output.getDocument().getLength());
    }
}
