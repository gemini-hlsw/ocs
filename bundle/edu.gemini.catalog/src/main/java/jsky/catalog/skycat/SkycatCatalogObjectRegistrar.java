package jsky.catalog.skycat;

import edu.gemini.shared.util.immutable.*;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogRegistry;

import java.util.*;

/**
 * Registrar for objects associated with class names in the skycat config file.
 * In an OSGi environment, this would be handled by making each bundle provide
 * a service which could be tracked. In the Observing Tool ghetto, we will be
 * relying upon the skycat config file to configure it.
 */
public final class SkycatCatalogObjectRegistrar<T> {

    /**
     * Encapsulates the mechanism used for turning a class name into an object.
     * @param <T> type of object that is created by the instantiator
     */
    public interface Instantiator<T> {
        Option<T> instantiate(String className);
    }

    // Name of the config file key.
    private final Map<String, T> factoryMap;

    /**
     * Constructs with the name of the key that appears in the config file.
     *
     * @param configKey key in the <code>skycat.cfg</code> file that identifies
     * the objects held in this registrar
     */
    public SkycatCatalogObjectRegistrar(String configKey, Instantiator<T> inst) {
        Map<String, T> map = new HashMap<>();

        for (Catalog c : CatalogRegistry.instance) {
            if (!(c instanceof SkycatCatalog)) continue;
            SkycatCatalog cat = (SkycatCatalog) c;
            SkycatConfigEntry entry = cat.getConfigEntry();

            String className = entry.getProperty(configKey);
            Option<T> factOpt = inst.instantiate(className);
            if (factOpt.isEmpty()) continue;

            map.put(entry.getShortName(), factOpt.getValue());
        }

        factoryMap = Collections.unmodifiableMap(map);
    }

    /**
     * Finds the object associated with the given id, if any.
     *
     * @param id short name of the catalog server as specified in the
     * <code>skycat.cfg</code> file
     *
     * @return the associated object wrapped in a
     * {@link edu.gemini.shared.util.immutable.Some} instance;
     * {@link edu.gemini.shared.util.immutable.None} if there is no associated
     * factory
     */
    public Option<T> lookup(String id) {
        T res = factoryMap.get(id);
        //noinspection unchecked
        return (res == null) ? None.INSTANCE : new Some<T>(res);
    }

}
