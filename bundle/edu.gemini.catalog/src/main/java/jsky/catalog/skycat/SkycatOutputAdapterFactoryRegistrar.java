//
// $
//

package jsky.catalog.skycat;

import edu.gemini.catalog.skycat.SkycatOutputAdapter;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registrar for {@link edu.gemini.shared.catalog.skycat.SkycatOutputAdapter.Factory}
 * instances.  In an OSGi environment, this would be handled by making each
 * bundle provide a service which could
 * be tracked. In the Observing Tool ghetto, we will be relying upon the
 * skycat config file to configure it.
 */
public enum SkycatOutputAdapterFactoryRegistrar {
    instance;
    private static final Logger LOG = Logger.getLogger(SkycatOutputAdapterFactoryRegistrar.class.getName());

    /**
     * Name of the skycat config file key.  The key identifies the class
     * containing a SkycatServerOutputAdapter, the factory for which is then
     * obtained using reflection.
     */
    public static final String OUTPUT_ADAPTER = "output_adapter";

    /**
     * Each SkyOcatOutputAdapterFactory is a singleton implemented as a static
     * final instance.
     */
    private static final SkycatCatalogObjectRegistrar.Instantiator<SkycatOutputAdapter.Factory> INST =
        new SkycatCatalogObjectRegistrar.Instantiator<SkycatOutputAdapter.Factory>() {

            // Uses reflection to turn the class name into an object.  This is
            // a bit grim, but the other options that occurred to me seemed worse:
            // * search through the classpath looking for instances
            // * explicitly register them by the short name that appears in the config
            //   file

            @Override
            public Option<SkycatOutputAdapter.Factory> instantiate(String className) {
                if (className == null) return None.instance();

                try {
                    Class c = Class.forName(className);
                    Field f = c.getField("FACTORY");
                    if (f == null) return None.instance();
                    SkycatOutputAdapter.Factory fact;
                    fact = (SkycatOutputAdapter.Factory) f.get(null);
                    if (fact == null) return None.instance();
                    Object[] enums = c.getEnumConstants();

                    return new Some<SkycatOutputAdapter.Factory>(fact);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Problem extracting object from " + className, ex);
                }
                return None.instance();

            }
        };

    private static final SkycatCatalogObjectRegistrar<SkycatOutputAdapter.Factory> reg =
            new SkycatCatalogObjectRegistrar<SkycatOutputAdapter.Factory>(OUTPUT_ADAPTER, INST);

    /**
     * Finds the SkycatOutputAdapter.Factory associated with the given id, if
     * any.
     *
     * @param id short name of the catalog server as specified in the
     * <code>skycat.cfg</code> file
     *
     * @return the associated {@link SkycatOutputAdapter.Factory} wrapped in a
     * {@link edu.gemini.shared.util.immutable.Some} instance;
     * {@link edu.gemini.shared.util.immutable.None} if there is no associated
     * factory
     */
    public Option<SkycatOutputAdapter.Factory> lookup(String id) {
        return reg.lookup(id);
    }

}
