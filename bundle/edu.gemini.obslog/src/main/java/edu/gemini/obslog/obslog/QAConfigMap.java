package edu.gemini.obslog.obslog;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obsrecord.UniqueConfig;

import java.util.List;
import java.util.ArrayList;

/**
 * Gemini Observatory/AURA
 * $Id: QAConfigMap.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
 */
public class QAConfigMap extends ConfigMap {
      //private static final Logger LOG = LogUtil.getLogger(QAConfigMap.class);

    /**
     * A <tt>UniqueConfigMap</tt> is the most used <tt>ConfigMap</tt> used by the views in eObslog. This class
     * uses the <tt><@link EObslogVisit></tt> that is transfered from the database to fill in the values of the
     * <tt>ConfigMap</tt> used by the view.
     * @param uc is an instance of <tt>UniqueConfig</tt>
     * @param instConfiguration
     */
    public QAConfigMap(UniqueConfig uc, DatasetRecord dset, List<OlLogItem> instConfiguration) {
        if (uc == null) throw new NullPointerException();

        // Only need the classin the uc
        Config config = uc.getConfig();
        for (int i = 0, size = instConfiguration.size(); i < size; i++) {
            OlLogItem item = instConfiguration.get(i);
            String key = item.getProperty();
            String value = (String) config.getItemValue(new ItemKey(item.getSequenceName()));
            put(key, value);
        }

        ConfigMapUtil.addStartTime(dset, this);
        ConfigMapUtil.addDatasetID(dset, this);
        ConfigMapUtil.addDatasetQA(dset, this);
        ConfigMapUtil.addOneFile(dset, this);
        List<DatasetRecord> dsets = new ArrayList<DatasetRecord>();
        dsets.add(dset);
        ConfigMapUtil.addComments(dsets, this);
    }
}
