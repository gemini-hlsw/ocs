package edu.gemini.spModel.obsseq;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.DefaultConfig;

import java.io.Serializable;
import java.util.*;

/**
 * The ObservingSequence is the top-level {@link Sequence} container.  It
 * includes a base "static" configuration along with its series of child
 * {@link Sequence} objects.  The primary purpose of the ObservingSequence is
 * to generate the proper {@link ConfigSequence} for an observation.  The
 * {@link ConfigSequence} is used to setup the data processing for the
 * observation as well as to actually execute the observation.
 *
 * <p>When the ConfigSequence is created via a call to
 * {@link #getConfigSequence()} or
 * {@link #getConfigSequence(edu.gemini.spModel.config2.Config)}, the
 * static configuration comes into play.  For each child {@link Sequence}, we
 * begin with a fresh static Config object.  We then step through the
 * configurations offered by the child Sequence, applying them to the static
 * configuration one by one creating new configuration steps to add to the
 * {@link ConfigSequence}.
 */
public class ObservingSequence implements SequenceContainer, Serializable {

    private Config _staticConfig;
    private List<Sequence> _sequences = new ArrayList<>();

    public Config getStaticConfig() {
        return _staticConfig;
    }

    public void setStaticConfig(Config staticConfig) {
        _staticConfig = staticConfig;
    }

    public Sequence[] getAllSequences() {
        return _sequences.toArray(Sequence.EMPTY_ARRAY);
    }

    public void setAllSequences(Sequence[] children) {
        _sequences.clear();
        Collections.addAll(_sequences, children);
    }

    public void addSequence(int index, Sequence child) {
        _sequences.add(index, child);
    }

    public void addSequence(Sequence child) {
        _sequences.add(child);
    }

    public void clearSequences() {
        _sequences.clear();
    }

    public Iterator<Sequence> sequenceIterator() {
        return _sequences.iterator();
    }

    public Sequence getSequence(int index) {
        return _sequences.get(index);
    }

    public boolean isEmptySequenceContainer() {
        return _sequences.isEmpty();
    }

    public Sequence removeSequence(int index) {
        return _sequences.remove(index);
    }

    public boolean removeSequence(Sequence child) {
        return _sequences.remove(child);
    }

    public int getSequenceCount() {
        return _sequences.size();
    }

    /**
     * Gets the number of configuration steps produced by this
     * ObservingSequence.  This is equal to the sum of the steps produced by
     * each child {@link Sequence}.
     */
    public int getStepCount() {
        int stepCount = 0;

        for (Iterator<Sequence> it=sequenceIterator(); it.hasNext(); ) {
            Sequence seq = it.next();
            stepCount += seq.getStepCount();
        }

        return Math.max(1, stepCount);
    }

    /**
     * Constructs the {@link ConfigSequence} produced by this ObservingSequence
     * as it is currently configured.  Exactly {@link #getStepCount}
     * configuration changes should be in the returned ConfigSequence.
     * See also {@link #getConfigSequence(edu.gemini.spModel.config2.Config)}
     * for an option in which base configuration information to be included
     * in the sequence can be specified.
     *
     * @return {@link ConfigSequence} produced by this ObservingSequence in its
     * current configuration
     */
    public ConfigSequence getConfigSequence() {
        return getConfigSequence(null);
    }

    /**
     * Constructs the {@link ConfigSequence} produced by this ObservingSequence
     * as it is currently configured.  Provides the opportunity to specify a
     * base configuration of items that should apply at the start of the
     * iteration of nested configuration change steps.  Exactly
     * {@link #getStepCount()} configuration changes should be in the returned
     * ConfigSequence.
     *
     * @param base the base set of items that should be applied before
     * iterating over the configuration changes specified by any child
     * {@link Sequence}s; may be <code>null</code> if there are no such
     * changes
     *
     * @return {@link ConfigSequence} produced by this ObservingSequence in its
     * current configuration
     */
    public ConfigSequence getConfigSequence(Config base) {
        List<Config> configList = new ArrayList<>();

        for (Sequence seq : _sequences) {
            ConfigMerger cm = seq.getConfigMerger();

            Config staticConf = getStaticConfig();

            Config curConf;
            if (staticConf == null) {
                curConf = new DefaultConfig();
            } else {
                curConf = new DefaultConfig(staticConf);
            }

            if (base != null) curConf.putAll(base);

            while (cm.hasNextConfig()) {
                cm.mergeNextConfig(curConf);
                configList.add(new DefaultConfig(curConf));
            }
        }

        Config[] res = configList.toArray(DefaultConfig.EMPTY_ARRAY);
        return new ConfigSequence(res);
    }
}
