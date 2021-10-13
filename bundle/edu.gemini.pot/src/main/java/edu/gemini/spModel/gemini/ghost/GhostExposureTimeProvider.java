// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;

public interface GhostExposureTimeProvider {

    int getRedExposureCount();
    double getRedExposureTime();

    int getBlueExposureCount();
    double getBlueExposureTime();

    static DefaultParameter redExposureCountParameter(GhostExposureTimeProvider p) {
        return DefaultParameter.getInstance(Ghost.RED_EXPOSURE_COUNT_PROP(),  p.getRedExposureCount());
    }

    static DefaultParameter redExposureTimeParameter(GhostExposureTimeProvider p) {
        return DefaultParameter.getInstance(Ghost.RED_EXPOSURE_TIME_PROP(),  p.getRedExposureTime());
    }

    static DefaultParameter blueExposureCountParameter(GhostExposureTimeProvider p) {
        return DefaultParameter.getInstance(Ghost.BLUE_EXPOSURE_COUNT_PROP(),  p.getBlueExposureCount());
    }

    static DefaultParameter blueExposureTimeParameter(GhostExposureTimeProvider p) {
        return DefaultParameter.getInstance(Ghost.BLUE_EXPOSURE_TIME_PROP(),  p.getBlueExposureTime());
    }

    static void addToSysConfig(ISysConfig sc, GhostExposureTimeProvider p) {
        sc.putParameter(redExposureCountParameter(p));
        sc.putParameter(redExposureTimeParameter(p));

        sc.putParameter(blueExposureCountParameter(p));
        sc.putParameter(blueExposureTimeParameter(p));
    }

    static void addToConfig(IConfig config, String sysConfigName, GhostExposureTimeProvider p) {
        config.putParameter(sysConfigName, redExposureCountParameter(p));
        config.putParameter(sysConfigName, redExposureTimeParameter(p));

        config.putParameter(sysConfigName, blueExposureCountParameter(p));
        config.putParameter(sysConfigName, blueExposureTimeParameter(p));
    }
}
