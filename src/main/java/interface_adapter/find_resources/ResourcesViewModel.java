package interface_adapter.find_resources;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

/** Immutable presentation snapshots; listeners decide how to dispatch onto their UI thread. */
public final class ResourcesViewModel {
    public static final class CourseOption {
        public final long id;
        public final String name;
        public CourseOption(long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
    public static final class Row {
        public final String title;
        public final String type;
        public final String course;
        public final String url;
        public Row(String title, String type, String course, String url) {
            this.title = title; this.type = type; this.course = course; this.url = url;
        }
    }
    public static final class State {
        public final List<CourseOption> courses;
        public final List<Row> rows;
        public final String message;
        public State(List<CourseOption> courses, List<Row> rows, String message) {
            this.courses = List.copyOf(courses); this.rows = List.copyOf(rows); this.message = message;
        }
    }
    private volatile State state = new State(List.of(), List.of(), "Load courses to start.");
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    public State getState() { return state; }
    public void setState(State next) {
        State previous = state; state = next; changes.firePropertyChange("state", previous, next);
    }
    public void addListener(PropertyChangeListener listener) { changes.addPropertyChangeListener(listener); }
}
