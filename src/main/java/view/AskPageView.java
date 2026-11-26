package view;

import interface_adapter.ask_page.AskPageController;
import interface_adapter.ask_page.AskPageViewModel;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** Reusable selected-page panel. Application composition supplies the selected page. */
public final class AskPageView extends JPanel {
    private final JTextArea question = new JTextArea(3, 60);
    private final JTextArea result = new JTextArea(12, 60);
    private final JButton ask = new JButton("Ask about selected page");
    private final long courseId;
    private final String slug;

    public AskPageView(AskPageController controller, AskPageViewModel model, long courseId, String slug) {
        super(new BorderLayout(8, 8));
        this.courseId = courseId;
        this.slug = slug;
        JPanel input = new JPanel(new BorderLayout(4, 4));
        question.setLineWrap(true);
        question.setWrapStyleWord(true);
        question.getAccessibleContext().setAccessibleName("Question about selected Canvas page");
        input.add(new JScrollPane(question), BorderLayout.CENTER);
        input.add(ask, BorderLayout.SOUTH);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        add(input, BorderLayout.NORTH);
        add(new JScrollPane(result), BorderLayout.CENTER);
        ask.addActionListener(event -> {
            ask.setEnabled(false);
            question.setEnabled(false);
            result.setText("Reading page and generating answer...");
            controller.ask(this.courseId, this.slug, question.getText());
        });
        model.addListener(event -> SwingUtilities.invokeLater(() -> render(model.getState())));
        render(model.getState());
    }

    private void render(AskPageViewModel.State state) {
        String source = state.sourceUrl.isEmpty() ? "" : "\n\nSource: " + state.sourceTitle + "\n" + state.sourceUrl;
        result.setText(state.message + source);
        result.setCaretPosition(0);
        ask.setEnabled(true);
        question.setEnabled(true);
    }
}
