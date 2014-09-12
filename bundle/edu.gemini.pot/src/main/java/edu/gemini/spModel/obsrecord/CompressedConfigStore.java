//
// $
//

package edu.gemini.spModel.obsrecord;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.util.CompressedString;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioParseException;
import edu.gemini.spModel.pio.xml.PioXmlException;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;

import java.io.Serializable;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ConfigStore implementation that stores a compressed version of the XML
 * describing the configuration.  This is done for space saving considerations.
 * When using just SimpleConfigStore, about 25% of a program's memory
 * requirements are taken up by the ConfigStore.  The information is rarely
 * used and so can be saved in a compressed format, reducing the memory
 * consumption for ConfigStores to 5% roughly.
 */
final class CompressedConfigStore implements Serializable, ConfigStore {
    private static final Logger LOG = Logger.getLogger(CompressedConfigStore.class.getName());

    // CacheObject is a container for a ConfigStore and a "version" number.
    // The version is just an MD5 of the compressed XML.  Versions are needed
    // because the cache information is always local to a process, but the
    // actual CompressedConfigStore objects are serialized and shipped to/from
    // the database.  So, in a client like the OT, the cache can be out of date
    // after reading an update from the database.
    private static final class CacheObject {
        byte[] version;
        ConfigStore store;

        CacheObject(ConfigStore store, byte[] version) {
            this.store = store;
            this.version = version;
        }

        boolean matches(byte[] version) {
            return Arrays.equals(this.version, version);
        }
    }

    // LRU cache of CacheObjects
    private static final class ConfigStoreCache extends LinkedHashMap<SPObservationID, CacheObject> {
        public static final int DEFAULT_SIZE = 1000;
        private final int cachesize;

        public ConfigStoreCache() {
            this(DEFAULT_SIZE);
        }

        public ConfigStoreCache(int cachesize) {
            super((int) Math.ceil((cachesize+1) / 0.75f), 0.75f, true);
            if (cachesize <= 0) throw new IllegalArgumentException("cachesize = " + cachesize);
            this.cachesize = cachesize;
        }

        public int getCacheSize() {
            return cachesize;
        }

        protected boolean removeEldestEntry(Map.Entry<SPObservationID, CacheObject> eldest) {
            return size() > cachesize;
        }
    }

    private static final ConfigStoreCache CACHE = new ConfigStoreCache();

    private final CompressedString store;

    CompressedConfigStore() {
        store = new CompressedString();
    }

    CompressedConfigStore(ConfigStore that) {
        if (that instanceof CompressedConfigStore) {
            store = new CompressedString(((CompressedConfigStore) that).store);
        } else {
            store = new CompressedString();
            synchronized (CACHE) {
                write(that.toParamSet(new PioXmlFactory()), null);
            }

        }
    }

    CompressedConfigStore(ParamSet paramSet) {
        store = new CompressedString();
        synchronized (CACHE) {
            write(paramSet, null);
        }
    }

    // Simply decompresses the config information and parses it.
    private ConfigStore read() {
        String xml = store.get();
        if ((xml == null) || "".equals(xml)) return new SimpleConfigStore();

        try {
            ParamSet pset = (ParamSet) PioXmlUtil.read(new StringReader(xml));
            return new SimpleConfigStore(pset);
        } catch (PioParseException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            return new SimpleConfigStore();
        } catch (PioXmlException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            return new SimpleConfigStore();
        }
    }

    // Decompresses, parses, and caches the config information.
    private ConfigStore read(DatasetLabel label) {
        byte[] version = store.md5();
        CacheObject res = CACHE.get(label.getObservationId());
        if ((res == null) || !res.matches(version)) {
            ConfigStore tmp = read();
            res = new CacheObject(tmp, version);
            CACHE.put(label.getObservationId(), res);
        }
        return res.store;
    }

    // Converts the config store to a compressed string, and updates the
    // cache version (if present).  The assumption is that the caller has
    // already updated the cached ConfigStore itself -- just the version needs
    // to be updated in the cache.
    private void write(ConfigStore store, DatasetLabel label) {
        write(store.toParamSet(new PioXmlFactory()), label);
    }

    private void write(ParamSet pset, DatasetLabel label) {
        try {
            String xml = PioXmlUtil.toXmlString(pset);
            store.set(xml);

            if (label != null) {
                CacheObject obj = CACHE.get(label.getObservationId());
                if (obj != null) obj.version = store.md5();
            }

        } catch (PioXmlException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            store.set(null);
        }
    }

    public ParamSet toParamSet(PioFactory factory) {
        synchronized (CACHE) {
            return read().toParamSet(factory);
        }
    }


    public void addConfigAndLabel(Config config, DatasetLabel label) {
        synchronized (CACHE) {
            ConfigStore store = read(label);
            store.addConfigAndLabel(config, label);
            write(store, label);
        }
    }

    public void remove(DatasetLabel label) {
        synchronized (CACHE) {
            ConfigStore store = read(label);
            store.remove(label);
            write(store, label);
        }
    }

    public Config getConfigForDataset(DatasetLabel label) {
        synchronized (CACHE) {
            return read(label).getConfigForDataset(label);
        }
    }

    public boolean containsDataset(DatasetLabel label) {
        synchronized (CACHE) {
            return read(label).containsDataset(label);
        }
    }

    public ObsClass getObsClass(DatasetLabel label) {
        synchronized (CACHE) {
            return read(label).getObsClass(label);
        }
    }
}
