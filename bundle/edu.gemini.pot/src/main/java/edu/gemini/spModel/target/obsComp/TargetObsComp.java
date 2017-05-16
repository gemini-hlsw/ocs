package edu.gemini.spModel.target.obsComp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.env.TargetEnvironment;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;

/**
 * A class for telescope observation component items.  Maintains a
 * position list and keeps up-to-date the base position element of
 * the observation data for the observation context.
 */
public final class TargetObsComp extends AbstractDataObject implements GuideProbeProvider {
    // for serialization
    private static final long serialVersionUID = 2L;

    private static final TargetObsCompHelper helper = new TargetObsCompHelper();

    public static final SPComponentType SP_TYPE = SPComponentType.TELESCOPE_TARGETENV;

    private static final String _VERSION =  "2009B-1";

    // Property name used when the contained TargetEnvironment is updated.
    public static final String TARGET_ENV_PROP = "TargetEnvironment";
    public static final String TARGET_POS_PROP = "TargetPos";

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

    public String getTitle() {
        // Always compute the name; the UI disallows changes.
        final Asterism asterism = getAsterism();
        final String initName  = asterism.name();
        final String finalName = initName == null || initName.trim().isEmpty() ? "<Untitled>" : initName;
        return helper.targetTag(asterism) + ": " + finalName;
    }

    /**
     * Get the target environment (never null).
     */
    public TargetEnvironment getTargetEnvironment() {
        return targetEnv;
    }

    /**
     * Get the asterism (never null).
     */
    public Asterism getAsterism() {
      return getTargetEnvironment().getAsterism();
    }

    @Deprecated
    public SPTarget getArbitraryTargetFromAsterism() {
      return getTargetEnvironment().getArbitraryTargetFromAsterism();
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
        targetEnv.getTargets().foreach(target -> target.deleteWatcher(prop));
    }

    private void watchTargets() {
        targetEnv.getTargets().foreach(target -> target.addWatcher(prop));
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
