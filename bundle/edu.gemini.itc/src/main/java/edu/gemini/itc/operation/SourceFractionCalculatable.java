// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

public interface SourceFractionCalculatable extends Calculatable {
    public void setImageQuality(double im_qual);

    public void setApType(String ap_type);

    public void setApDiam(double ap_diam);

    public void setSFPrint(boolean SFprint);

    public double getSourceFraction();

    public double getNPix();

    public double getApDiam();

    public double getApPix();

    public double getSwAp();

}
