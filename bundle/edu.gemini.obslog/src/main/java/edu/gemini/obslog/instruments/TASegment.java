package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.*;
import edu.gemini.obslog.transfer.EChargeObslogVisit;
import edu.gemini.spModel.time.TimeAmountFormatter;

import java.io.Serializable;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: TASegment.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public final class TASegment extends OlBasicSegment implements Serializable {
    //private static final Logger LOG = LogUtil.getLogger(TASegment.class);

    private static final String NARROW_TYPE = "TA";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Time Accounting Log";

    private static final String CHECKBOX_NAME = "Cb";

    private boolean _isMultiNight;

    public TASegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems);
        if (logItems == null || obsLogOptions == null) throw new NullPointerException();

        _isMultiNight = obsLogOptions.isMultiNight();
    }

    /**
     * Add information for one observation to the segment.  This method calls mandatory decorators and allows
     * the instrument segment to decorate its own items.
     * This is only called if the observation has unique configs
     *
     * @param evisit the <tt>ObsVisit</tt>
     */
    public void addObservationData(EChargeObslogVisit evisit) {
        // Here we get a normal visit with one config and a set of datasets that  match, so unroll it

        // Create one map and share the EObslogVisit uniqueconfig for all of them
        TAConfigMap map = new TAConfigMap(evisit, getTableInfo());

        ConfigMapUtil.decorateDatasetUT(map, _isMultiNight);
        ConfigMapUtil.addCommentRowCount(map);

        decorateObservationData(map);

        // Add them all to the segment data
        _getSegmentDataList().add(map);
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map <code>TAConfigMap</code>
     */
    public void decorateObservationData(ConfigMap map) {
        _decorateTimes(map);
        _decorateCheckbox(map);
    }

    private void _decorateTimes(ConfigMap map) {
        if (map == null) return;

        String time = map.sget(ConfigMapUtil.OBSLOG_NONCHARGED_NAME);
        if (time != null) {
            map.put(ConfigMapUtil.OBSLOG_NONCHARGED_NAME, TimeAmountFormatter.getHMSFormat(Long.parseLong(time)));
        }

        time = map.sget(ConfigMapUtil.OBSLOG_PARTNERCHARGED_NAME);
        if (time != null) {
            map.put(ConfigMapUtil.OBSLOG_PARTNERCHARGED_NAME, TimeAmountFormatter.getHMSFormat(Long.parseLong(time)));
        }

        time = map.sget(ConfigMapUtil.OBSLOG_PROGRAMCHARGED_NAME);
        if (time != null) {
            map.put(ConfigMapUtil.OBSLOG_PROGRAMCHARGED_NAME, TimeAmountFormatter.getHMSFormat(Long.parseLong(time)));
        }
    }

    private void _decorateCheckbox(ConfigMap map) {
        if (map == null) return;

        map.put(CHECKBOX_NAME,  "bulk-" + map.get(ConfigMapUtil.OBSLOG_UNIQUECONFIG_ID));
    }

    public void completed(List<EChargeObslogVisit> evisits) {
        List<ConfigMap> maps = _getSegmentDataList();

        ConfigMapUtil.addTimeGaps(evisits, maps);
    }

    /**
     * Return the segment caption.
     *
     * @return The caption.
     */
    public String getSegmentCaption() {
        return SEGMENT_CAPTION;
    }
}
