// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id:$
//
package edu.gemini.spModel.smartgcal.functors;

import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProviderHolder;
import edu.gemini.spModel.gemini.calunit.smartgcal.VersionInfo;
import edu.gemini.spModel.smartgcal.provider.CalibrationProviderImpl;
import edu.gemini.spModel.smartgcal.repository.CalibrationUpdateEvent;
import edu.gemini.spModel.smartgcal.repository.CalibrationUpdater;


import java.util.List;

public class CalibrationUpdateService {

    public static List<VersionInfo> getVersionInfo() {
        return CalibrationProviderHolder.getProvider().getVersionInfo();
    }

    public static CalibrationUpdateEvent updateNow() {
        CalibrationUpdateEvent event = CalibrationUpdater.instance.updateNow();
        // TODO: this should be part of the updateNow() method (?)
        CalibrationProviderImpl provider = (CalibrationProviderImpl) CalibrationProviderHolder.getProvider();
        provider.update();
        return event;
    }

    public static long nextUpdate() {
        return CalibrationUpdater.instance.nextUpdate();
    }
}
