//
// $
//

package edu.gemini.spModel.telescope;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * The Instrument Support Structure (ISS) port.  Some instrument features will
 * differ depending upon which port they are mounted on.
 */
public enum IssPort implements DisplayableSpType, LoggableSpType, SequenceableSpType {
    SIDE_LOOKING("Side-looking"),
    UP_LOOKING("Up-looking"),
    ;

    public static final IssPort DEFAULT = SIDE_LOOKING;

    private String _displayValue;

    private IssPort(String displayValue) {
        _displayValue = displayValue;
    }

    public String displayValue() {
        return _displayValue;
    }

    public String sequenceValue() {
        return _displayValue;
    }

    public String logValue() {
        return _displayValue;
    }

    public String shortName() {
        int i = name().indexOf('_');
        return (i >= 0) ? name().substring(0, i) : name();
    }

    public String toString() {
        return _displayValue;
    }

    /** Return a Port by name **/
    public static IssPort getPort(String name) {
        return getPort(name, DEFAULT);
    }

    /** Return a Port by name giving a value to return upon error **/
    public static IssPort getPort(String name, IssPort nvalue) {
        return SpTypeUtil.oldValueOf(IssPort.class, name, nvalue);
    }
}
