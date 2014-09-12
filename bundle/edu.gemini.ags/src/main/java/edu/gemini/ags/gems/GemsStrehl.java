package edu.gemini.ags.gems;

/**
 * See OT-27
 */
public class GemsStrehl {
    private double avg;
    private double rms;
    private double min;
    private double max;

    public GemsStrehl(double avg, double rms, double min, double max) {
        this.avg = avg;
        this.rms = rms;
        this.min = min;
        this.max = max;
    }

    public double getAvg() {
        return avg;
    }

    public double getRms() {
        return rms;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "GemsStrehl{" +
                "avg=" + avg +
                ", rms=" + rms +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
