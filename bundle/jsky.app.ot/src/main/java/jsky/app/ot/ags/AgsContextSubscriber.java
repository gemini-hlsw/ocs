package jsky.app.ot.ags;

import edu.gemini.pot.sp.ISPObservation;

/**
 * An interface for clients interested in changes to the AGS context of an
 * observation being edited.
 */
public interface AgsContextSubscriber {
    void notify(ISPObservation obs, AgsContext oldOptions, AgsContext newOptions);
}
