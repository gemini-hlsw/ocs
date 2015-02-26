package edu.gemini.itc.operation;

public interface SourceFractionCalculatable extends Calculatable {
    public void setImageQuality(double im_qual);

    public void setApType(boolean ap_type);

    public void setApDiam(double ap_diam);

    public void setSFPrint(boolean SFprint);

    public double getSourceFraction();

    public double getNPix();

    public double getApDiam();

    public double getApPix();

    public double getSwAp();

}
