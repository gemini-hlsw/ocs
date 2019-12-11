package edu.gemini.spModel.seqcomp;

public interface GhostIExpSeqComponent extends IObserveSeqComponent {
    public double getRedExposureTime();
    public void setRedExposureTime(double expTime);
    public double getBlueExposureTime();
    public void setBlueExposureTime(double expTime);
}
