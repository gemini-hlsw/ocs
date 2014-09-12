package edu.gemini.qpt.shared.sp;

import edu.gemini.spModel.type.DisplayableSpType;

/**
 * An enum that represents the band of a program/observation. This is a combination of whether a program is classical
 * (strictly speaking there is no band in this case) or whether it has a queue band (see @link{Prog.getQueueBand}).
 * Having these different concepts represented in a single enum makes filtering a bit simpler.
 */
public enum Band implements DisplayableSpType {

    Band1("Band 1"),
    Band2("Band 2"),
    Band3("Band 3"),
    Band4("Band 4"),        // aka "Poor Weather Band"
    Undefined("No Band");   // the band in the sp model can be null, we translate this here to "Undefined"

    private final String displayValue;

    private Band(String displayValue) {
        this.displayValue = displayValue;
    }

    public String displayValue() {
        return displayValue;
    }

}
