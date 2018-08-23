package edu.gemini.qpt.shared.sp;

import edu.gemini.spModel.ictd.CustomMaskKey;
import edu.gemini.ictd.IctdDatabase;
import edu.gemini.shared.util.immutable.ImEither;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Left;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Right;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.ictd.Availability;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import edu.gemini.qpt.shared.util.EnumPio;
import edu.gemini.qpt.shared.util.PioSerializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.Serializable;

/**
 * Collection of ICTD data relevant to the QPT and QVis.
 */
public final class Ictd implements Serializable, PioSerializable {
    private static final Logger LOGGER = Logger.getLogger(Ictd.class.getName());

    /** ICTD Database configuration for the two sites. */
    public static final class SiteConfig {
        public final IctdDatabase.Configuration gn;
        public final IctdDatabase.Configuration gs;

        public SiteConfig(IctdDatabase.Configuration gn, IctdDatabase.Configuration gs) {
            assert gn != null;
            assert gs != null;
            this.gn = gn;
            this.gs = gs;
        }

        public IctdDatabase.Configuration configFor(final Site s) {
            final IctdDatabase.Configuration c;
            switch (s) {
                case GN: c = gn; break;
                case GS: c = gs; break;
                default: throw new RuntimeException("Unexpected site: " + s);
            }
            return c;
        }

        public static final SiteConfig forTesting =
                new SiteConfig(
                        IctdDatabase.asJava().testConfiguration(),
                        IctdDatabase.asJava().testConfiguration());
    }

    /**
     * Constructs an ICTD instance from query results, if possible. Returns a
     * Right<String, Ictd> if so, and a Left<String, Ictd> if not.
     */
    public static ImEither<String, Ictd> query(SiteConfig dbs, Site s) {
        final IctdDatabase.Configuration c = dbs.configFor(s);

        try {
            return new Right<>(new Ictd(
                IctdDatabase.asJava().unsafeSelectFeatureAvailability(c, s),
                IctdDatabase.asJava().unsafeSelectMaskAvailability(c, s)
            ));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not query ICTD", ex);
            return new Left<>("Error querying the ICTD, sorry.");
        }

    }

    public static final String FEATURE = "feature";
    public static final String MASK    = "mask";
    public static final String ENTRY   = "entry";
    public static final String AVAIL   = "avail";
    public static final String NAME    = "name";


    public final Map<Enum<?>,       Availability> featureAvailability;
    public final Map<CustomMaskKey, Availability> maskAvailability;

    public Ictd(
        Map<Enum<?>,       Availability> featureAvailability,
        Map<CustomMaskKey, Availability> maskAvailability
    ) {
        this.featureAvailability = Collections.unmodifiableMap(new HashMap<>(featureAvailability));
        this.maskAvailability    = Collections.unmodifiableMap(new HashMap<>(maskAvailability));
    }

    public Ictd(ParamSet params) {
        final ParamSet feature = params.getParamSet(FEATURE);
        final Map<Enum<?>, Availability> f = new HashMap<>();
        for (ParamSet entry : feature.getParamSets(ENTRY)) {
            final Enum<?>      e = EnumPio.getEnum(entry);
            final Availability a = Pio.getEnumValue(entry, AVAIL, Availability.Missing);
            f.put(e, a);
        }

        final ParamSet mask = params.getParamSet(MASK);
        final Map<CustomMaskKey, Availability> m = new HashMap<>();
        for (ParamSet entry : mask.getParamSets(ENTRY)) {

            // Mask Key
            final Option<CustomMaskKey> ok =
                ImOption.apply(Pio.getValue(entry, NAME))
                        .flatMap(s -> ImOption.fromScalaOpt(CustomMaskKey.parse(s)));

            if (ok.isEmpty()) {
                LOGGER.warning("Missing or unparseable mask key: " + Pio.getValue(entry, NAME));
            }

            // Availability
            final Availability a = Pio.getEnumValue(entry, AVAIL, Availability.Missing);

            // Add an entry to the map
            ok.foreach(k -> m.put(k, a));
        }

        this.featureAvailability = Collections.unmodifiableMap(f);
        this.maskAvailability    = Collections.unmodifiableMap(m);
    }

    public ParamSet getParamSet(PioFactory factory, String name) {
        final ParamSet params  = factory.createParamSet(name);

        final ParamSet feature = factory.createParamSet(FEATURE);
        for (Map.Entry<Enum<?>, Availability> me: featureAvailability.entrySet()) {
            final ParamSet e = EnumPio.getEnumParamSet(factory, ENTRY, me.getKey());
            Pio.addEnumParam(factory, e, AVAIL, me.getValue());
            feature.addParamSet(e);
        }
        params.addParamSet(feature);

        final ParamSet mask = factory.createParamSet(MASK);
        for (Map.Entry<CustomMaskKey, Availability> me: maskAvailability.entrySet()) {
            final ParamSet e = factory.createParamSet(ENTRY);
            Pio.addParam    (factory, e, NAME,  me.getKey().format());
            Pio.addEnumParam(factory, e, AVAIL, me.getValue());
            mask.addParamSet(e);
        }
        params.addParamSet(mask);

        return params;
    }

}
