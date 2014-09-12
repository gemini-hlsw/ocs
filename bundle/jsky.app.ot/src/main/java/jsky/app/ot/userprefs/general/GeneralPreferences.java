package jsky.app.ot.userprefs.general;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import jsky.app.ot.userprefs.model.ExternalizablePreferences;
import jsky.app.ot.userprefs.model.PreferencesChangeListener;
import jsky.app.ot.userprefs.model.PreferencesSupport;
import jsky.util.Preferences;

/**
 * General OT preferences.
 */
public final class GeneralPreferences implements ExternalizablePreferences {

    /**
     * Instances are created via the given Builder, which should be constructed
     * and updated as required before calling the {@link #build} method}.
     */
    public static final class Builder {
        private boolean phase2Checking = true;
        private boolean guideStarAlternatives = true;
        private boolean warnUnsavedChanges = true;

        public Builder() { /* empty */ }

        public Builder phase2Checking(boolean val) {
            phase2Checking = val;
            return this;
        }

        public Builder guideStarAlternatives(boolean val) {
            guideStarAlternatives = val;
            return this;
        }

        public Builder warnUnsavedChanges(boolean val) {
            warnUnsavedChanges = val;
            return this;
        }

        public GeneralPreferences build() {
            return new GeneralPreferences(this);
        }
    }

    public static final class Factory implements ExternalizablePreferences.Factory<GeneralPreferences> {
        // Handle upgrading from past versions where preference bits were
        // stored in Jsky Preferences.
        private static final String P2C_PREF = "jsky.app.ot.OTOptions.checkEngineEnabled";

        private GeneralPreferences createDefault() {
            Builder b = new Builder();
            b.phase2Checking(Preferences.get(P2C_PREF, true));
            return b.build();
        }

        public GeneralPreferences create(Option<ParamSet> psetOpt) {
            if (None.instance().equals(psetOpt)) return createDefault();

            Builder b = new Builder();
            ParamSet pset = psetOpt.getValue();
            b.phase2Checking(Pio.getBooleanValue(pset, PHASE_2_CHECKING_PARAM, true));
            b.guideStarAlternatives(Pio.getBooleanValue(pset, GUIDE_STAR_ALT_PARAM, true));
            b.warnUnsavedChanges(Pio.getBooleanValue(pset, WARN_UNSAVED_CHANGES_PARAM, true));
            return b.build();
        }
    }

    public static final Factory FACTORY = new Factory();
    public static final String PSET_NAME = "general";

    private static final String PHASE_2_CHECKING_PARAM = "phase2Checking";
    private static final String GUIDE_STAR_ALT_PARAM = "guideStarAlt";
    private static final String WARN_UNSAVED_CHANGES_PARAM = "warnUnsavedChanges";

    private final boolean phase2Checking;
    private final boolean guideStarAlternatives;
    private final boolean warnUnsavedChanges;

    private GeneralPreferences(Builder builder) {
        this.phase2Checking = builder.phase2Checking;
        this.guideStarAlternatives = builder.guideStarAlternatives;
        this.warnUnsavedChanges = builder.warnUnsavedChanges;
    }

    /**
     * @return <code>true</code> if phase 2 checking should be performed;
     *         <code>false</code> otherwise
     */
    public boolean isPhase2Checking() {
        return phase2Checking;
    }

    /**
     * @return <code>true</code> if guide star alternatives should be
     *         suggested; <code>false</code> to ignore better alternatives and
     *         automatically pick an inferior guide star that matches the observing
     *         context
     */
    public boolean showGuideStarAlternatives() {
        return guideStarAlternatives;
    }

    public boolean warnUnsavedChanges() {
        return warnUnsavedChanges;
    }

    private Builder getBuilder() {
        Builder b = new Builder();
        b.phase2Checking(this.phase2Checking);
        b.guideStarAlternatives(this.guideStarAlternatives);
        b.warnUnsavedChanges(this.warnUnsavedChanges);
        return b;
    }

    public GeneralPreferences withPhase2Checking(boolean val) {
        return getBuilder().phase2Checking(val).build();
    }

    public GeneralPreferences withGuideStarAlternatives(boolean val) {
        return getBuilder().guideStarAlternatives(val).build();
    }

    public GeneralPreferences withWarnUnsavedChanges(boolean val) {
        return getBuilder().warnUnsavedChanges(val).build();
    }

    public boolean store() {
        return store(this);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet res = factory.createParamSet(PSET_NAME);
        Pio.addBooleanParam(factory, res, PHASE_2_CHECKING_PARAM, phase2Checking);
        Pio.addBooleanParam(factory, res, GUIDE_STAR_ALT_PARAM, guideStarAlternatives);
        Pio.addBooleanParam(factory, res, WARN_UNSAVED_CHANGES_PARAM, warnUnsavedChanges);
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeneralPreferences that = (GeneralPreferences) o;

        if (phase2Checking != that.phase2Checking) return false;
        if (guideStarAlternatives != that.guideStarAlternatives) return false;
        return warnUnsavedChanges == that.warnUnsavedChanges;
    }

    @Override
    public int hashCode() {
        int result = phase2Checking ? 1 : 0;
        result = 31 * result + (guideStarAlternatives ? 1 : 0);
        result = 31 * result + (warnUnsavedChanges ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("[phase2Checking=%b, guideStarAlt=%b, warnUnsavedChanges=%b]", phase2Checking, guideStarAlternatives, warnUnsavedChanges);
    }

    private static final PreferencesSupport<GeneralPreferences> sup =
            new PreferencesSupport<GeneralPreferences>(PSET_NAME, FACTORY);

    public static GeneralPreferences fetch() {
        return sup.fetch();
    }

    public static boolean store(GeneralPreferences val) {
        return sup.store(val);
    }

    public static void addChangeListener(PreferencesChangeListener<GeneralPreferences> listener) {
        sup.addChangeListener(listener);
    }
}
