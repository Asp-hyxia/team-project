package interface_adapter.summarize_announcements;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

/** Immutable presentation snapshots; listeners decide how to dispatch onto their UI thread. */
public final class SummarizeAnnouncementsViewModel {
    /** One dated announcement with its authoritative Canvas source link. */
    public static final class Row {
        public final String title;
        public final String date;
        public final String url;

        public Row(String title, String date, String url) {
            this.title = title;
            this.date = date;
            this.url = url;
        }
    }

    public static final class State {
        public final List<Row> rows;
        public final String summary;
        public final String message;
        public final String retrievalNote;
        public final boolean busy;

        public State(List<Row> rows, String summary, String message, String retrievalNote, boolean busy) {
            this.rows = List.copyOf(rows);
            this.summary = summary;
            this.message = message;
            this.retrievalNote = retrievalNote;
            this.busy = busy;
        }

        /** Same snapshot with only the busy flag changed, for UI-driven enablement. */
        public State withBusy(boolean busy) {
            return new State(rows, summary, message, retrievalNote, busy);
        }
    }

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private volatile State state =
            new State(List.of(), "", "Select a course and a date range, then summarize.", "", false);

    public State getState() { return state; }

    public void setState(State next) {
        State previous = state;
        state = next;
        changes.firePropertyChange("state", previous, next);
    }

    /** Convenience for a View that only flips enablement; never used to publish results. */
    public void setBusy(boolean busy) { setState(getState().withBusy(busy)); }

    public void addListener(PropertyChangeListener listener) { changes.addPropertyChangeListener(listener); }
}
