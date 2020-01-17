package edu.gemini.spModel.seqcomp;

/**
 * A Ghost sequence component that supports red and blue exposure time.
 */
public interface GhostExpTimeSeqComponent extends IObserveSeqComponent {
    public double getRedExposureTime();
    public void setRedExposureTime(double expTime);
    public double getBlueExposureTime();
    public void setBlueExposureTime(double expTime);
}
