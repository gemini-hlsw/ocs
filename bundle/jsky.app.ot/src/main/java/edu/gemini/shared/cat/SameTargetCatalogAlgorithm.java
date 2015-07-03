package edu.gemini.shared.cat;

/**
 * Base class for algorithms that returns the same target as guide star
 */
@Deprecated
public abstract class SameTargetCatalogAlgorithm extends AbstractCatalogAlgorithm {
    /**
     * Base class for setting guide stars that are the same as the target
     *
     * @param name display name
     * @param desc description
     */
    protected SameTargetCatalogAlgorithm(String name, String desc) {
        super(name, desc);
        setStarTypeOptions(new String[]{"WFS", "OIWFS", "AOWFS"});
    }
}
