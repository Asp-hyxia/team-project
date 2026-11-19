package view;

import interface_adapter.find_resources.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

/** Swing owns rendering and browser actions; all service calls run through the controller. */
public final class ResourcesView extends JPanel {
    private final FindResourcesController controller;
    private final JComboBox<ResourcesViewModel.CourseOption> courses = new JComboBox<>();
    private final JTextField query = new JTextField(24);
    private final JButton search = new JButton("Search resources");
    private final JButton reload = new JButton("Reload courses");
    private final JTextField pageSlug = new JTextField(24);
    private final JButton usePage = new JButton("Use page slug");
    private final JButton ask = new JButton("Ask about selected page");
    private final JButton open = new JButton("Open Canvas source");
    private final JTextArea status = new JTextArea(4, 60);
    private final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Title", "Type", "Course", "Source URL"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private List<ResourcesViewModel.Row> rows = List.of();

    public ResourcesView(FindResourcesController controller, ResourcesViewModel model, String mode) {
        this(controller, model, mode, null);
    }

    public ResourcesView(FindResourcesController controller, ResourcesViewModel model, String mode,
            Consumer<ResourcesViewModel.Row> onAskPage) {
        super(new BorderLayout(8, 8));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel heading = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Canvas Resource Finder — " + mode);
        title.setFont(title.getFont().deriveFont(20f));
        heading.add(title, BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        courses.setPreferredSize(new java.awt.Dimension(580, 28));
        controls.add(new JLabel("Course:")); controls.add(courses); controls.add(reload);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Title contains:")); filters.add(query); filters.add(search);
        JPanel lowerControls = new JPanel(new java.awt.GridLayout(0, 1));
        lowerControls.add(filters);
        if (onAskPage != null) {
            JPanel directPage = new JPanel(new FlowLayout(FlowLayout.LEFT));
            directPage.add(new JLabel("Page slug from Canvas URL:"));
            pageSlug.getAccessibleContext().setAccessibleName("Canvas page slug");
            directPage.add(pageSlug);
            directPage.add(usePage);
            lowerControls.add(directPage);
            usePage.addActionListener(event -> {
                ResourcesViewModel.CourseOption course = (ResourcesViewModel.CourseOption) courses.getSelectedItem();
                if (course != null && !pageSlug.getText().isBlank()) {
                    onAskPage.accept(new ResourcesViewModel.Row(course.id, pageSlug.getText().strip(),
                            pageSlug.getText().strip(), "PAGE", course.name, ""));
                }
            });
        }
        heading.add(controls, BorderLayout.CENTER); heading.add(lowerControls, BorderLayout.SOUTH);
        JScrollPane results = new JScrollPane(table);
        results.setColumnHeaderView(table.getTableHeader());
        add(heading, BorderLayout.NORTH); add(results, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(4, 4));
        status.setEditable(false); status.setLineWrap(true); status.setWrapStyleWord(true);
        footer.add(new JScrollPane(status), BorderLayout.CENTER); JPanel actions = new JPanel(new FlowLayout());
        actions.add(open);
        if (onAskPage != null) { actions.add(ask); }
        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Canvas titles are inert text, including strings starting with <html>.
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                label.putClientProperty("html", null); return label;
            }
        });
        courses.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                label.putClientProperty("html", null); return label;
            }
        });
        search.addActionListener(event -> submit(false));
        query.addActionListener(event -> { if (search.isEnabled()) { submit(false); } });
        reload.addActionListener(event -> submit(true));
        open.addActionListener(event -> openSource());
        ask.addActionListener(event -> {
            int selected = table.getSelectedRow();
            if (selected >= 0 && onAskPage != null) {
                onAskPage.accept(rows.get(table.convertRowIndexToModel(selected)));
            }
        });
        table.getSelectionModel().addListSelectionListener(event -> {
            int selected = table.getSelectedRow();
            open.setEnabled(table.isEnabled() && selected >= 0);
            ask.setEnabled(table.isEnabled() && selected >= 0
                    && selected < rows.size() && "PAGE".equals(rows.get(table.convertRowIndexToModel(selected)).type));
        });
        model.addListener(event -> SwingUtilities.invokeLater(() -> render(model.getState())));
        render(model.getState());
    }
    public void loadCourses() { submit(true); }
    private void submit(boolean loadCourses) {
        ResourcesViewModel.CourseOption selected = (ResourcesViewModel.CourseOption) courses.getSelectedItem();
        search.setEnabled(false); reload.setEnabled(false); courses.setEnabled(false); query.setEnabled(false); open.setEnabled(false);
        table.setEnabled(false);
        ask.setEnabled(false);
        usePage.setEnabled(false);
        pageSlug.setEnabled(false);
        status.setText("Loading resource metadata…");
        controller.search(loadCourses || selected == null ? null : selected.id, loadCourses ? "" : query.getText());
    }
    private void render(ResourcesViewModel.State state) {
        ResourcesViewModel.CourseOption previous = (ResourcesViewModel.CourseOption) courses.getSelectedItem();
        courses.removeAllItems();
        for (ResourcesViewModel.CourseOption course : state.courses) {
            courses.addItem(course);
            if (previous != null && course.id == previous.id) { courses.setSelectedItem(course); }
        }
        rows = state.rows;
        tableModel.setRowCount(0);
        for (ResourcesViewModel.Row row : rows) { tableModel.addRow(new String[]{row.title, row.type, row.course, row.url}); }
        String message = state.message;
        if (usePage.getParent() != null) {
            message += "\nIf a page is absent from the directory, enter its slug from the Canvas URL above. Canvas access is checked when you ask.";
        }
        status.setText(message); status.setCaretPosition(0);
        usePage.setEnabled(!state.courses.isEmpty());
        pageSlug.setEnabled(true);
        table.setEnabled(true);
        search.setEnabled(!state.courses.isEmpty()); reload.setEnabled(true); courses.setEnabled(true); query.setEnabled(true); open.setEnabled(false); ask.setEnabled(false);
    }
    private void openSource() {
        int selected = table.getSelectedRow();
        if (selected < 0) { return; }
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                status.setText("Copy the source URL from the table into your browser."); return;
            }
            Desktop.getDesktop().browse(URI.create(rows.get(table.convertRowIndexToModel(selected)).url));
        }
        catch (IOException | RuntimeException exception) { status.setText("Could not open the browser. Copy the source URL from the table."); }
    }
}
