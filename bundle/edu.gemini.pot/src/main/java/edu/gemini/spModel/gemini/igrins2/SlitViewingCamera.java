package edu.gemini.spModel.gemini.igrins2;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;

public enum SlitViewingCamera implements DisplayableSpType, LoggableSpType {
    ONE_IMAGE_EXPOSURE("1 image / exposure"),
    CONTINUOUS("Continuous images")
    ;

    public static final SlitViewingCamera DEFAULT = SlitViewingCamera.ONE_IMAGE_EXPOSURE;
    private final String displayValue;

    SlitViewingCamera(final String displayValue) {
        this.displayValue = displayValue;
    }

    @Override
    public String displayValue() {
        return displayValue;
    }

    @Override
    public String logValue() {
        return displayValue();
    }

    @Override
    public String toString() {
        return displayValue;
    }

}
