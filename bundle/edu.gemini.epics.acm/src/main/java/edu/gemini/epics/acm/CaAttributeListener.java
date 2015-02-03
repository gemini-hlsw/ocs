package edu.gemini.epics.acm;

import java.util.List;

/**
 * Defines the interface that must be implemented by clients to monitor an
 * attribute.
 * 
 * @author jluhrs
 *
 * @param <T>
 *            The type of the attribute that will be monitored.
 */
public interface CaAttributeListener<T> {
    /**
     * Called when the monitored attribute refreshes its value.
     * 
     * @param newVals
     *            the attributes's new value. It can be <code>null</code>.
     */
    public void onValueChange(List<T> newVals);

    /**
     * Called when the validity state of the attribute's value changes
     * 
     * @param newValidity
     *            the new validity state.
     */
    public void onValidityChange(boolean newValidity);
}
