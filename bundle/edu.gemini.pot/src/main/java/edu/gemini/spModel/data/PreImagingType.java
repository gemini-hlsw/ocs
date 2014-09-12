package edu.gemini.spModel.data;

import edu.gemini.spModel.type.DisplayableSpType;

/**
 * A simple enum that represents pre-imaging on and off (or yes/no) for GMOS and F2.
 * In the context of QV and QPT we need a specific enum for each option we want to deal with, the generic
 * YesNoType unfortunately does not do. We might find a better solution for this, but for now I need to
 * introduce this type to avoid the need for changes to the existing QPT.
 */
public enum PreImagingType implements DisplayableSpType {

    TRUE("Yes"),
    FALSE("No");

    private String _displayValue;

    private PreImagingType(String displayValue) {
        _displayValue = displayValue;
    }

    public String displayValue() {
        return _displayValue;
    }

}
