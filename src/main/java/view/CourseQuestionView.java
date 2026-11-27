package view;

import interface_adapter.course_question.CourseQuestionController;
import interface_adapter.course_question.CourseQuestionViewModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;

public final class CourseQuestionView extends JPanel {
    private static final int ANSWER_REFRESH_MS = 50;

    private final JTextArea question = new JTextArea(3, 45);
    private final JTextArea result = new JTextArea(12, 45);
    private final JButton send = new JButton("Send");
    private final JPanel references = new JPanel();
    private final Map<String, JCheckBox> selections = new LinkedHashMap<>();
    private final CourseQuestionViewModel model;
    private final Timer answerUpdates;
    private List<CourseQuestionViewModel.PageOption> displayedPages = List.of();
    private boolean hasTurns;
    private int answerStart = -1;
    private boolean showingAnswer;

    public CourseQuestionView(CourseQuestionController controller, CourseQuestionViewModel model,
            long courseId, String title) {
        super(new BorderLayout(12, 12));
        this.model = model;
        answerUpdates = new Timer(ANSWER_REFRESH_MS, event -> flushAnswerDelta());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(20f));
        add(heading, BorderLayout.NORTH);

        references.setLayout(new BoxLayout(references, BoxLayout.Y_AXIS));
        JScrollPane referencePane = new JScrollPane(references);
        referencePane.setBorder(BorderFactory.createTitledBorder("Page references"));
        referencePane.setPreferredSize(new Dimension(285, 0));

        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        if (result.getCaret() instanceof DefaultCaret) {
            ((DefaultCaret) result.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }
        JScrollPane answerPane = new JScrollPane(result);
        answerPane.setBorder(BorderFactory.createTitledBorder("Conversation"));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, referencePane, answerPane);
        split.setResizeWeight(0.25);
        add(split, BorderLayout.CENTER);

        JPanel composer = new JPanel(new BorderLayout(8, 0));
        question.setLineWrap(true);
        question.setWrapStyleWord(true);
        question.getAccessibleContext().setAccessibleName("Course question");
        JScrollPane questionPane = new JScrollPane(question);
        questionPane.setBorder(BorderFactory.createTitledBorder("Question"));
        composer.add(questionPane, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);
        add(composer, BorderLayout.SOUTH);

        send.addActionListener(event -> {
            List<String> selectedIds = new ArrayList<>();
            selections.forEach((id, box) -> {
                if (box.isSelected()) {
                    selectedIds.add(id);
                }
            });
            String submittedQuestion = question.getText();
            question.setText("");
            setInputsEnabled(false);
            startTurn(submittedQuestion);
            answerUpdates.start();
            controller.ask(courseId, submittedQuestion, selectedIds);
        });
        model.addListener(event -> SwingUtilities.invokeLater(() -> render(model.getState())));
        render(model.getState());
        controller.loadCatalog(courseId);
    }

    @Override
    public void removeNotify() {
        answerUpdates.stop();
        super.removeNotify();
    }

    private void startTurn(String submittedQuestion) {
        if (hasTurns) {
            result.append("\n\n");
        }
        else {
            result.setText("");
        }
        if (!submittedQuestion.isBlank()) {
            result.append("You\n" + submittedQuestion.strip() + "\n\nAssistant\n");
        }
        answerStart = result.getDocument().getLength();
        result.append("Reading Page references and generating an answer...");
        result.setCaretPosition(answerStart);
        hasTurns = true;
    }

    private void flushAnswerDelta() {
        String delta = model.drainAnswerDelta();
        if (delta.isEmpty()) {
            return;
        }
        if (!showingAnswer) {
            result.replaceRange("", answerStart, result.getDocument().getLength());
            showingAnswer = true;
        }
        result.append(delta);
    }

    private void render(CourseQuestionViewModel.State state) {
        answerUpdates.stop();
        model.drainAnswerDelta();
        if (!displayedPages.equals(state.pages)) {
            references.removeAll();
            selections.clear();
            for (CourseQuestionViewModel.PageOption page : state.pages) {
                JCheckBox box = new JCheckBox();
                box.putClientProperty("html.disable", Boolean.TRUE);
                box.setText(page.title);
                references.add(box);
                selections.put(page.id, box);
            }
            displayedPages = state.pages;
            references.revalidate();
            references.repaint();
        }
        if (answerStart >= 0) {
            result.replaceRange(state.result, answerStart, result.getDocument().getLength());
            answerStart = -1;
            showingAnswer = false;
        }
        else if (!hasTurns) {
            result.setText(state.result);
            result.setCaretPosition(0);
        }
        setInputsEnabled(!state.pages.isEmpty());
    }

    private void setInputsEnabled(boolean enabled) {
        send.setEnabled(enabled);
        question.setEnabled(enabled);
        selections.values().forEach(box -> box.setEnabled(enabled));
    }
}
