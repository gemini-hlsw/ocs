//
// $
//

package jsky.catalog.skycat;

import edu.gemini.catalog.skycat.table.SkyObjectFactory;
import edu.gemini.shared.util.immutable.*;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Registrar for {@link SkyObjectFactory} instances.  In an OSGi environment,
 * this would be handled by making each bundle provide a service which could
 * be tracked. In the Observing Tool ghetto, we will be relying upon the
 * skycat config file to configure it.
 */
public enum SkyObjectFactoryRegistrar {
    instance;
    private static final Logger LOG = Logger.getLogger(SkyObjectFactoryRegistrar.class.getName());

    /**
     * Name of the skycat config file key.  The key identifies the class
     * containing a SkyObjectFactory singleton, which is then obtained using
     * reflection.  The singleton must be implemented as an enum with one entry.
     */
    public static final String SKYOBJ_FACTORY = "skyobj_factory";

    /**
     * Each SkyObjectFactory is a singleton implemented as a single enum
     * instance.  This instantiator is used to turn the class name into an
     * factory instance.
     */
    private static final SkycatCatalogObjectRegistrar.Instantiator<SkyObjectFactory> INST =
        className -> {
            if (className == null) return None.instance();

            try {
                Class<?> c = Class.forName(className);
                Object[] enums = c.getEnumConstants();
                if ((enums != null) && (enums.length > 0)) {
                    //noinspection unchecked
                    return new Some<>((SkyObjectFactory) enums[0]);
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Problem extracting object from " + className, ex);
            }
            return None.instance();

        };

    private static final SkycatCatalogObjectRegistrar<SkyObjectFactory> reg =
            new SkycatCatalogObjectRegistrar<>(SKYOBJ_FACTORY, INST);

    /**
     * Finds the SkyObjectFactory associated with the given id, if any.
     *
     * @param id short name of the catalog server as specified in the
     * <code>skycat.cfg</code> file
     *
     * @return the associated {@link SkyObjectFactory} wrapped in a {@link Some}
     * instance; {@link None} if there is no associated factory
     */
    public Option<SkyObjectFactory> lookup(String id) {
        return reg.lookup(id);
    }

    /**
     * @return a list of tuples(key, factory) for each known factory
     */
    public List<Tuple2<String,SkyObjectFactory>> allFactories() {
        return reg.allFactories();
    }
}
