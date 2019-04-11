package edu.gemini.ags.servlet.estimation;

import edu.gemini.ags.servlet.estimation.ToContextHelper;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.DDMMSS;
import edu.gemini.skycalc.HHMMSS;
import edu.gemini.shared.util.immutable.Option;

import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.*;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.inst.InstRegistry;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.michelle.MichelleParams;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.SkyBackground;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.WaterVapor;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.trecs.TReCSParams;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.telescope.PosAngleConstraint;
import edu.gemini.spModel.telescope.PosAngleConstraintAware;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.util.Collections;

/**
 * Implements a function, more or less: <code>HttpServletRequest => ObsContext</code>..
 * Really it is a partial function, but since this is Java we handle that with
 * an Exception which carries along with it an explanation of why we can't
 * make the ObsContext.  Doing the best we can without being able to declare
 * this:
 *
 * <code>HttpServletRequest => Either[String, ObsContext]</code>
 *
 * <p>
 * Example request:
 * http://localhost:12000/ags?ra=00:00:00&dec=00:00:00&&cc=CC50&iq=IQ20&sb=SB50&inst=GMOS&pac=NONE
 * </p>
 */
public enum ToContext {
    instance;

    private static ToContextHelper helper = new ToContextHelper();

    public static final class RequestException extends Exception {
        public RequestException(String message) {
            super(message);
        }
    }

    private interface ParseOp<T> {
        T apply(String s) throws Exception;
    }

    public static final String RA  = "ra";
    private static final ParseOp<Angle> RA_OP = s -> HHMMSS.parse(s.replace(',','.'));

    public static final String DEC = "dec";
    private static final ParseOp<Angle> DEC_OP = s -> DDMMSS.parse(s.replace(',','.'));

    public static final String TARGET_TYPE = "targetType";
    enum TargetType { sidereal, nonsidereal }
    private static final ParseOp<TargetType> TARGET_TYPE_OP = TargetType::valueOf;

    public static final String POS_ANGLE_CONSTRAINT = "pac";
    private static final ParseOp<PosAngleConstraint> POS_ANGLE_CONSTRAINT_OP = s -> {
        // We previously had value UNKNOWN, but eliminated it since it was superseded by UNBOUNDED.
        if (s.equals("UNKNOWN"))
            return PosAngleConstraint.UNBOUNDED;
        return PosAngleConstraint.valueOf(s);
    };

    public static final String CC = "cc";
    private static final ParseOp<Option<CloudCover>> CC_OP = CloudCover::read;

    public static final String IQ = "iq";
    private static final ParseOp<Option<ImageQuality>> IQ_OP = ImageQuality::read;

    public static final String SB = "sb";
    private static final ParseOp<Option<SkyBackground>> SB_OP = SkyBackground::read;

    public static final String INST = "inst";
    private static final ParseOp<Option<SPInstObsComp>> INST_OP = s -> {
        // There is no Speckle or Visitor in the spModel so we simulate it as Texes
        if ("Speckle".equalsIgnoreCase(s)) {
            return InstRegistry.instance.prototype("Texes");
        } else {
            return InstRegistry.instance.prototype(s);
        }
    };

    public static final String ALTAIR = "altair";
    private static final ParseOp<AltairParams.Mode> ALTAIR_OP = ToContext::modeFromAgsServletParameter;

    public static final String NIRI_CAMERA = "niriCamera";
    private static final ParseOp<Niri.Camera> NIRI_CAMERA_OP = Niri.Camera::valueOf;


    static String enc(HttpServletRequest req) {
        return req.getCharacterEncoding() == null ? "UTF-8" : req.getCharacterEncoding();
    }

    private <T> T parse(HttpServletRequest req, String param, ParseOp<T> op, T defaultVal) throws RequestException {
        String str = req.getParameter(param);

        try {
            if (str == null) return defaultVal;
            return op.apply(URLDecoder.decode(str, enc(req)));
        } catch (Exception ex) {
            throw new RequestException("Couldn't parse '" + param + "': " + str);
        }
    }

    private <T> T parse(HttpServletRequest req, String param, ParseOp<T> op) throws RequestException {
        T res = parse(req, param, op, null);
        if (res == null) throw new RequestException("Missing '" + param + "' parameter");
        return res;
    }

    private <T> T parseOption(HttpServletRequest req, String param, ParseOp<Option<T>> op) throws RequestException {
        Option<T> o = parse(req, param, op);
        if (o.isEmpty()) {
            throw new RequestException("Couldn't parse '" + param + "'");
        }
        return o.getValue();
    }

    private TargetEnvironment targetEnv(HttpServletRequest req, long when) throws RequestException {
        Angle ra  = parse(req, RA,  RA_OP);
        Angle dec = parse(req, DEC, DEC_OP);
        TargetType tt = parse(req, TARGET_TYPE, TARGET_TYPE_OP, TargetType.sidereal);

        double raDeg  = ra.toDegrees().getMagnitude();
        double decDeg = dec.toDegrees().getMagnitude();

        SPTarget t;
        switch (tt) {
            // Here we just need an SPTarget with a contained NonSiderealTarget. In the end, this
            // is just used to determine whether it is a non-sidereal target or not in order to
            // pick the right guider.
            case nonsidereal:
                t = new SPTarget(helper.nonSiderealWithSingleEphemerisElement(raDeg, decDeg, when));
                break;
            default:
                t = new SPTarget(raDeg, decDeg);
        }
        return TargetEnvironment.create(t);
    }

    private Conditions conds(HttpServletRequest req) throws RequestException {
        CloudCover    cc = parseOption(req, CC, CC_OP);
        ImageQuality  iq = parseOption(req, IQ, IQ_OP);
        SkyBackground sb = parseOption(req, SB, SB_OP);
        return new Conditions(cc, iq, sb, WaterVapor.ANY);
    }

    public ObsContext apply(HttpServletRequest req) throws RequestException {

        // Construct a scheduling block at the current time with zero length.
        SchedulingBlock sb      = SchedulingBlock.apply(System.currentTimeMillis());
        TargetEnvironment env   = targetEnv(req, sb.start());
        Conditions conds        = conds(req);
        SPInstObsComp inst      = parseOption(req, INST, INST_OP);
        final Option<Site> site = ObsContext.getSiteFromInstrument(inst);

        // --- instrument pos angle constraint aware?
        if (inst instanceof PosAngleConstraintAware) {
            PosAngleConstraint pac = parse(req, POS_ANGLE_CONSTRAINT, POS_ANGLE_CONSTRAINT_OP);
            ((PosAngleConstraintAware) inst).setPosAngleConstraint(pac);
        } // otherwise pac is ignored!

        // --- instrument configurable with GeMS?
        AbstractDataObject aoComp = null;
        if (inst instanceof Gsaoi) {
            aoComp = getGeMS(req);
        }
        // --- instrument configurable with Altair?
        else if (inst instanceof InstGNIRS ||
                 inst instanceof InstNIRI ||
                 inst instanceof InstNIFS ||
                 inst instanceof InstGmosNorth) {
            aoComp = getAltair(req);
        } // otherwise altair is ignored

        // --- instrument is NIRI?
        if (inst instanceof InstNIRI) {
            Niri.Camera camera = parse(req, NIRI_CAMERA, NIRI_CAMERA_OP, Niri.Camera.DEFAULT);
            ((InstNIRI) inst).setCamera(camera);
        } // otherwise niri camera is ignored

        // --- set chop mode by default for Michelle (impacts selected estimation strategy)
        if (inst instanceof InstMichelle) {
            ((InstMichelle) inst).setChopMode(new Some<>(MichelleParams.ChopMode.CHOP));
        }

        // --- set chop mode by default for TReCS (impacts selected estimation strategy)
        if (inst instanceof InstTReCS) {
            ((InstTReCS) inst).setObsMode(TReCSParams.ObsMode.CHOP);
        }

        final TargetObsComp toc = new TargetObsComp();
        toc.setTargetEnvironment(env);
        return ObsContext.create(env, inst, site, conds, Collections.emptySet(), aoComp, new Some<>(sb));
    }

    private Gems getGeMS(final HttpServletRequest req) throws RequestException {
        final Gems gems = new Gems();
        return gems;
    }

    private InstAltair getAltair(HttpServletRequest req) throws RequestException {
        AltairParams.Mode mode = parse(req, ALTAIR, ALTAIR_OP, null);
        // if no altair parameter was or the value is "NO" mode will be null
        if (mode == null) {
            return null;
        }
        InstAltair altair = new InstAltair();
        altair.setMode(mode);
        return altair;
    }

    /**
     * Translates the parameter values for "altair" used by the PIT into the AltairParams.Mode values
     * used in the OT.
     * @param paramName
     * @return
     */
    private static AltairParams.Mode modeFromAgsServletParameter(String paramName) {
        switch (paramName) {
            case "NO":     return null;
            case "NGS":    return AltairParams.Mode.NGS;
            case "NGS_FL": return AltairParams.Mode.NGS_FL;
            case "LGS":    return AltairParams.Mode.LGS;
            case "LGS_P1": return AltairParams.Mode.LGS_P1;
            case "LGS_OI": return AltairParams.Mode.LGS_OI;
            default:       throw new IllegalArgumentException("unknown value " + paramName + " for altair mode");
        }
    }

}
