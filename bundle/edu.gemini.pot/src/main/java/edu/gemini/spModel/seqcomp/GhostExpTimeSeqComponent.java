package edu.gemini.spModel.seqcomp;

/**
 * A Ghost sequence component that supports red and blue exposure time.
 */
public interface GhostExpTimeSeqComponent extends IObserveSeqComponent {
    double getRedExposureTime();
    void setRedExposureTime(double expTime);
    double getBlueExposureTime();
    void setBlueExposureTime(double expTime);
}
