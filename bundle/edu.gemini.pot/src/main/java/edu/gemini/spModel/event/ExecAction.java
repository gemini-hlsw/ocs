//
// $Id: ExecAction.java 6750 2005-11-20 22:23:04Z shane $
//

package edu.gemini.spModel.event;

/**
 * An interface containing a method for each type of event, used with the
 * {@link ExecEvent#doAction(ExecAction)} implementation.
 * Various actions that must perform distinct tasks based upon event type may
 * implement this interface to avoid switch like statements.
 */
public interface ExecAction {

    // Sequence command actions.

    /**
     * Performs the action associated with an abort observe event.
     */
    void abortObserve(ExecEvent event);

    /**
     * Performs the action associated with a pause observe event.
     */
    void pauseObserve(ExecEvent event);

    /**
     * Performs the action associated with a continue observe event.
     */
    void continueObserve(ExecEvent event);

    /**
     * Performs the action associated with an abort observe event.
     */
    void stopObserve(ExecEvent event);


    // Other observation execution actions.

    /**
     * Performs the action associated with a start visit event.
     */
    void startVisit(ExecEvent event);

    /**
     * Performs the action associated with a slew event.
     */
    void slew(ExecEvent event);

    /**
     * Performs the action associated with a start sequence event.
     */
    void startSequence(ExecEvent event);

    /**
     * Performs the action associated with a start dataset event.
     */
    void startDataset(ExecEvent event);

    /**
     * Performs the action associated with an end dataset event.
     */
    void endDataset(ExecEvent event);

    /**
     * Performs the action associated with an end sequence event.
     */
    void endSequence(ExecEvent event);

    /**
     * Performs the action associated with an end visit event.
     */
    void endVisit(ExecEvent event);


    // Time accounting actions.

    /**
     * Performs the action associated with an overlap event.
     */
    void overlap(ExecEvent evt);

    /**
     * Performs the action associated with a start idle event.
     */
    void startIdle(ExecEvent event);

    /**
     * Performs the action associated with an end idle event.
     */
    void endIdle(ExecEvent event);
}
