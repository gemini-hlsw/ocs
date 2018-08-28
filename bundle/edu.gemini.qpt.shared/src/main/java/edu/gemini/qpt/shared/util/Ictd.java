package edu.gemini.qpt.shared.util;

import edu.gemini.spModel.ictd.CustomMaskKey;
import edu.gemini.spModel.ictd.IctdService;
import edu.gemini.spModel.ictd.IctdSummary;
import edu.gemini.shared.util.immutable.ImEither;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Left;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Right;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.ictd.Availability;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.util.security.auth.keychain.KeyChain;
import edu.gemini.util.trpc.client.TrpcClient$;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Utility for querying the ICTD and for converting to/from ParamSet.
 */
public final class Ictd {
    private static final Logger LOGGER = Logger.getLogger(Ictd.class.getName());

    private Ictd() {
        // defeat instantiation
    }

    /**
     * Constructs an ICTD instance from query results, if possible. Returns a
     * Right<String, Ictd> if so, and a Left<String, Ictd> if not.
     */
    public static ImEither<String, IctdSummary> query(KeyChain kc, Peer peer, Site s) {

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try {

            final ClassLoader classLoader = Ictd.class.getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);

            final IctdService service = TrpcClient$.MODULE$.apply(peer.host, peer.port).withKeyChain(kc).proxy(IctdService.class);
            return new Right<>(service.summary(peer.site));

        } catch (UndeclaredThrowableException ute) {
            LOGGER.log(Level.WARNING, "Could not query ICTD", ute);
            return new Left<>("Error querying the ICTD, sorry.");

        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

    }

    public static final String FEATURE = "feature";
    public static final String MASK    = "mask";
    public static final String ENTRY   = "entry";
    public static final String AVAIL   = "avail";
    public static final String NAME    = "name";

    public static IctdSummary decode(ParamSet params) {
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

        return IctdSummary.fromJava(f, m);
    }

    public static ParamSet encode(PioFactory factory, String name, IctdSummary summary) {
        final ParamSet params  = factory.createParamSet(name);

        final ParamSet feature = factory.createParamSet(FEATURE);
        for (Map.Entry<Enum<?>, Availability> me : summary.featureAvailabilityJava().entrySet()) {
            final ParamSet e = EnumPio.getEnumParamSet(factory, ENTRY, me.getKey());
            Pio.addEnumParam(factory, e, AVAIL, me.getValue());
            feature.addParamSet(e);
        }
        params.addParamSet(feature);

        final ParamSet mask = factory.createParamSet(MASK);
        for (Map.Entry<CustomMaskKey, Availability> me: summary.maskAvailabilityJava().entrySet()) {
            final ParamSet e = factory.createParamSet(ENTRY);
            Pio.addParam    (factory, e, NAME,  me.getKey().format());
            Pio.addEnumParam(factory, e, AVAIL, me.getValue());
            mask.addParamSet(e);
        }
        params.addParamSet(mask);

        return params;
    }

}
