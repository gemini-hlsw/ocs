//
//$Id: P2Problems.java 43466 2012-03-22 14:56:36Z abrighton $
//

package edu.gemini.p2checker.api;

import edu.gemini.pot.sp.ISPProgramNode;

import java.io.Serializable;
import java.util.*;

/**
 * A storage class that contains <code>Problem</code> associated to
 * nodes. A <code>Problem</coe> is gotten by the execution of
 * <code>IRule</code> over different <code>ISPProgramNode</code>
 *
 * @see edu.gemini.p2checker.api.IRule
  */
public class P2Problems implements Serializable, IP2Problems {

//    public static final String PROBLEMS_PROP = "Problems";

    public static final List<Problem> NO_PROBLEM = Collections.unmodifiableList(new ArrayList<>());


    private Set<Problem> _problemSet;

    public P2Problems() {
    }


    public P2Problems(IP2Problems copy) {
        if (copy != null) {
            _problemSet = new HashSet<>(copy.getProblems());
        }
    }

    public static P2Problems singleton(Problem problem) {
        final Set<Problem> ps = new HashSet<>();
        ps.add(problem);
        return new P2Problems(ps);
    }

    public static P2Problems fromProblems(Collection<Problem> problems) {
        return new P2Problems(new HashSet<>(problems));
    }

    private P2Problems(Set<Problem> problems) {
        _problemSet = problems;
    }

    /**
     * Adds a Problem with <code>ProblemType.WARNING</code> type set
     * @param description the description of the problem
     * @param node the node where this error was found
     * @return the newly created warning-problem, so clients can do special operations with it.
     */
    public Problem addWarning(String id, String description, ISPProgramNode node) {
        Problem p = new Problem(Problem.Type.WARNING, id, description, node);
        _getProblems().add(p);
        return p;
    }

    /**
     * Adds a Problem with <code>ProblemType.ERROR</code> type set
     * @param id the unique problem id
     * @param description the description of the problem
     * @param node the node where this warning was found
     * @return the newly created error-problem, so clients can do special operations with it.
     */
    public Problem addError(String id, String description, ISPProgramNode node) {
        Problem p = new Problem(Problem.Type.ERROR, id, description, node);
        _getProblems().add(p);
        return p;
    }

    /**
     * Auxiliary method to append the problems in the <code>problems</code> object to
     * this <code>P2Problems</code> representation
     * @param problems the <code>P2Problems</code> to add. If the object is null or
     * contains no actual problems, this operation is ignored.
     */
    public void append(IP2Problems problems) {
        if ((problems == null) || (problems.getProblemCount() == 0)) return;
        _getProblems().addAll(problems.getProblems());
    }

    /**
     * Add the given <code>problem</code> to this representation of <code>P2Problems</code>.
     * @param problem The <code>Problem</code> to add. If this object is null, the operation
     * does not have any effect.
     */
    public void append(Problem problem) {
        if (problem == null) return;
        _getProblems().add(problem);
    }

    public IP2Problems appended(Problem problem) {
        final IP2Problems result;

        if (_problemSet == null) {
            result = singleton(problem);
        } else {
           final HashSet<Problem> copy = new HashSet<>(_problemSet);
           copy.add(problem);
           result = new P2Problems(copy);
        }

        return result;
    }

    public IP2Problems appended(IP2Problems problems) {
        final IP2Problems result;

        if (_problemSet == null) {
            result = fromProblems(problems.getProblems());
        } else {
           final HashSet<Problem> copy = new HashSet<>(_problemSet);
           copy.addAll(problems.getProblems());
           result = new P2Problems(copy);
        }

        return result;
    }

    /**
     * Get the problems in this container.
     * @return a List of <code>Problem</code>. If no problems are found, and empty valid list
     * is returned.
     */
    public List<Problem> getProblems() {
        if (_problemSet ==  null) return NO_PROBLEM;
        return Collections.unmodifiableList(new ArrayList<>(_problemSet));
    }

    private Set<Problem> _getProblems() {
        if (_problemSet ==  null) _problemSet = new HashSet<>();
        return _problemSet;
    }

    /**
     * Remove all the problems from this <code>P2Problem</code> representation
     */
    public void clear() {
        if (_problemSet != null) {
            _problemSet.clear();
        }
    }

    /**
     * Gets the type of the most severe problem type found in this  <code>P2Problem</code> representation
     * @return the severest problem type found, or <code>null</code> if there are no problems
     */
    public Problem.Type getSeverity() {
        if (_problemSet == null) return Problem.Type.NONE;
        for (Problem problem : _problemSet) {
            if (problem.getType() == Problem.Type.ERROR) return Problem.Type.ERROR;
        }
        return Problem.Type.WARNING;
    }

    /**
     * Return the ammount of warnings found in this container of problems.
     * @return the ammount of warnings found in this container of problems. The
     * result will take into account both regular
     * {@link Problem} (counted as 1) as well as
     * {@link ProblemRollup} objects
     * (counted as many as it represents)
     */
    public int getWarningCount() {
        return _getCountByType(Problem.Type.WARNING);
    }

    /**
     * Return the ammount of errors found in this container of problems.
     * @return the ammount of errors found in this container of problems.
     * The result will take into account both regular
     * {@link Problem} (counted as 1) as
     * well as {@link ProblemRollup} objects
     * (counted as many as it represents)
     */
    public int getErrorCount() {
        return _getCountByType(Problem.Type.ERROR);
    }

    /**
     * Returns the count of problems of any type. The result will take into
     * account both regular {@link Problem}
     * (counted as 1) as well as {@link ProblemRollup}
     * objects (counted as many as it represents)
     */
    public int getProblemCount() {
        if (_problemSet == null) return 0;
        return _problemSet.size();
    }

    //Utility method to get the count of problems with the specific type.
    private int _getCountByType(Problem.Type type) {
        if (_problemSet == null) return 0;

        int count = 0;
        for (Problem problem: _problemSet) {
            if (problem.getType() == type) {
                //Take into account problem representatives to get the appropriate accounting
                if (problem instanceof ProblemRollup) {
                    count += ((ProblemRollup)problem).getRepresentedCount();
                } else {
                    count++;
                }
            }
        }
        return count;
    }
}
