// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TargetObsComp.java 45277 2012-05-15 22:53:11Z swalker $
//
package edu.gemini.spModel.target.obsComp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.system.ITarget;

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

    public static final SPComponentType SP_TYPE = SPComponentType.TELESCOPE_TARGETENV;

    private static final String _VERSION =  "2009B-1";

    // Property name used when the contained TargetEnvironment is updated.
    public static final String TARGET_ENV_PROP = "TargetEnvironment";
    public static final String TARGET_POS_PROP = "TargetPos";

    private static TargetEnvironment createEmptyEnvironment() {
        SPTarget base = new SPTarget();
        return TargetEnvironment.create(base);
    }

    private final class PcePropagator implements TelescopePosWatcher {

        @Override public void telescopePosUpdate(WatchablePos tp) {
            firePropertyChange(TARGET_POS_PROP, null, tp);
        }

    }

    private TargetEnvironment targetEnv;
    private transient PcePropagator prop = new PcePropagator();

    public TargetObsComp() {
        super(SP_TYPE);
        setVersion(_VERSION);
        targetEnv = createEmptyEnvironment();
    }


    /**
     * Override clone to make sure the position list is correctly
     * initialized.
     */
    public Object clone() {
        final TargetObsComp toc = (TargetObsComp) super.clone();
        toc.targetEnv = targetEnv.cloneTargets();
        toc.prop      = toc.new PcePropagator();
        toc.targetEnv.getTargets().foreach(new ApplyOp<SPTarget>() {
            @Override public void apply(SPTarget target) { target.addWatcher(toc.prop); }
        });
        return toc;
    }

    /**
     * Override getTitle to return the name of the base position if set.
     */
    public String getTitle() {
        // By default, append the name of the base position.  If a title
        // has been directly set though, use that instead.
        final String title = super.getTitle();
        if (!isTitleChanged() || title.startsWith("Targets") && title.endsWith(")")) {
            // assume user did not edit title manually
            TargetEnvironment env = getTargetEnvironment();
            SPTarget tp = env.getBase();
            if (tp != null) {
                ITarget t = tp.getTarget();
                String name = t.getName();
                if (name == null || name.trim().length() == 0) {
                    name = "<Untitled>";
                }
                switch (t.getTag()) {
                    case JPL_MINOR_BODY:   return "Comet: " + name;
                    case MPC_MINOR_PLANET: return "Minor Planet: " + name;
                    case NAMED:            return "Solar System: " + name;
                    case SIDEREAL:         return "Target: " + name;
                }
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
    public void setTargetEnvironment(TargetEnvironment env) {
        if (env == null) env = createEmptyEnvironment();
        if (targetEnv == env) return;

        final TargetEnvironment orig = targetEnv;
        targetEnv = env;
        watchTargets();

        // Notify listeners that the env has been updated.
        firePropertyChange(TARGET_ENV_PROP, orig, targetEnv);
    }

    private void watchTargets() {
        targetEnv.getTargets().foreach(new ApplyOp<SPTarget>() {
            @Override
            public void apply(SPTarget target) {
                target.deleteWatcher(prop);
                target.addWatcher(prop);
            }
        });
    }

    // I'm so sorry.
    public void copyPropertyChangeListenersFrom(TargetObsComp that) {
        if ((that == null) || (that == this)) return;
        super.copyPropertyChangeListenersFrom(that);
        super.copyPropertyChangeListenersFrom(TARGET_ENV_PROP, that);
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
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);
        paramSet.addParamSet(targetEnv.getParamSet(factory));
        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        ParamSet targetEnv = paramSet.getParamSet(TargetEnvironment.PARAM_SET_NAME);
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

    private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
        is.defaultReadObject();
        prop = new PcePropagator();
        watchTargets();
    }
}
