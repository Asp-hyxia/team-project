package interface_adapter.ask_page;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public final class AskPageViewModel {
    public static final class State {
        public final String message;
        public final String sourceTitle;
        public final String sourceUrl;

        public State(String message, String sourceTitle, String sourceUrl) {
            this.message = message;
            this.sourceTitle = sourceTitle;
            this.sourceUrl = sourceUrl;
        }
    }

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private volatile State state = new State("Select a Canvas page and ask a question.", "", "");

    public State getState() { return state; }

    public void setState(State value) {
        State previous = state;
        state = value;
        changes.firePropertyChange("state", previous, value);
    }

    public void addListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }
}
