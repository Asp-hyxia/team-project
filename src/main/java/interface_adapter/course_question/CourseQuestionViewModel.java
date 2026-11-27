package interface_adapter.course_question;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

public final class CourseQuestionViewModel {
    public static final class PageOption {
        public final String id;
        public final String title;

        public PageOption(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    public static final class State {
        public final List<PageOption> pages;
        public final String result;

        public State(List<PageOption> pages, String result) {
            this.pages = List.copyOf(pages);
            this.result = result;
        }
    }

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final StringBuilder pendingAnswer = new StringBuilder();
    private volatile State state = new State(List.of(), "Loading course references...");

    public State getState() {
        return state;
    }

    void setState(State value) {
        State previous = state;
        state = value;
        changes.firePropertyChange("state", previous, value);
    }

    synchronized void appendAnswerDelta(String delta) {
        pendingAnswer.append(delta);
    }

    public synchronized String drainAnswerDelta() {
        String delta = pendingAnswer.toString();
        pendingAnswer.setLength(0);
        return delta;
    }

    public void addListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }
}
