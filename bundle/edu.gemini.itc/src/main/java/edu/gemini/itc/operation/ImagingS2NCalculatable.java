package edu.gemini.itc.operation;

public interface ImagingS2NCalculatable extends Calculatable {

    void setSecondaryIntegral(double secondary_integral);

    void setSecondarySourceFraction(double secondary_source_fraction);

    double getVarSource();
    double getVarBackground();
    double getVarDark();
    double getVarReadout();
    double getExposureTime();

    int numberSourceExposures();

    double singleSNRatio();
    double totalSNRatio();

}
