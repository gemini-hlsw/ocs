//
// $Id: ConfigBridge.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.config.map.ConfigValMap;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.obsComp.GuideSequence;

import java.util.Collection;
import java.util.Map;

/**
 * This class is used to bridge between the old style configuration builder
 * classes and the new configuration model.  It contains a method which takes
 * an observation and extracts a {@link ConfigSequence} from it.
 */
public final class ConfigBridge {
    private static void _addItems(ItemKey rootKey, Config config, Collection params) {
        for (Object param1 : params) {
            final IParameter param = (IParameter) param1;
            final String paramName = param.getName();

            final Object val = param.getValue();
            if (val == null) continue;

            if (val instanceof ISysConfig) {
                // Some params are more complex and have values that are
                // actually system confs themselves.
                final ISysConfig tmp = (ISysConfig) val;
                final ItemKey newRoot = new ItemKey(rootKey, tmp.getSystemName());
                _addItems(newRoot, config, tmp.getParameters());
            } else {
                config.putItem(new ItemKey(rootKey, paramName), val);
            }
        }
    }

    public static ConfigSequence extractSequence(ISPObservation obs, Map options, ConfigValMap map) {
        return extractSequence(obs, options, map, false);
    }

    public static ConfigSequence extractSequence(ISPObservation obs, Map options, ConfigValMap map, boolean filterMeta) {
       return mapSequence(calculateSequence(obs, options, filterMeta), map);
    }

    private static ConfigSequence mapSequence(ConfigSequence sequence, ConfigValMap map) {
        if (map == ConfigValMapInstances.IDENTITY_MAP) return sequence;
        else {
            // Map the final config values as requested.
            final Config[] steps = sequence.getAllSteps();
            for (Config step : steps) {
                final ItemKey[] keys = step.getKeys();
                for (ItemKey key : keys) step.putItem(key, map.apply(step.getItemValue(key)));
            }
            return new ConfigSequence(steps);
        }
    }

    private static ConfigSequence calculateSequence(ISPObservation obs, Map options, boolean filterMeta) {
        ConfigSequence configSeq = new ConfigSequence();

        // make sure that important default options are added (e.g. smartgcal)
        options = ObservationCB.getDefaultSequenceOptions(options);

        // Get the config builder.
        IConfigBuilder cb;
        cb = (IConfigBuilder) obs.getClientData(IConfigBuilder.USER_OBJ_KEY);
        cb.reset(options);

        IConfig full = new DefaultConfig();

        // TODO: the cache concept is an abomination that will need to be
        // removed.  It's one of 3 or 4 different ways of working around the
        // fundamental problem with the step-by-step config building code that
        // only has available the changes from the previous step and yet needs
        // the context of all the current values.  The "full" config is another
        // mechanism for dealing with this as is the instrument elapsed time
        // handback.  All of that crap needs to go away in favor of simply
        // post-processing time accounting after the sequence is built.  That
        // is, until the sequence building code can be replaced altogether to
        // no longer use this iterator mechanism and instead store all the
        // steps and not the iterators.  Until then, we've got to support all
        // the old mechanisms.  Without further ado, I give you the sysconfig
        // cache ...
        ISysConfig cache = new DefaultSysConfig("");

        // Run it through, adding each step to the new ConfigSequence.
        while (cb.hasNext()) {
            // Create a new Config for the next step.
            Config config = new edu.gemini.spModel.config2.DefaultConfig();

            // Get the next step.
            IConfig oldConfig = new CachedConfig(cache); // new DefaultConfig(); (see TODO above)
            cb.applyNext(oldConfig, full);
            full.mergeSysConfigs(oldConfig);

            // Add all the configuration items for this step from the config
            // builder.
            for (Object o : oldConfig.getSysConfigs()) {
                ISysConfig sysConfig = (ISysConfig) o;
                if (filterMeta && sysConfig.isMetadata()) continue;

                String sysName = sysConfig.getSystemName();
                ItemKey sysKey = new ItemKey(sysName);
                _addItems(sysKey, config, sysConfig.getParameters());
            }
            configSeq.addStep(config);
        }

        // Post-process the configuration if necessary.
        for (ISPObsComponent obsComp : obs.getObsComponents()) {
            Object dobj = obsComp.getDataObject();
            if (dobj instanceof ConfigPostProcessor) {
                configSeq = ((ConfigPostProcessor) dobj).postProcessSequence(configSeq);
            }
        }

        // Post-process to fix guiding for offset positions as required by the
        // seqexec. :-(
        return (new GuideSequence(ObsContext.create(obs))).postProcessSequence(configSeq);
    }
}
