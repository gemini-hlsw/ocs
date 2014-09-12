package edu.gemini.spModel.inst;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;

/**
 * Created with IntelliJ IDEA.
 * User: sraaphor
 * Date: 3/15/14
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
public enum PositionAngleMode implements DisplayableSpType, LoggableSpType, SequenceableSpType {
    EXPLICITLY_SET("Explicitly Set"),
    MEAN_PARALLACTIC_ANGLE("Mean Parallactic Angle");

    private final String _displayValue;

    PositionAngleMode(String displayValue) {
        _displayValue = displayValue;
    }

    public String displayValue() {
        return _displayValue;
    }

    public String logValue() {
        return _displayValue;
    }

    public String sequenceValue() {
        return _displayValue;
    }

    @Override
    public String toString() {
        return _displayValue;
    }
}
