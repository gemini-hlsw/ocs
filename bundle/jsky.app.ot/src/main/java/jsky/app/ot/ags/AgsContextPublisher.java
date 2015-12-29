package jsky.app.ot.ags;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.Option;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides support for watching an observation for changes to available and
 * default AGS options.
 */
public final class AgsContextPublisher {
    private ISPObservation obs;
    private AgsContext agsContext = AgsContext.EMPTY;
    private final List<AgsContextSubscriber> subs = new ArrayList<>();

    public void subscribe(AgsContextSubscriber sub) {
        subs.add(sub);
    }
    public void removeSubscription(AgsContextSubscriber sub) {
        subs.remove(sub);
    }

    private final PropertyChangeListener obsListener = evt -> SwingUtilities.invokeLater(() -> updateAgsContext(obs));


    private void updateAgsContext(final ISPObservation obs) {
        final AgsContext newOptions = AgsContext.create(obs);
        final AgsContext oldOptions = agsContext;
        if (!oldOptions.equals(newOptions)) {
            agsContext = newOptions;
            new ArrayList<>(subs).forEach(s -> s.notify(obs, oldOptions, newOptions));
        }
    }

    public void watch(final Option<ISPObservation> obsShell) {
        unwatch();
        obsShell.foreach(this::watch);
    }

    public void unwatch() {
        if (obs != null) {
            obs.removeCompositeChangeListener(obsListener);
        }
        obs        = null;
        agsContext = AgsContext.EMPTY;
    }

    private void watch(final ISPObservation newObs) {
        obs        = newObs;
        agsContext = AgsContext.create(newObs);
        obs.addCompositeChangeListener(obsListener);
    }

    public AgsContext getAgsContext() {
        return agsContext;
    }
}
