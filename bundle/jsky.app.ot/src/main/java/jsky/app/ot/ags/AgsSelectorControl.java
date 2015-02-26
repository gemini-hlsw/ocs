package jsky.app.ot.ags;

import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.shared.util.immutable.Option;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class AgsSelectorControl {
    public interface Listener {
        public void agsStrategyUpdated(Option<AgsStrategy> strategy);
    }

    private final List<Listener> listeners = new ArrayList<>();

    protected void fireSelectionUpdate(Option<AgsStrategy> strategy) {
        for (Listener l : new ArrayList<>(listeners)) {
            l.agsStrategyUpdated(strategy);
        }
    }

    public abstract JComponent getUi();
    public abstract void setAgsOptions(AgsContext opts);

    public void addSelectionListener(Listener l) {
        listeners.add(l);
    }

    public void removeSelectionListener(Listener l) {
        listeners.remove(l);
    }

}
