package edu.gemini.pot.sp;

/**
 * This is the interface for a Science Program Observation node.  Note that
 * as an <code>{@link ISPContainerNode}</code>, the <code>ISPObservation</code>
 * may accept structure listeners.  See the <code>ISPContainerNode</code>
 * class description for more detail.
 */
public interface ISPObservation extends ISPObsComponentContainer, ISPContainerNode, ISPProgramNode {

    /**
     * Names the property in the <code>{@link SPStructureChange}</code>
     * object delivered when a sequence component is added or removed.
     */
    String SEQ_COMPONENT_PROP = "SeqComponent";

    /**
     * Gets the observation's number.  Each time an observation is
     * created, it is given a unique (within the program) number.
     */
    int getObservationNumber();

    /**
     * Gets the observation id, which is the reference number of the program
     * and a sequential index.  This id is known to, displayed to, and used
     * by humans.
     */
    SPObservationID getObservationID();

    /**
     * Returns the observation id as a string, if known, otherwise the given string.
     */
    String getObservationIDAsString(String s);

    ISPObsQaLog getObsQaLog();
    void setObsQaLog(ISPObsQaLog log) throws SPNodeNotLocalException, SPTreeStateException;
    ISPObsExecLog getObsExecLog();
    void setObsExecLog(ISPObsExecLog log) throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Gets the root sequence component in this observation.
     */
    ISPSeqComponent getSeqComponent();

    /**
     * Sets the root sequence component in this observation.
     *
     * <p>A structure change event is fired for this method.  The
     * <code>{@link SPStructureChange}</code> object delivered will have
     * <ul>
     *   <li>property name = <code>{@link #SEQ_COMPONENT_PROP}</code>
     *   <li>parent = this node
     *   <li>new value = the given <code>sequenceComponent</code>
     * </ul>
     *
     * @param sequenceComponent the new sequence component to take the place
     * of the existing one (if any)
     *
     * @throws SPNodeNotLocalException if the <code>sequenceComponent</code>
     * was not created in the same JVM as this observation
     *
     * @throws SPTreeStateException if the <code>sequenceComponent</code> is
     * already in another observation
     */
    void setSeqComponent(ISPSeqComponent sequenceComponent)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Removes the root sequence component, leaving the observation with no
     * sequence.
     *
     * <p>A structure change event is fired for this method.  The
     * <code>{@link SPStructureChange}</code> object delivered will have
     * <ul>
     *   <li>property name = <code>{@link #SEQ_COMPONENT_PROP}</code>
     *   <li>parent = this node
     *   <li>new value = <code>null</code>
     * </ul>
     */
    void removeSeqComponent();
}

