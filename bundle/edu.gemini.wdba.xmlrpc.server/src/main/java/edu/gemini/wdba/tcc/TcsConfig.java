package edu.gemini.wdba.tcc;

import edu.gemini.spModel.core.HorizonsDesignation;
import edu.gemini.spModel.core.NonSiderealTarget;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TcsConfig extends ParamSet {

    private static final Logger LOG = Logger.getLogger(TcsConfig.class.getName());

    private ObservationEnvironment _oe;

    public TcsConfig(ObservationEnvironment oe) {
        super("");
        if (oe == null) throw new NullPointerException("Config requires a non-null observation environment");
        // First add the special parameter pointing to the base
        addAttribute(NAME, oe.getObservationTitle());
        addAttribute(TYPE, TccNames.TCS_CONFIGURATION);
        _oe = oe;
    }

    /**
     * Returns the name of the ephemeris file associated with the given
     * horizons designation.
     */
    public static String ephemerisFile(final HorizonsDesignation hd) {
        try {
            // See TcsEphemerisExport.
            return URLEncoder.encode(hd.toString() + ".eph", StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            LOG.log(Level.SEVERE, "UTF-8 is not supported!", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * build will use the <code>(@link TargetEnv}</code> to construct
     * an XML document.
     */
    public boolean build() {
        putParameter(TccNames.FIELD, _oe.getBasePositionName());
        putParameter(TccNames.SLEWOPTIONS, TccNames.NORMAL);
        ITccInstrumentSupport is = _oe.getInstrumentSupport();
        putParameter(TccNames.POINT_ORIG, is.getTccConfigInstrumentOrigin());
        putParameter(TccNames.INSTRUMENT, is.getTccConfigInstrument());
        String chopState = is.getChopState();
        if (chopState != null) {
            putParameter(TccNames.CHOP, chopState);
        }

        // Add a parameter that identifies the ephemeris file to look for if
        // this is a non-sidereal target.
        final scala.Option<NonSiderealTarget> nsOption = _oe.getTargetEnvironment().getBase().getNonSiderealTarget();
        if (nsOption.isDefined()) {
            final NonSiderealTarget ns = nsOption.get();
            final scala.Option<HorizonsDesignation> hdOption = ns.horizonsDesignation();
            if (hdOption.isDefined()) {
                putParameter(TccNames.EPHEMERIS, ephemerisFile(hdOption.get()));
            }
        }
        return true;
    }
}
