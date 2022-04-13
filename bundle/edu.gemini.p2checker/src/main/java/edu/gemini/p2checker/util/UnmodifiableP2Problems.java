//
// $Id$
//

package edu.gemini.p2checker.util;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.Problem;
import edu.gemini.pot.sp.ISPProgramNode;

import java.util.List;

/**
 * Implementation of {@link IP2Problems} in which all methods that modify the
 * state throw UnsupportedOperationException.
 */
public class UnmodifiableP2Problems implements IP2Problems {

    private final IP2Problems _delegate;

    public UnmodifiableP2Problems(IP2Problems delegate) {
        _delegate = delegate;
    }

    public Problem addWarning(String id, String description, ISPProgramNode node) {
        throw new UnsupportedOperationException();
    }

    public Problem addError(String id, String description, ISPProgramNode node) {
        throw new UnsupportedOperationException();
    }

    public void append(IP2Problems problems) {
        throw new UnsupportedOperationException();
    }

    public void append(Problem problem) {
        throw new UnsupportedOperationException();
    }

    public IP2Problems appended(Problem problem) {
        return new UnmodifiableP2Problems(_delegate.appended(problem));
    }

    public IP2Problems appended(IP2Problems problems) {
        return new UnmodifiableP2Problems(_delegate.appended(problems));
    }

    public List<Problem> getProblems() {
        // Assuming _delegate implements getProblems correctly ...
        return _delegate.getProblems();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Problem.Type getSeverity() {
        return _delegate.getSeverity();
    }

    public int getWarningCount() {
        return _delegate.getWarningCount();
    }

    public int getErrorCount() {
        return _delegate.getErrorCount();
    }

    public int getProblemCount() {
        return _delegate.getProblemCount();
    }
}
