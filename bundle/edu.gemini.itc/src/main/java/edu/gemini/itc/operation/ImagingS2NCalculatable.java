// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

public interface ImagingS2NCalculatable extends Calculatable {
    public void setSedIntegral(double sed_integral);

    public void setSourceFraction(double source_fraction);

    public void setSecondaryIntegral(double secondary_integral);

    public void setSecondarySourceFraction(double secondary_source_fraction);

    public void setDarkCurrent(double dark_current);

    public void setNpix(double Npix);

    public void setSkyIntegral(double sky_integral);

    public void setSkyAperture(double skyAper);

    public void setExtraLowFreqNoise(int elfnParam);

    public String getBackgroundLimitResult();

}
