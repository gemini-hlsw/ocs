package edu.gemini.ags.gems;

import edu.gemini.shared.skyobject.SkyObject;

import java.util.List;

/**
 * See OT-24
}  */
public class GemsCatalogSearchCriterion {
    private GemsCatalogSearchKey key;
    private CatalogSearchCriterion criterion;

    public GemsCatalogSearchCriterion(GemsCatalogSearchKey key, CatalogSearchCriterion criterion) {
        this.key = key;
        this.criterion = criterion;
    }

    public GemsCatalogSearchKey getKey() {
        return key;
    }

    public CatalogSearchCriterion getCriterion() {
        return criterion;
    }

    public GemsCatalogSearchResults setResults(List<SkyObject> results) {
        return new GemsCatalogSearchResults(this, results);
    }

    @Override
    public String toString() {
        return "GemsCatalogSearchCriterion{" +
                "key=" + key +
                ", criterion=" + criterion +
                '}';
    }
}
