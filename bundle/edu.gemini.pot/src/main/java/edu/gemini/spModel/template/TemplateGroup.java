package edu.gemini.spModel.template;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.type.DescribableSpType;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.util.VersionToken;

import java.io.Serializable;

public final class TemplateGroup extends AbstractDataObject {

    public static final String VERSION = "2012B-1";
    public static final SPComponentType SP_TYPE = SPComponentType.TEMPLATE_GROUP;

    // Private PIO parameters
    private static final String PARAM_BLUEPRINT = "blueprint";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_VERSION_TOKEN = "versionToken";
    private static final String PARAM_VERSION_TOKEN_NEXT = "versionTokenNext";

    // Public property identifiers (for truly mutable stuff only)
    public static final String PROP_STATUS = PARAM_STATUS;
    public static final String PROP_SPLIT_TOKEN = PARAM_VERSION_TOKEN;

    /**
     * Observation status values.
     */
    public static enum Status implements DisplayableSpType, DescribableSpType {

        PHASE2("Phase 2", "In Phase 2"),
        FOR_REVIEW("For Review", "Ready for review by contact scientist"),
        IN_REVIEW("In Review", "Under review by NGO staff"),
        READY("Ready", "Ready to execute or schedule"),
        ;

        /** The default ObservationStatus value **/
        public static Status DEFAULT = PHASE2;

        private String displayValue;
        private String description;

        private Status(String displayVal, String description) {
            displayValue = displayVal;
            this.description = description;
        }

        public String displayValue() {
            return displayValue;
        }

        public String description() {
            return description;
        }

        public String toString() {
            return displayValue;
        }

    }

    /**
     * Immutable, PIO-serializable carrier for parameters that can be applied to a TemplateGroup.
     */
    public static final class Args implements Serializable {

        public static final String PARAM_SET_NAME     = "templateArgs";
        public static final String PARAM_TARGET       = "target";
        public static final String PARAM_SITE_QUALITY = "siteQuality";
        public static final String PARAM_TIME         = "time";

        private final String targetId;
        private final String siteQualityId;
        private final TimeValue time;

        public Args(String targetId, String siteQualityId, TimeValue time) {
            this.targetId      = targetId;
            this.siteQualityId = siteQualityId;
            this.time          = time;
        }

        public Args(ParamSet paramSet) {
            this(Pio.getValue(paramSet, PARAM_TARGET),
                 Pio.getValue(paramSet, PARAM_SITE_QUALITY),
                 TimeValue.millisecondsToTimeValue(Pio.getLongValue(paramSet, PARAM_TIME, 0l), TimeValue.Units.hours));
        }

        public String getTargetId() {
            return targetId;
        }

        public String getSiteQualityId() {
            return siteQualityId;
        }

        public TimeValue getTime() {
            return time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Args that = (Args) o;
            if (!siteQualityId.equals(that.siteQualityId)) return false;
            if (!targetId.equals(that.targetId)) return false;
            if (!time.equals(that.time)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = targetId.hashCode();
            result = 31 * result + siteQualityId.hashCode();
            result = 31 * result + time.hashCode();
            return result;
        }

        public ParamSet getParamSet(PioFactory factory) {
            final ParamSet ps = factory.createParamSet(PARAM_SET_NAME);
            Pio.addParam(factory, ps, PARAM_TARGET, targetId);
            Pio.addParam(factory, ps, PARAM_SITE_QUALITY, siteQualityId);
            Pio.addLongParam(factory, ps, PARAM_TIME, time.getMilliseconds());
            return ps;
        }

    }

    // Each template group is derived from a single blueprint, and has a list of (Target, Conditions) refs to
    // which it can apply. These args can be moved between templates, but only if the templates share the same
    // blueprint (this allows forking).
    private String blueprintId;
    private Status status = Status.DEFAULT;
    private VersionToken versionToken = new VersionToken(1);

    public TemplateGroup() {
        setTitle("Untitled");
        setType(SP_TYPE);
        setVersion(VERSION);
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(String blueprintId) {
        if (blueprintId == null)
            throw new IllegalArgumentException("blueprintId cannot be null.");
        this.blueprintId = blueprintId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (this.status != status) {
            final Status prev = this.status;
            this.status = status;
            firePropertyChange(PROP_STATUS, prev, status);
        }
    }

    public VersionToken getVersionToken() {
        return versionToken;
    }

    public void setVersionToken(VersionToken versionToken) {
        if (!this.versionToken.equals(versionToken)) {
            final VersionToken prev = this.versionToken;
            this.versionToken = versionToken;
            firePropertyChange(PROP_SPLIT_TOKEN, prev, versionToken);
        }
    }

    @Override
    public ParamSet getParamSet(PioFactory factory) {
        final ParamSet ps = super.getParamSet(factory);
        Pio.addParam(factory, ps, PARAM_BLUEPRINT, blueprintId);
        Pio.addParam(factory, ps, PARAM_STATUS, status.name());
        Pio.addParam(factory, ps, PARAM_VERSION_TOKEN, versionToken.toString());
        Pio.addIntParam(factory, ps, PARAM_VERSION_TOKEN_NEXT, versionToken.nextSegment());
        return ps;
    }

    @Override
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);
        blueprintId = Pio.getValue(paramSet, PARAM_BLUEPRINT);
        status = Status.valueOf(Pio.getValue(paramSet, PARAM_STATUS));

        int[] segments = VersionToken.segments(Pio.getValue(paramSet, PARAM_VERSION_TOKEN, versionToken.toString()));
        int next = Pio.getIntValue(paramSet, PARAM_VERSION_TOKEN_NEXT, 1);
        versionToken = VersionToken.apply(segments, next);
    }

}
