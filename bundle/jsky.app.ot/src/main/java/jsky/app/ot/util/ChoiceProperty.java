// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ChoiceProperty.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.util;

import java.io.Serializable;


/**
 * This class represents an object property that consists of a set of
 * possible string choices, once of which is the "current" choice.
 */
public class ChoiceProperty implements Serializable {
    // Choices
    private String[] _choices;

    // The current choice index.
    private int _curValue = -1;

    /**
     * Construct with the set of choices.  The initial choice is the
     * 1'st element (index 0).
     */
    public ChoiceProperty(String[] choices) {
        _choices = choices;
        if (choices.length > 0) {
            _curValue = 0;
        }
    }

    /**
     * Get the set of choices that are available.
     */
    public String[] getChoices() {
        return _choices;
    }

    /**
     * Get the index of the current choice.
     */
    public int getCurValue() {
        return _curValue;
    }

    /**
     * Set the current choice (index).
     */
    public void setCurValue(int value) {
        if (value >= _choices.length) {
            _curValue = _choices.length - 1;
        } else if (value < 0) {
            _curValue = 0;
        }
        _curValue = value;
    }

}

