package edu.gemini.itc.operation;

public interface ImagingS2NCalculatable extends Calculatable {

    public void setSecondaryIntegral(double secondary_integral);

    public void setSecondarySourceFraction(double secondary_source_fraction);

    public String getBackgroundLimitResult();

}
