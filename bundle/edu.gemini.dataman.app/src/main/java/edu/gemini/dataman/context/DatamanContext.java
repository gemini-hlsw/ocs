//
// $Id: DatamanContext.java 131 2005-09-14 18:22:24Z shane $
//

package edu.gemini.dataman.context;


/**
 * The context in which the Dataman app executes.  Contains the external
 * {@link DatamanServices services} used by the Dataman app, its
 * {@link DatamanConfig configuration} and its {@link DatamanState state}.
 */
public interface DatamanContext extends DatamanServices {

    /**
     * Gets (mutable) state information associated with the Dataman app.
     */
    DatamanState getState();

    /**
     * Gets (immutable) configuration information associated with the Dataman
     * app.
     */
    DatamanConfig getConfig();
}
