package edu.gemini.spModel.target.obsComp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.GuideEnv;
import edu.gemini.spModel.target.env.GuideEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.system.ITarget;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for telescope observation component items.  Maintains a
 * position list and keeps up-to-date the base position element of
 * the observation data for the observation context.
 */
public final class TargetObsComp extends AbstractDataObject implements GuideProbeProvider {
    // for serialization
    private static final long serialVersionUID = 2L;

    public static final SPComponentType SP_TYPE = SPComponentType.TELESCOPE_TARGETENV;

    private static final String _VERSION =  "2009B-1";

    // Property name used when the contained TargetEnvironment is updated.
    public static final String TARGET_ENV_PROP = "TargetEnvironment";
    public static final String TARGET_POS_PROP = "TargetPos";

    // A map from ITarget tags to strings used to create the title prefix for this node.
    private static Map<ITarget.Tag,String> targetPrefixes = new HashMap<ITarget.Tag,String>() {{
        put(ITarget.Tag.JPL_MINOR_BODY,   "Comet: ");
        put(ITarget.Tag.MPC_MINOR_PLANET, "Minor Planet: ");
        put(ITarget.Tag.NAMED,            "Solar System: ");
        put(ITarget.Tag.SIDEREAL,         "Target: ");
    }};

    private static TargetEnvironment createEmptyEnvironment() {
        final SPTarget base = new SPTarget();
        return TargetEnvironment.create(base);
    }

    private final class PcePropagator implements TelescopePosWatcher {

        @Override public void telescopePosUpdate(final WatchablePos tp) {
            firePropertyChange(TARGET_POS_PROP, null, tp);
        }

    }

    private TargetEnvironment targetEnv;
    private transient PcePropagator prop = new PcePropagator();

    public TargetObsComp() {
        super(SP_TYPE);
        setVersion(_VERSION);
        targetEnv = createEmptyEnvironment();
        watchTargets();
    }


    /**
     * Override clone to make sure the position list is correctly
     * initialized.
     */
    public Object clone() {
        final TargetObsComp toc = (TargetObsComp) super.clone();
        toc.targetEnv = targetEnv.cloneTargets();
        toc.prop      = toc.new PcePropagator();
        toc.targetEnv.getTargets().foreach(target -> target.addWatcher(toc.prop));
        return toc;
    }

    /**
     * Override getTitle to return the name of the base position if set.
     */
    public String getTitle() {
        // By default, append the name of the base position.  If a title
        // has been directly set though, use that instead.
        final String title = super.getTitle();
        if (!isTitleChanged() || targetPrefixes.values().stream().anyMatch(title::startsWith)) {
            final TargetEnvironment env = getTargetEnvironment();
            final SPTarget tp = env.getBase();
            if (tp != null) {
                final ITarget t = tp.getTarget();
                final String initName  = t.getName();
                final String finalName = initName == null || initName.trim().isEmpty() ? "<Untitled>" : initName;
                return targetPrefixes.getOrDefault(t.getTag(), "") + finalName;
            }
        }

        return title;
    }

    /**
     * Get the target environment.
     */
    public TargetEnvironment getTargetEnvironment() {
        return targetEnv;
    }

    /**
     * Set the TargetEnvironment.
     */
    public void setTargetEnvironment(final TargetEnvironment env) {
        final TargetEnvironment envNotNull = env == null ? createEmptyEnvironment() : env;
        if (targetEnv == envNotNull) return;

        final TargetEnvironment orig = targetEnv;
        unwatchTargets();
        targetEnv = envNotNull;
        watchTargets();

        // Notify listeners that the env has been updated.
        firePropertyChange(TARGET_ENV_PROP, orig, targetEnv);
    }

    private void unwatchTargets() {
        targetEnv.getTargets().foreach(target -> {
            target.deleteWatcher(prop);
        });
    }

    private void watchTargets() {
        targetEnv.getTargets().foreach(target -> {
            target.addWatcher(prop);
        });
    }

    /**
     * Convenience method to access the base position.
     */
    public SPTarget getBase() {
        return targetEnv.getBase();
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory the PioFactory to use
     */
    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);
        paramSet.addParamSet(targetEnv.getParamSet(factory));
        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(final ParamSet paramSet) {
        super.setParamSet(paramSet);

        final ParamSet targetEnv = paramSet.getParamSet(TargetEnvironment.PARAM_SET_NAME);
        if (targetEnv == null) {
            setTargetEnvironment(createEmptyEnvironment());
        } else {
            setTargetEnvironment(TargetEnvironment.fromParamSet(targetEnv));
        }
    }

    private static final Collection<GuideProbe> GUIDE_PROBES = GuideProbeUtil.instance.createCollection(PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2);

    public Collection<GuideProbe> getGuideProbes() {
        return GUIDE_PROBES;
    }

    private void readObject(final ObjectInputStream is) throws IOException, ClassNotFoundException {
        is.defaultReadObject();
        prop = new PcePropagator();
        watchTargets();
    }
}
