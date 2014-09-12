//
// $
//

package edu.gemini.spModel.telescope;

import edu.gemini.pot.sp.ISPNode;

/**
 * An interface used to mark and work with ISS Port sensitive components.  That
 * is, components that must be updated in some way when the ISS Port value
 * associated with an instrument changes.
 */
public interface IssPortSensitiveComponent {

    /**
     *
     * @param providerNode remote node whose data object can be assumed to
     * implement {@link IssPortProvider}
     *
     * @param oldValue old {@link IssPort} value for this provider
     * @param newValue new {@link IssPort} value for this provider
     */
    void handleIssPortUpdate(ISPNode providerNode, IssPort oldValue, IssPort newValue);
}
