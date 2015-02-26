package jsky.app.ot.ags;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;

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

    private final PropertyChangeListener obsListener = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    updateAgsContext(obs);
                }
            });
        }
    };

    private void updateAgsContext(ISPObservation obs) {
        final AgsContext newOptions = AgsContext.create(obs);
        final AgsContext oldOptions = agsContext;
        if (!oldOptions.equals(newOptions)) {
            agsContext = newOptions;
            for (AgsContextSubscriber s : new ArrayList<>(subs)) {
                s.notify(obs, oldOptions, newOptions);
            }
        }
    }

    public void watch(ISPObservation obs) {
        if (this.obs != null) {
            this.obs.removeCompositeChangeListener(obsListener);
        }
        this.obs        = obs;
        this.agsContext = AgsContext.create(obs);
        if (this.obs != null) {
            this.obs.addCompositeChangeListener(obsListener);
        }
    }

    public AgsContext getAgsContext() {
        return agsContext;
    }
}
