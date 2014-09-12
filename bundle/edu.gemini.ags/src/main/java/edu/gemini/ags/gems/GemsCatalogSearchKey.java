package edu.gemini.ags.gems;

import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.gems.GemsGuideStarType;

/**
 * Represents the GeMS catalog star options
 * See OT-24
 */
public class GemsCatalogSearchKey {
    private GemsGuideStarType type;
    private GemsGuideProbeGroup group;

    public GemsCatalogSearchKey(GemsGuideStarType type, GemsGuideProbeGroup group) {
        this.type = type;
        this.group = group;
    }

    public GemsGuideStarType getType() {
        return type;
    }

    public GemsGuideProbeGroup getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "GemsCatalogSearchKey{" +
                "type=" + type +
                ", group=" + group.getKey() +
                '}';
    }
}
