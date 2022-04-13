//
//$Id: IRule.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.p2checker.api;


import java.util.logging.Logger;

/**
 * Public interface for any given rule that can be used to validate a program.
 * Any <code>IRule</code> must be designed to operate on an <code>ISPObservation</code>,
 * since that's the minimum unit that contains enough information to be validated.
 * <p/>
 * Every <code>ISPObservation</code> within the program is internally encapsulated in
 * <code>ObservationElements</code> objects before calling the {@link #check(ObservationElements)}
 * method. Using <code>ObservationElements</code> objects allows the implementation
 * classes to get access to all the elements within an <code>ISPObservation</code>.
 * This makes it easier to implement complex validation rules that involve several components
 * within an observation.
 * </p>
 * @see edu.gemini.p2checker.api.ObservationElements
 *
 */
public interface IRule {

    Logger LOG = Logger.getLogger(IRule.class.getName());
    // shortcuts
    Problem.Type WARNING = Problem.Type.WARNING;
    Problem.Type ERROR = Problem.Type.ERROR;

    /**
     * Perform a validation of an observation represented by the
     * <code>ObservationElements</code>. The result of the validation are
     * <code>P2Problems</code>.
     *
     * @param elements ObservationElements object that contains the observation
     * components needed by the rule to perform the checking.
     * @return Problems found while validating the observation
     */
    IP2Problems check(ObservationElements elements) ;

}
