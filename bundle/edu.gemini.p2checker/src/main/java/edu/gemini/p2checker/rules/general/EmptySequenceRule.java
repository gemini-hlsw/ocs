//
// $Id$
//

package edu.gemini.p2checker.rules.general;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.p2checker.util.P2CheckerUtil;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.ISPSeqObject;
import edu.gemini.spModel.seqcomp.IObserveSeqComponent;


import java.util.List;

/**
 * A rule that checks for zero step instrument or offset iterators, and
 * leaf nodes that aren't observe iterators.
 */
public class EmptySequenceRule implements IRule {
    private static final String PREFIX = "EmptySequenceRule_";

    private static final String ZERO_STEP_ITERATOR_MESSAGE =
            "Each offset iterator or instrument sequence should have at least one step.";

    private static final String EMPTY_ITERATOR_MESSAGE =
            "At least one observe, dark, flat, bias or arc should be included " +
            "in every offset iterator or instrument sequence.";

    /**
     * State that is kept in the stack while checking a sequence.
     */
    private class State {
        boolean zeroStepFound = false;
        boolean emptyIteratorFound = false;

        boolean shouldContinueChecking() {
            return !(zeroStepFound && emptyIteratorFound);
        }
    }

    public IP2Problems check(ObservationElements elements)  {

        ISPSeqComponent seqComp = elements.getSeqComponentNode();
        if (seqComp == null) return null; // no sequence to check.

        // Do a DFS on the sequence looking for leaf iterators that aren't
        // observes of some kind.  Looking for zero step iterators.
        State s = new State();
        check(seqComp, s);

        // If no problems are found, return the constant empty problems list.
        if (!s.zeroStepFound && !s.emptyIteratorFound) {
            return P2CheckerUtil.NO_PROBLEMS;
        }

        // Add the appropriate problems.
        IP2Problems probs = new P2Problems();
        if (s.zeroStepFound) {
            probs.addError(PREFIX+"ZERO_STEP_ITERATOR_MESSAGE", ZERO_STEP_ITERATOR_MESSAGE, seqComp);
        }
        if (s.emptyIteratorFound) {
            probs.addError(PREFIX+"EMPTY_ITERATOR_MESSAGE", EMPTY_ITERATOR_MESSAGE, seqComp);
        }

        return probs;
    }

    // Traverses the sequence looking for
    private void check(ISPSeqComponent seqComp, State state)  {
        Object dobj = seqComp.getDataObject();
        boolean isObs = (dobj instanceof IObserveSeqComponent);
        List<ISPSeqComponent> children = seqComp.getSeqComponents();
        if ((children == null) || (children.size() == 0)) {
            if (!isObs) {
                state.emptyIteratorFound = true;
            }
        } else {
            //noinspection unchecked
            for (ISPSeqComponent child : children) {
                check(child, state);
                if (!state.shouldContinueChecking()) return;
            }
        }

        if (!state.zeroStepFound && (dobj instanceof ISPSeqObject)) {
            if (((ISPSeqObject) dobj).getStepCount() <= 0) {
                state.zeroStepFound = true;
            }
        }
    }
}
