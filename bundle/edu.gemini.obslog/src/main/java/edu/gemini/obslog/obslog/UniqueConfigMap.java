package edu.gemini.obslog.obslog;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.obsrecord.UniqueConfig;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.DisplayableSpType;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: UniqueConfigMap.java,v 1.4 2006/06/12 05:06:59 gillies Exp $
//

public class UniqueConfigMap extends ConfigMap {
    //private static final Logger LOG = LogUtil.getLogger(UniqueConfigMap.class);

    /**
     * A <tt>UniqueConfigMap</tt> is the most used <tt>ConfigMap</tt> used by the views in eObslog. This class
     * uses the <tt><@link EObslogVisit></tt> that is transfered from the database to fill in the values of the
     * <tt>ConfigMap</tt> used by the view.
     * @param evisit an instance of <tt>EObslogVisit</tt>
     * @param instConfiguration
     */
    public UniqueConfigMap(EObslogVisit evisit, List<OlLogItem> instConfiguration) {
        if (evisit == null) throw new NullPointerException();

        UniqueConfig uc = evisit.getUniqueConfig();
        // Add all the items in the instrument
        Config config = uc.getConfig();
        for (int i = 0, size = instConfiguration.size(); i < size; i++) {
            OlLogItem item = instConfiguration.get(i);
            String key = item.getProperty();

            String strVal;
            // Note that these checks for types are not yet working since values in ObsRecord are already
            // Strings.  This will change, so I'm leaving this for now.
            Object val = config.getItemValue(new ItemKey(item.getSequenceName()));
            if (val instanceof LoggableSpType) {
                strVal = ((LoggableSpType) val).logValue();
            } else if (val instanceof DisplayableSpType) {
                strVal = ((DisplayableSpType) val).displayValue();
            } else if (val == null) {
                // Upper layers are currently assuming null is returned if a value is not present
                strVal = null;
            } else {
                strVal = val.toString();
            }
            put(key, strVal);
        }

        ConfigMapUtil.addStartTime(evisit.getDatasetRecords().get(0), this);
        ConfigMapUtil.addDatasetIDs(evisit.getDatasetRecords(), this);
        ConfigMapUtil.addDatasetQA(evisit.getDatasetRecords().get(0), this);
        ConfigMapUtil.addFileInfo(evisit.getDatasetRecords(), this);
        ConfigMapUtil.addComments(evisit.getDatasetRecords(), this);
        ConfigMapUtil.addCommentRowCount(this);
        ConfigMapUtil.addDatasetComments(evisit.getDatasetRecords(), this);
    }

}
