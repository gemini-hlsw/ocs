package edu.gemini.spModel.template;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPTemplateGroup;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.obscomp.SPGroup.GroupType;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.util.VersionToken;

public final class TemplateGroup extends AbstractDataObject {

    public static final String VERSION = "2021A-1";
    public static final SPComponentType SP_TYPE = SPComponentType.TEMPLATE_GROUP;

    public static final ISPNodeInitializer<ISPTemplateGroup, TemplateGroup> NI =
        new SimpleNodeInitializer<>(SP_TYPE, TemplateGroup::new);

    // Private PIO parameters
    private static final String PARAM_BLUEPRINT = "blueprint";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_VERSION_TOKEN = "versionToken";
    private static final String PARAM_VERSION_TOKEN_NEXT = "versionTokenNext";
    private static final String PARAM_GROUP_TYPE = "groupType";

    // Public property identifiers (for truly mutable stuff only)
    public static final String PROP_SPLIT_TOKEN = PARAM_VERSION_TOKEN;
    public static final String PROP_GROUP_TYPE = PARAM_GROUP_TYPE;

    // Each template group is derived from a single blueprint, and has a list of (Target, Conditions) refs to
    // which it can apply. These args can be moved between templates, but only if the templates share the same
    // blueprint (this allows forking).
    private String blueprintId;
    private VersionToken versionToken = new VersionToken(1);
    private GroupType groupType = GroupType.DEFAULT;

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

    public GroupType getGroupType() {
        return groupType;
    }

    public void setGroupType(GroupType groupType) {
        if (groupType == null)
            throw new IllegalArgumentException("groupType cannot be null");
        this.groupType = groupType;
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
        Pio.addParam(factory, ps, PARAM_VERSION_TOKEN, versionToken.toString());
        Pio.addIntParam(factory, ps, PARAM_VERSION_TOKEN_NEXT, versionToken.nextSegment());
        Pio.addEnumParam(factory, ps, PARAM_GROUP_TYPE, groupType);
        return ps;
    }

    @Override
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);
        blueprintId = Pio.getValue(paramSet, PARAM_BLUEPRINT);
        groupType = Pio.getEnumValue(paramSet, PROP_GROUP_TYPE, GroupType.DEFAULT);
        final int[] segments = VersionToken.segments(Pio.getValue(paramSet, PARAM_VERSION_TOKEN, versionToken.toString()));
        final int next = Pio.getIntValue(paramSet, PARAM_VERSION_TOKEN_NEXT, 1);
        versionToken = VersionToken.apply(segments, next);
    }

}
