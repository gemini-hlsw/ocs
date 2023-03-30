package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.*;
import static edu.gemini.spModel.obscomp.InstConstants.COADDS_KEY;
import static edu.gemini.spModel.obscomp.InstConstants.EXPOSURE_TIME_KEY;

/**
 * Provides a generic, exposure time * coadds calculator.
 */
public enum ExposureCalculator {
    instance;

    /**
     * Computes the exposure time indicated in the given Config by multiplying
     * nominal exposure time by the number of coadds.
     * @return CategorizedTime with category {@link Category@EXPOSURE}
     */
    public CategorizedTime totalExposureTime(Config stepConfig) {
        return categorize(totalExposureTimeSec(stepConfig));
    }

    public CategorizedTime categorize(double secs) {
        return CategorizedTime.fromSeconds(Category.EXPOSURE, secs);
    }

    /**
     * Computes the exposure time indicated in the given Config by multiplying
     * nominal exposure time by the number of coadds.
     * @return time in seconds
     */
    public double totalExposureTimeSec(Config stepConfig) {
        return exposureTimeSec(stepConfig) * coadds(stepConfig);
    }

    /**
     * Extracts the exposure time item from the Config, defaulting to 0.0 sec.
     * @return exposure time in seconds
     */
    public double exposureTimeSec(Config stepConfig) {
        Double exposureTime = (Double) stepConfig.getItemValue(EXPOSURE_TIME_KEY);
        if (exposureTime == null) exposureTime = 0.0;
        return exposureTime;
    }

    /**
     * Extracts the number of coadds from the Config, defaulting to 1.
     * @return exposure time in seconds
     */
    public int coadds(Config stepConfig) {
        Integer coadds = (Integer) stepConfig.getItemValue(COADDS_KEY);
        if (coadds == null) coadds = 1;
        return coadds;
    }

}
