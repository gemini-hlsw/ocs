package edu.gemini.spModel.seqcomp;

public interface GhostIExpSeqComponent extends IObserveSeqComponent {
    public double getRedExposureTime();
    public void setRedExposureTime(double expTime);
    public int getRedExposureCount();
    public void setRedExposureCount(int newValue);
    public double getBlueExposureTime();
    public void setBlueExposureTime(double expTime);
    public int getBlueExposureCount();
    public void setBlueExposureCount(int newValue);
}
