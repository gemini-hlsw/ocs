//
// $Id: UniqueConfig.java 39571 2011-12-05 17:52:26Z swalker $
//

package edu.gemini.spModel.obsrecord;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.dataset.DatasetLabel;

import java.io.Serializable;

/**
 * A UniqueConfig is a {@link edu.gemini.spModel.config2.Config configuration}
 * and a collection of {@link DatasetLabel datasets labels}s for datasets that
 * were collected under that configuration.
 *
 * <p>This class is a value object and is therefore immutable.
 */
public final class UniqueConfig implements Serializable {

    public static final UniqueConfig[] EMPTY_ARRAY = new UniqueConfig[0];

    /**
     * Creates with the "shared" Config, but without any datasets.  This is
     * appropriate for configurations that don't produce any data, or as the
     * starting point for a UniqueConfig that is built up with the
     * {@link #extendWith(DatasetLabel)} method.
     *
     * <p>Note that the {@link #configsEqual} method will ignore any items in
     * this config whose keys are included in the {@link #IGNORE_KEYS} array.
     *
     * @param config configuration associated with this UniqueConfig
     * @param timestamp time to associate with this UniqueConfig; by
     * convention this is the time of the start of the first dataset that
     * was taken with this config
     */
    public static UniqueConfig create(Config config, long timestamp) {
        config = new DefaultConfig(config);
        removeNonEssentialItems(config);
        return new UniqueConfig(config, timestamp);
    }

    /**
     * Creates with the "shared" Config and a single dataset that was collected
     * with this config.  This is appropriate for configurations that produce
     * only a single dataset, or as the starting point for a UniqueConfig that
     * is built up with {@link #extendWith(DatasetLabel)} method.
     *
     * <p>Note that the {@link #configsEqual} method will ignore any items in
     * this config whose keys are included in the {@link #IGNORE_KEYS} array.
     *
     * @param config configuration associated with the given dataset
     * @param timestamp time to associate with this UniqueConfig; by
     * convention this is the time of the start of the first dataset that
     * was taken with this config
     * @param label the dataset label collected with this configuration; if
     * <code>null</code>, this method is equivalent to
     * {@link #UniqueConfig(edu.gemini.spModel.config2.Config, long)}.
     */
    public static UniqueConfig create(Config config, long timestamp, DatasetLabel label) {
        config = new DefaultConfig(config);
        removeNonEssentialItems(config);
        return new UniqueConfig(config, timestamp, label);
    }

    /**
     * Creates with the shared Config and the collection of datasets that
     * were created with this config.
     *
     * <p>Note that the {@link #configsEqual} method will ignore any items in
     * this config whose keys are included in the {@link #IGNORE_KEYS} array.
     *
     * @param config configuration associated with the given datasets
     * @param timestamp time to associate with this UniqueConfig; by
     * convention this is the time of the start of the first dataset that
     * was taken with this config
     * @param labels dataset labels of datasets which share the same
     * configuration (excepting any items whose keys are inclueded in
     * {@link #IGNORE_KEYS})
     */
    public static UniqueConfig create(Config config, long timestamp, DatasetLabel[] labels) {
        config = new DefaultConfig(config);
        removeNonEssentialItems(config);
        return new UniqueConfig(config, timestamp, labels);
    }

    /**
     * Keys that should be ignored when comparing two configurations for
     * equality.  In other words, these items don't distinguish two configs
     * that are otherwise identical.
     */
    public static ItemKey[] IGNORE_KEYS = new ItemKey[] {
        new ItemKey("metadata"),
        new ItemKey("observe:dataLabel"),
        new ItemKey("observe:elapsedTime"),
        new ItemKey("smartgcal"),
        new ItemKey("telescope"),
    };

    /**
     * Removes all Items from the given Config that should not be considered
     * when comparing configurations for uniqueness.  In other words, all
     * items whose keys are contained in {@link #IGNORE_KEYS}.
     *
     * @param config configuration that will be modified by removing
     * non-essential keys
     */
    public static void removeNonEssentialItems(Config config) {
        config.removeAll(IGNORE_KEYS);
    }

    private final Config _config;
    private final long _timestamp;
    private DatasetLabel[] _datasetLabels;

    /**
     * Creates with the "shared" Config, but without any datasets.  This is
     * appropriate for configurations that don't produce any data, or as the
     * starting point for a UniqueConfig that is built up with the
     * {@link #extendWith(DatasetLabel)} method.
     *
     * <p>Note that the {@link #configsEqual} method will ignore any items in
     * this config whose keys are included in the {@link #IGNORE_KEYS} array.
     *
     * @param config configuration associated with this UniqueConfig
     * @param timestamp time to associate with this UniqueConfig; by
     * convention this is the time of the start of the first dataset that
     * was taken with this config
     */
    UniqueConfig(Config config, long timestamp) {
        if (config == null) throw new NullPointerException();
        _config    = config;
        _timestamp = timestamp;
        _datasetLabels = DatasetLabel.EMPTY_ARRAY;
    }

    /**
     * Creates with the "shared" Config and a single dataset that was collected
     * with this config.  This is appropriate for configurations that produce
     * only a single dataset, or as the starting point for a UniqueConfig that
     * is built up with {@link #extendWith(DatasetLabel)} method.
     *
     * <p>Note that the {@link #configsEqual} method will ignore any items in
     * this config whose keys are included in the {@link #IGNORE_KEYS} array.
     *
     * @param config configuration associated with the given dataset
     * @param timestamp time to associate with this UniqueConfig; by
     * convention this is the time of the start of the first dataset that
     * was taken with this config
     * @param label the dataset label collected with this configuration; if
     * <code>null</code>, this method is equivalent to
     * {@link #UniqueConfig(edu.gemini.spModel.config2.Config, long)}.
     */
    UniqueConfig(Config config, long timestamp, DatasetLabel label) {
        this(config, timestamp);
        if (label != null) {
            _datasetLabels = new DatasetLabel[1];
            _datasetLabels[0] = label;
        }
    }

    /**
     * Creates with the shared Config and the collection of datasets that
     * were created with this config.
     *
     * <p>Note that the {@link #configsEqual} method will ignore any items in
     * this config whose keys are included in the {@link #IGNORE_KEYS} array.
     *
     * @param config configuration associated with the given datasets
     * @param timestamp time to associate with this UniqueConfig; by
     * convention this is the time of the start of the first dataset that
     * was taken with this config
     * @param labels dataset labels of datasets which share the same
     * configuration (excepting any items whose keys are inclueded in
     * {@link #IGNORE_KEYS})
     */
    UniqueConfig(Config config, long timestamp, DatasetLabel[] labels) {
        this(config, timestamp);
        if (labels != null) {
            _datasetLabels = new DatasetLabel[labels.length];
            System.arraycopy(labels, 0, _datasetLabels, 0, labels.length);
        }
    }

    private UniqueConfig(UniqueConfig conf, DatasetLabel label) {
        _config = conf._config; // share same reference since it isn't modified
        _timestamp = conf._timestamp;
        int len = conf._datasetLabels.length;
        _datasetLabels = new DatasetLabel[len + 1];
        System.arraycopy(conf._datasetLabels, 0, _datasetLabels, 0, len);
        _datasetLabels[len] = label;
    }

    /**
     * Creates a new UniqueConfig that shares the same underlying {@link Config}
     * information, but with an additional dataset.  UniqueConfig is
     * immutable, so a newly created UniqueConfig is returned by this method
     * leaving this instance unmodified.
     *
     * @param label the dataset by which this UniqueConfig should be
     * extended
     */
    public synchronized UniqueConfig extendWith(DatasetLabel label) {
        return new UniqueConfig(this, label);
    }

    /**
     * Gets the unique configuration shared by all the datasets.  The caller
     * is free to modify the returned Config object without impacting this
     * UniqueConfig instance.
     *
     * <p>The Config returned by this method will not include any item whose
     * key is included in {@link #IGNORE_KEYS}.
     *
     * @return configuration shared by all the datasets in this UniqueConfig
     * instance
     */
    public Config getConfig() {
        return new DefaultConfig(_config);
    }

    /**
     * Gets the time to associate with this UniqueConfig.  By convention this
     * is the time of the start of the first dataset that was taken with this
     * config. (Technically the time of the start dataset event for the first
     * dataset.)
     *
     * @return timestamp associated with this config
     */
    public long getConfigTime() {
        return _timestamp;
    }

    /**
     * Gets (a copy of) all the datasets that share the same {@link Config}.
     * The caller is free to modify the returned array without impacting this
     * UniqueConfig instance.
     *
     * @return dataset labels whose datasets share the same unique config
     */
    public synchronized DatasetLabel[] getDatasetLabels() {
        DatasetLabel[] res = new DatasetLabel[_datasetLabels.length];
        System.arraycopy(_datasetLabels, 0, res, 0, _datasetLabels.length);
        return res;
    }

    /**
     * Determines whether the given Config matches the Config maintained by this
     * instance.  In other words, whether the config is equivalent when items
     * whose keys are included in the {@link #IGNORE_KEYS} array are not taken
     * into account.
     *
     * @param config the configuration to match against the configuration
     * information contained in this instance
     *
     * @return <code>true</code> if <code>config</code> matches this
     * UniqueConfig; <code>false</code> otherwise
     */
    public boolean configsEqual(Config config) {
        config = new DefaultConfig(config);
        removeNonEssentialItems(config);
        return _config.equals(config);
    }
}
