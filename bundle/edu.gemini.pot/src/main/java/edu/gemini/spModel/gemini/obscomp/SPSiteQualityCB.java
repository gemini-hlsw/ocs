//
// $Id$
//

package edu.gemini.spModel.gemini.obscomp;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.config.AbstractObsComponentCB;
import edu.gemini.spModel.data.config.*;


import java.util.List;
import java.util.Map;

/**
 *
 */
public class SPSiteQualityCB extends AbstractObsComponentCB {
    private static final String SYSTEM_NAME = "ocs";

    private static final String MIN_HOUR_ANGLE = "MinHourAngle";
    private static final String MAX_HOUR_ANGLE = "MaxHourAngle";

    private static final String MIN_AIRMASS = "MinAirmass";
    private static final String MAX_AIRMASS = "MaxAirmass";
    
    private static final String TIMING_WINDOW_START = "TimingWindowStart";
    private static final String TIMING_WINDOW_DURATION = "TimingWindowDuration";
    private static final String TIMING_WINDOW_REPEAT = "TimingWindowRepeat";
    private static final String TIMING_WINDOW_PERIOD = "TimingWindowPeriod";

    
    private SPSiteQuality _dataObj;

    public SPSiteQualityCB(ISPObsComponent obsComp) {
        super(obsComp);
    }

    public Object clone() {
        SPSiteQualityCB result = (SPSiteQualityCB) super.clone();
        result._dataObj = null;
        return result;
    }

    protected void thisReset(Map options)  {
        _dataObj = (SPSiteQuality) getDataObject();
    }

    protected boolean thisHasConfiguration()  {
        return _dataObj != null;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull)  {
        if (_dataObj == null) return;

        IConfigParameter obsConds = DefaultConfigParameter.getInstance("obsConditions");
        config.putParameter(SYSTEM_NAME, obsConds);

        ISysConfig sysConfig = (ISysConfig) obsConds.getValue();


        IParameter ip;

        ip = StringParameter.getInstance(
                SPSiteQuality.SKY_BACKGROUND_PROP.getName(),
                _dataObj.getSkyBackground().sequenceValue());
        sysConfig.putParameter(ip);

        ip = StringParameter.getInstance(
                SPSiteQuality.CLOUD_COVER_PROP.getName(),
                _dataObj.getCloudCover().sequenceValue());
        sysConfig.putParameter(ip);

        ip = StringParameter.getInstance(
                SPSiteQuality.IMAGE_QUALITY_PROP.getName(),
                _dataObj.getImageQuality().sequenceValue());
        sysConfig.putParameter(ip);

        ip = StringParameter.getInstance(
                SPSiteQuality.WATER_VAPOR_PROP.getName(),
                _dataObj.getWaterVapor().sequenceValue());
        sysConfig.putParameter(ip);

        SPSiteQuality.ElevationConstraintType elType =_dataObj.getElevationConstraintType();
        if (elType == SPSiteQuality.ElevationConstraintType.HOUR_ANGLE) {
            ip = StringParameter.getInstance(
                MIN_HOUR_ANGLE,
                String.valueOf(_dataObj.getElevationConstraintMin()));
            sysConfig.putParameter(ip);

            ip = StringParameter.getInstance(
                MAX_HOUR_ANGLE,
                String.valueOf(_dataObj.getElevationConstraintMax()));
            sysConfig.putParameter(ip);
        } else if (elType == SPSiteQuality.ElevationConstraintType.AIRMASS) {
            ip = StringParameter.getInstance(
                MIN_AIRMASS,
                String.valueOf(_dataObj.getElevationConstraintMin()));
            sysConfig.putParameter(ip);

            ip = StringParameter.getInstance(
                MAX_AIRMASS,
                String.valueOf(_dataObj.getElevationConstraintMax()));
            sysConfig.putParameter(ip);
        }

        List<SPSiteQuality.TimingWindow> timingWindows = _dataObj.getTimingWindows();
        if (timingWindows.size() != 0) {
            int i = -1;
            for(SPSiteQuality.TimingWindow timingWindow : timingWindows) {
                i++;
                ip = StringParameter.getInstance(
                    TIMING_WINDOW_START+i,
                    String.valueOf(timingWindow.getStart()));
                sysConfig.putParameter(ip);

                ip = StringParameter.getInstance(
                    TIMING_WINDOW_DURATION+i,
                    String.valueOf(timingWindow.getDuration()));
                sysConfig.putParameter(ip);

                ip = StringParameter.getInstance(
                        TIMING_WINDOW_REPEAT+i,
                    String.valueOf(timingWindow.getRepeat()));
                sysConfig.putParameter(ip);

                ip = StringParameter.getInstance(
                        TIMING_WINDOW_PERIOD+i,
                    String.valueOf(timingWindow.getPeriod()));
                sysConfig.putParameter(ip);
            }
        }

        _dataObj = null;
    }
}
