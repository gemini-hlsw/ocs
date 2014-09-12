//
// $Id: ExecActionAdapter.java 6750 2005-11-20 22:23:04Z shane $
//

package edu.gemini.spModel.event;

/**
 * An empty implementation of the ExecAction, to be used to create
 * subclasses that care about a subset of the events.  All method
 * implementations are empty.
 */
public abstract class ExecActionAdapter implements ExecAction {

    public void abortObserve(ExecEvent event) {
    }

    public void pauseObserve(ExecEvent event) {
    }

    public void continueObserve(ExecEvent event) {
    }

    public void stopObserve(ExecEvent event) {
    }

    public void startVisit(ExecEvent event) {
    }

    public void slew(ExecEvent event) {
    }

    public void startSequence(ExecEvent event) {
    }

    public void startDataset(ExecEvent event) {
    }

    public void endDataset(ExecEvent event) {
    }

    public void endSequence(ExecEvent event) {
    }

    public void endVisit(ExecEvent event) {
    }

    public void overlap(ExecEvent event) {
    }

    public void startIdle(ExecEvent event) {
    }

    public void endIdle(ExecEvent event) {
    }
}
