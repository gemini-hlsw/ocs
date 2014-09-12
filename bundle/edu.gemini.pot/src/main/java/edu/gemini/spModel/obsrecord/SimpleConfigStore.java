//
// $Id: SimpleConfigStore.java 15646 2008-11-25 20:28:34Z swalker $
//

package edu.gemini.spModel.obsrecord;

import edu.gemini.spModel.config2.*;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioParseException;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.text.ParseException;
import java.util.*;



//
// The design goal here is to store as few Configs as possible.  So there is
// a HashMap that maps from Config to ConfigAndLabels.  The Config object
// used as the key to this HashMap is also referenced in the ConfigAndLabels
// object value.  So if 50 datasets have the same Config, they will all be
// represented in this single ConfigAndLabels object, even if they are not in
// the same "UniqueConfig" because they were distinct intervening Configs.
//



/**
 * An implementation class that keeps the mapping to/from dataset labels and
 * their configs.
 */
final class SimpleConfigStore implements Serializable, ConfigStore {
    public static final Logger LOG = Logger.getLogger(SimpleConfigStore.class.getName());

    private static final ItemKey OBS_CLASS_KEY = new ItemKey("observe:class");
/*
    <paramset name="configMap">
        <paramset name="configMapEntry">
            <param name="datasetLabels">
                <value sequence="0">GS-2005B-Q-23-1-001</value>
                <value sequence="0">GS-2005B-Q-23-1-002</value>
            </param>
            <paramset name="config">
                <paramset name="item">
                   <param name="key"><value>ocs:someprop</value></param>
                   <param name="value"><value>1</value></param>
                </paramset>
            </paramset>
        </paramset>
    </paramset>
*/

    static final String CONFIG_MAP_PARAM_SET        = "configMap";
    static final String CONFIG_MAP_ENTRY_PARAM_SET  = "configMapEntry";
    static final String DATASET_LABELS_PARAM        = "datasetLabels";

    /**
     * Stores an unmodifiable Config and the dataset labels that were created
     * using this Config.
     */
    private static class ConfigAndLabels implements Serializable {
        final Config config;
        final Set<DatasetLabel> labels;

        ConfigAndLabels(Config config) {
            this.config = Configs.unmodifiableConfig(config);
            labels = new TreeSet<DatasetLabel>();
        }

        ConfigAndLabels(ConfigAndLabels that) {
            this.config = that.config;  // unmodifiable
            labels = new TreeSet<DatasetLabel>(that.labels);
        }

        void addLabel(DatasetLabel label) {
            labels.add(label);
        }
    }

    // Map from Config to ConfigAndLabels. Once a Config is stored, it is never
    // changed.  It represents the configuration at the time the dataset was
    // collected.
    private Map<Config, ConfigAndLabels> _config2LabelMap = new HashMap<Config, ConfigAndLabels>();


    // Map from DatasetLabel to Config that produced the dataset.  This is
    // for convenience/lookup speed and is generated on demand.
    private transient TreeMap<DatasetLabel, Config> _label2ConfigMap;


    SimpleConfigStore() {
    }

    SimpleConfigStore(SimpleConfigStore that) {
        for (ConfigAndLabels cal : that._config2LabelMap.values()) {
            cal = new ConfigAndLabels(cal);
            _config2LabelMap.put(cal.config, cal);
        }
    }

    SimpleConfigStore(ParamSet paramSet) throws PioParseException {
        List<ParamSet> configMapParamSets = paramSet.getParamSets(CONFIG_MAP_ENTRY_PARAM_SET);

        for (ParamSet configMapParamSet : configMapParamSets) {

            // Get the dataset labels.
            List<DatasetLabel> datasetLabels = Collections.emptyList();
            Param datasetLabelsParam = configMapParamSet.getParam(DATASET_LABELS_PARAM);
            if (datasetLabelsParam != null) {
                datasetLabels = _getDatasetLabels(datasetLabelsParam);
            }

            // Get the configuration.
            ParamSet configParamSet = configMapParamSet.getParamSet(ConfigPio.CONFIG_PARAM_SET);
            if (configParamSet == null) {
                throw new PioParseException("missing '" + ConfigPio.CONFIG_PARAM_SET + "'");
            }

            // Add the ConfigAndLabels
            Config c = ConfigPio.toConfig(configParamSet);
            ConfigAndLabels cal = new ConfigAndLabels(c);
            _config2LabelMap.put(cal.config, cal);
            cal.labels.addAll(datasetLabels);
        }
    }

    private static List<DatasetLabel> _getDatasetLabels(Param p) throws PioParseException {
        List<DatasetLabel> res = new ArrayList<DatasetLabel>();

        List<String> values = p.getValues();
        for (String labelStr : values) {

            DatasetLabel label;
            try {
                label = new DatasetLabel(labelStr);
            } catch (ParseException ex) {
                throw new PioParseException("illegal dataset label '" + labelStr + "'");
            }
            res.add(label);
        }
        return res;
    }

    private static final void sortConfigAndLabelList(List<ConfigAndLabels> calList) {
        // Sort by first dataset label.  Label sets should be disjoint anyway.
        Collections.sort(calList, new Comparator<ConfigAndLabels>() {
            @Override public int compare(ConfigAndLabels o1, ConfigAndLabels o2) {
                final Iterator<DatasetLabel> it1 = o1.labels.iterator();
                final Iterator<DatasetLabel> it2 = o2.labels.iterator();
                if (!it1.hasNext()) {
                    return it2.hasNext() ? -1 : 0;
                } else if (!it2.hasNext()) {
                    return 1;
                } else {
                    final DatasetLabel lab1 = it1.next();
                    final DatasetLabel lab2 = it2.next();
                    return lab1.compareTo(lab2);
                }
            }
        });
    }

    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(CONFIG_MAP_PARAM_SET);

        // Sort them by the first label so that they are always written in the
        // same order, which makes it easier to diff two XML files.
        final List<ConfigAndLabels> calList = new ArrayList<ConfigAndLabels>(_config2LabelMap.values());
        sortConfigAndLabelList(calList);
        for (ConfigAndLabels cal : calList) {
            paramSet.addParamSet(_toParamSet(factory, cal));
        }
        return paramSet;
    }

    private static ParamSet _toParamSet(PioFactory factory, ConfigAndLabels cal) {
        ParamSet paramSet = factory.createParamSet(CONFIG_MAP_ENTRY_PARAM_SET);

        Param datalabelsParam = factory.createParam(DATASET_LABELS_PARAM);
        for (DatasetLabel label : cal.labels) {
            datalabelsParam.addValue(label.toString());
        }
        paramSet.addParam(datalabelsParam);

        paramSet.addParamSet(ConfigPio.toParamSet(factory, cal.config));
        return paramSet;
    }


    public void addConfigAndLabel(Config config, DatasetLabel label) {
        // Copy the config and remove items which should not be considered
        // in comparing configs to determine uniqueness.
        config = new DefaultConfig(config);
        UniqueConfig.removeNonEssentialItems(config);

        // Get (or create) the ConfigAndLabels object.
        ConfigAndLabels cal = _config2LabelMap.get(config);
        if (cal == null) {
            cal = new ConfigAndLabels(config);
            _config2LabelMap.put(cal.config, cal);
        }
        // Add the label.
        cal.addLabel(label);
        if (_label2ConfigMap != null) {
            _label2ConfigMap.put(label, cal.config);
        }
    }

    public void remove(DatasetLabel label) {
        // Make sure this label isn't already represented.
        Config c = getConfigForDataset(label);
        if (c != null) {
            // Cleanup the old dataset and config mapping.
            ConfigAndLabels existing = _config2LabelMap.get(c);
            existing.labels.remove(label);
            if (existing.labels.size() == 0) {
                _config2LabelMap.remove(c);
            }
            _label2ConfigMap.remove(label);
        }
    }

    private void _buildLabel2ConfigMap() {
        if (_label2ConfigMap != null) return; // already initialized

        _label2ConfigMap = new TreeMap<DatasetLabel, Config>();
        for (ConfigAndLabels cal : _config2LabelMap.values()) {
            for (DatasetLabel label : cal.labels) {
                _label2ConfigMap.put(label, cal.config);
            }
        }
    }

    /**
     * Gets the (unmodifiable) config that was in effect when the dataset with
     * the given <code>label</code> was started.
     *
     * @param label dataset label for the dataset whose configuration should be
     * returned
     *
     * @return Config that was used to configure the telescope at the time that
     * the dataset with the given <code>label</code> was created;
     * <code>null</code> if there is no configuration information
     */
    public Config getConfigForDataset(DatasetLabel label) {
        _buildLabel2ConfigMap();
        return _label2ConfigMap.get(label); // unmodifiable config
    }

    public boolean containsDataset(DatasetLabel label) {
        _buildLabel2ConfigMap();
        return _label2ConfigMap.containsKey(label);
    }

    /**
     * Gets the ObsClass associated with the given dataset.
     */
    public ObsClass getObsClass(DatasetLabel label) {
        Config c = getConfigForDataset(label);
        if (c == null) {
            LOG.log(Level.WARNING, "Could not find Config for dataset: "+ label);
            return ObsClass.SCIENCE;
        }

        String obsClassStr = (String) c.getItemValue(OBS_CLASS_KEY);
        if (obsClassStr == null) {
            LOG.log(Level.WARNING, "Could not find ObsClass using key: "+ OBS_CLASS_KEY);
            return ObsClass.SCIENCE;
        }

        ObsClass res = ObsClass.parseType(obsClassStr);
        if (res == null) {
            LOG.log(Level.WARNING, "Unknown ObsClass: " + obsClassStr);
            return ObsClass.SCIENCE;
        }
        return res;
    }
}
