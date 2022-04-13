//
// $Id$
//

package edu.gemini.p2checker.api;

import edu.gemini.p2checker.util.UnmodifiableP2Problems;
import edu.gemini.pot.sp.ISPProgramNode;

import java.util.List;

/**
 *
 */
public interface IP2Problems {

    IP2Problems EMPTY = new UnmodifiableP2Problems(new P2Problems());

    /**
     * Adds a Problem with <code>ProblemType.WARNING</code> type set
     * @param id the unique problem id
     * @param description the description of the problem
     * @param node the node where this error was found
     * @return the newly created warning-problem, so clients can do special operations with it.
     */
    Problem addWarning(String id, String description, ISPProgramNode node);

    /**
     * Adds a Problem with <code>ProblemType.ERROR</code> type set
     * @param id the unique problem id
     * @param description the description of the problem
     * @param node the node where this warning was found
     * @return the newly created error-problem, so clients can do special operations with it.
     */
    Problem addError(String id, String description, ISPProgramNode node);

    /**
     * Auxiliary method to append the problems in the <code>problems</code> object to
     * this <code>P2Problems</code> representation
     * @param problems the <code>P2Problems</code> to add. If the object is null or
     * contains no actual problems, this operation is ignored.
     */
    void append(IP2Problems problems);

    /**
     * Add the given <code>problem</code> to this representation of <code>P2Problems</code>.
     * @param problem The <code>Problem</code> to add. If this object is null, the operation
     * does not have any effect.
     */
    void append(Problem problem);

    IP2Problems appended(Problem problem);

    IP2Problems appended(IP2Problems problems);

    /**
     * Get the problems in this container.
     * @return a List of <code>Problem</code>. If no problems are found, and empty valid list
     * is returned.
     */
    List<Problem> getProblems();

    /**
     * Remove all the problems from this <code>P2Problem</code> representation
     */
    void clear();

    /**
     * Gets the type of the most severe problem type found in this  <code>P2Problem</code> representation
     * @return the severest problem type found, or <code>null</code> if there are no problems
     */
    Problem.Type getSeverity();

    /**
     * Return the ammount of warnings found in this container of problems.
     * @return the ammount of warnings found in this container of problems. The
     * result will take into account both regular
     * {@link Problem} (counted as 1) as well as
     * {@link ProblemRollup} objects
     * (counted as many as it represents)
     */
    int getWarningCount();

    /**
     * Return the amount of errors in this container of problems.
     * @return the amount of errors in this container of problems.
     * The result will take into account both regular
     * {@link Problem} (counted as 1) as
     * well as {@link ProblemRollup} objects
     * (counted as many as it represents)
     */
    int getErrorCount();

    /**
     * Returns the count of problems of any type. The result will take into
     * account both regular {@link Problem}
     * (counted as 1) as well as {@link ProblemRollup}
     * objects (counted as many as it represents)
     */
    int getProblemCount();
}
