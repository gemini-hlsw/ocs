package edu.gemini.ags.gems;

import edu.gemini.shared.skyobject.SkyObject;

import java.util.List;

/**
 * Results of a GeMS catalog search
 * See OT-24
 */
public class GemsCatalogSearchResults {
    private GemsCatalogSearchCriterion criterion;
    private List<SkyObject> results;

    public GemsCatalogSearchResults(GemsCatalogSearchCriterion criterion, List<SkyObject> results) {
        this.criterion = criterion;
        this.results = results;
    }

    public GemsCatalogSearchCriterion getCriterion() {
        return criterion;
    }

    public List<SkyObject> getResults() {
        return results;
    }
}
