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

public final class CourseQuestionView extends JPanel {
    private final JTextArea question = new JTextArea(3, 45);
    private final JTextArea result = new JTextArea(12, 45);
    private final JButton send = new JButton("Send");
    private final JPanel references = new JPanel();
    private final Map<String, JCheckBox> selections = new LinkedHashMap<>();
    private List<CourseQuestionViewModel.PageOption> displayedPages = List.of();

    public CourseQuestionView(CourseQuestionController controller, CourseQuestionViewModel model,
            long courseId, String title) {
        super(new BorderLayout(12, 12));
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
        JScrollPane answerPane = new JScrollPane(result);
        answerPane.setBorder(BorderFactory.createTitledBorder("Answer and sources"));
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
            result.setText("Reading Page references and generating an answer...");
            controller.ask(courseId, submittedQuestion, selectedIds);
        });
        model.addListener(event -> SwingUtilities.invokeLater(() -> render(model.getState())));
        render(model.getState());
        controller.loadCatalog(courseId);
    }

    private void render(CourseQuestionViewModel.State state) {
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
        result.setText(state.result);
        result.setCaretPosition(0);
        setInputsEnabled(!state.pages.isEmpty());
    }

    private void setInputsEnabled(boolean enabled) {
        send.setEnabled(enabled);
        question.setEnabled(enabled);
        selections.values().forEach(box -> box.setEnabled(enabled));
    }
}
