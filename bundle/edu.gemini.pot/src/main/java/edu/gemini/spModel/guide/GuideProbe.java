package edu.gemini.spModel.guide;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.PredicateOp;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.obs.context.ObsContext;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A immutable guide star probe, or "guider" more generally.
 */
public interface GuideProbe {

    /**
     * A guider is one of a adaptive optics wave front sensor, on-instrument
     * wavefront sensor, or a peripheral wavefront senson.
     */
    enum Type {
        AOWFS,
        OIWFS,
        PWFS,
    }

    /**
     * Compares guiders based upon their key string.
     */
    enum KeyComparator implements Comparator<GuideProbe>, Serializable {
        instance;

        public int compare(GuideProbe g1, GuideProbe g2) {
            return g1.getKey().compareTo(g2.getKey());
        }
    }

    /**
     * Compares guiders based upon their type, following by their key.
     */
    enum TypeComparator implements Comparator<GuideProbe>, Serializable {
        instance;

        public int compare(GuideProbe g1, GuideProbe g2) {
            int res = g1.getType().compareTo(g2.getType());
            if (res != 0) return res;
            return g1.getKey().compareTo(g2.getKey());
        }
    }

    /**
     * A {@link PredicateOp} used to match on GuideProbe
     * {@link GuideProbe#getType types}.
     */
    class TypeMatcher implements PredicateOp<GuideProbe> {
        private final GuideProbe.Type type;

        /**
         * Constucts with the GuideProbe type to match on.
         */
        public TypeMatcher(GuideProbe.Type type) {
            this.type = type;
        }

        @Override
        public Boolean apply(GuideProbe guideProbe) {
            return guideProbe.getType() == type;
        }
    }

    /**
     * Gets the short identifier for this type of guider.
     */
    String getKey();

    /**
     * Gets the type of the guider.
     */
    Type getType();

    /**
     * Gets a longer more descriptive name for the guider.
     */
    String getDisplayName();

    /**
     * Gets the name of the property that is sent to the sequencer or telescope
     * control console.
     */
    String getSequenceProp();

    /**
     * Get the guide options provided by this guider.
     */
    GuideOptions getGuideOptions();

    /**
     * Gets the {@link GuideProbeGroup group} that this guider is a part of,
     * if any.
     *
     * @return {@link Some}<{@link GuideProbeGroup}> if this guider is part of
     *         a group, or {@link None} otherwise
     */
    Option<GuideProbeGroup> getGroup();

    /**
     * Gets the range of a guide probe defined as a {@link PatrolField} with dimensions in arcseconds and
     * coordinates relative to the instrument's coordinate system.
     */
    PatrolField getPatrolField();

    /**
     * Gets a representation of the patrol field which is flipped and offset according to instrument
     * specifics (e.g. IFU offsets for GMOS and the port the instrument is currently mounted on).
     */
    Option<PatrolField> getCorrectedPatrolField(ObsContext ctx);

    /**
     * Indicates the bands that will be used for a given probe.
     * Use R-band by default.
     */
    BandsList getBands();
}
