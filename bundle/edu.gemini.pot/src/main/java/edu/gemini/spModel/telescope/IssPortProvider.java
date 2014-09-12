//
// $
//

package edu.gemini.spModel.telescope;

/**
 * A component that provides an IssPort property.
 */
public interface IssPortProvider {
    String PORT_PROPERTY_NAME = "issPort";

    /**
     * Returns the {@link IssPort} associated with the component.
     */
    IssPort getIssPort();

    /**
     * Updates the IssPort associated with the component, throwing a
     * property change event if the value has changed.
     *
     * @param port new ISS port value 
     */
    void setIssPort(IssPort port);
}
