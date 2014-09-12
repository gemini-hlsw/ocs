package edu.gemini.ags.gems.mascot;

/**
 * Java interface reporting Mascot progress
 */
public interface MascotProgress {
    /**
     * Reports on the progress of the Mascot calculations and can cancel (or stop) the calculations by returning false.
     * @param s results of one Strehl calculation
     * @param count the count of Strehl objects calculated so far
     * @param total the total number of Strehl objects to calculate
     * @param usable true if the Strehl object is usable (all of the stars are at valid locations/can be used)
     * @return true to continue, false to cancel
     */
    public boolean progress(Strehl s, int count, int total, boolean usable);

    /**
     * Sets the title for the progress dialog
     * @param s the title string
     */
    void setProgressTitle(String s);
}
