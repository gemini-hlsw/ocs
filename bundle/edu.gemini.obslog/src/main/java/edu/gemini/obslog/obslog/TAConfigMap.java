package edu.gemini.obslog.obslog;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.transfer.EChargeObslogVisit;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.dataset.DatasetRecord;

import java.util.List;

/**
 * Gemini Observatory/AURA
 * $Id: TAConfigMap.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
 */
public class TAConfigMap extends ConfigMap {
      //private static final Logger LOG = LogUtil.getLogger(TAConfigMap.class);

    /**
     * A <tt>TAConfigMap</tt> is used by the views in eObslog that display Time Accounting. This class
     * uses the <tt><@link EObslogVisit></tt> that is transfered from the database to fill in the values of the
     * <tt>ConfigMap</tt> used by the view.
     * @param  evisit is an instance of <tt>EChargeObsLogVisit</tt>
     * @param instConfiguration
     */
    public TAConfigMap(EChargeObslogVisit evisit, List<OlLogItem> instConfiguration) {
        if (evisit == null) throw new NullPointerException();

        // Only need the classin the uc
        Config config = evisit.getUniqueConfig().getConfig();
        for (int i = 0, size = instConfiguration.size(); i < size; i++) {
            OlLogItem item = instConfiguration.get(i);
            String key = item.getProperty();
            String value = (String) config.getItemValue(new ItemKey(item.getSequenceName()));
            put(key, value);
        }

        List<DatasetRecord> dsets  = evisit.getDatasetRecords();
        ConfigMapUtil.addConfigStartTime(evisit.getUniqueConfig(), this);
        ConfigMapUtil.addDatasetIDs(dsets, this);
        ConfigMapUtil.addFileInfo(dsets, this);
        ConfigMapUtil.addComments(dsets, this);

        ConfigMapUtil.addChargeTimeInfo(evisit, this);
    }
}
