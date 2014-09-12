package edu.gemini.spModel.config;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.config.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Support for managing meta data associated with sequence building.  Includes
 * the node key information in the sequence.
 */
public class MetaDataConfig {
    public static final String  NAME    = "metadata";
    public static final ItemKey MATCHER = new ItemKey(NAME + ":*");

    public static final String STEP_COUNT = "stepcount";
    public static final String COMPLETE   = "complete";
    public static final String SP_NODE    = "spnodekey";

    public static final ItemKey STEP_COUNT_KEY = new ItemKey(NAME + ":" + STEP_COUNT);
    public static final ItemKey COMPLETE_KEY   = new ItemKey(NAME + ":" + COMPLETE);
    public static final ItemKey SP_NODE_KEY    = new ItemKey(NAME + ":" + SP_NODE);

    /**
     * A ConfigSequence Predicate that matches configs that mention particular
     * node keys.  This is used to pick out Config steps for particular
     * sequence nodes.
     */
    public static final class NodeKeySequencePredicate implements ConfigSequence.Predicate {
        private final SPNodeKey nodeKey;

        public NodeKeySequencePredicate(SPNodeKey nodeKey) {
            this.nodeKey = nodeKey;
        }

        public boolean matches(Config c){
            return (nodeKey == null) || containsNodeKey(c);
        }

        private boolean containsNodeKey(Config c) {
            Object val = c.getItemValue(SP_NODE_KEY);
            return (val != null) && ((List<SPNodeKey>) val).contains(nodeKey);
        }
    }

    private final ISysConfig sys;

    private MetaDataConfig(ISysConfig sys) {
        this.sys = sys;
    }

    public Object getValue(String key, Object def) {
        IParameter p = sys.getParameter(key);
        if (p == null) return def;
        Object val =  p.getValue();
        return (val == null) ? def : val;
    }

    public MetaDataConfig setValue(String key, Object value) {
        sys.putParameter(DefaultParameter.getInstance(key, value));
        return this;
    }

    public int getStepCount() {
        return (Integer) getValue(STEP_COUNT, -1);
    }

    public void setStepCount(int count) {
        setValue(STEP_COUNT, count);
    }

    public boolean isComplete() {
        return (Boolean) getValue(COMPLETE, false);
    }

    public void setComplete(boolean isComplete) {
        setValue(COMPLETE, isComplete);
    }

    public List<SPNodeKey> getNodeKeys() {
        List<SPNodeKey> empty = Collections.emptyList();
        return (List<SPNodeKey>) getValue(SP_NODE, empty);
    }

    public void addNodeKey(SPNodeKey key) {
        List<SPNodeKey> newList = new ArrayList<SPNodeKey>(getNodeKeys());
        newList.add(key);
        setValue(SP_NODE, newList);
    }

    public static MetaDataConfig extract(IConfig conf) {
        ISysConfig sysConfig = conf.getSysConfig(NAME);
        if (sysConfig == null) {
            sysConfig = new DefaultSysConfig(NAME, true);
            conf.appendSysConfig(sysConfig);
        }
        return new MetaDataConfig(sysConfig);
    }

    public static void clear(IConfig config) {
        config.removeSysConfig(NAME);
    }
}
