package edu.gemini.spModel.seqcomp;

/**
 * A Ghost sequence component that supports red and blue exposure counts and counts.
 */
public interface GhostExpCountSeqComponent extends IObserveSeqComponent {
    public int getRedExposureCount();
    public void setRedExposureCount(int newValue);
    public int getBlueExposureCount();
    public void setBlueExposureCount(int newValue);
}
