package edu.gemini.spModel.seqcomp;

/**
 * A Ghost sequence component that supports red and blue exposure counts and counts.
 */
public interface GhostExpCountSeqComponent extends IObserveSeqComponent {
    int getRedExposureCount();
    void setRedExposureCount(int newValue);
    int getBlueExposureCount();
    void setBlueExposureCount(int newValue);
}
