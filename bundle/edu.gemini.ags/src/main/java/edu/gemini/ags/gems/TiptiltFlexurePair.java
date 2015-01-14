package edu.gemini.ags.gems;


import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.gems.GemsGuideStarType;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups a pair of GemsCatalogSearchResults to be used for tiptilt and flexure stars.
 * The two results must be from different guide probe groups (Canopus, GSAOI, etc).
 */
public class TiptiltFlexurePair {
    private final GemsCatalogSearchResults tiptiltResults;
    private final GemsCatalogSearchResults flexureResults;

    private TiptiltFlexurePair(GemsCatalogSearchResults tiptiltResults, GemsCatalogSearchResults flexureResults) {
        this.tiptiltResults = tiptiltResults;
        this.flexureResults = flexureResults;
    }

    public GemsCatalogSearchResults getTiptiltResults() {
        return tiptiltResults;
    }

    public GemsCatalogSearchResults getFlexureResults() {
        return flexureResults;
    }

    // Returns pairs of results that can be used for tiptilt and flexure (from different guide probe groups).
    public static List<TiptiltFlexurePair> pairs(final List<GemsCatalogSearchResults> results) {
        GemsCatalogSearchResults canopusTiptilt = null;
        GemsCatalogSearchResults gsaoiOrFlamingosFlexure = null;

        GemsCatalogSearchResults gsaoiTiptilt = null;
        GemsCatalogSearchResults canopusFlexure = null;

        for (GemsCatalogSearchResults searchResults : results) {
            final GemsCatalogSearchCriterion criterion = searchResults.criterion();
            final GemsCatalogSearchKey key = criterion.key();
            if (key.starType() == GemsGuideStarType.tiptilt) {
                final GemsGuideProbeGroup tiptiltGroup = key.group();
                final String groupKey = tiptiltGroup.getKey();
                if ("CWFS".equals(groupKey)) {
                    canopusTiptilt = searchResults;
                } else if ("ODGW".equals(groupKey)) {
                    gsaoiTiptilt = searchResults;
                }
            } else {
                final GemsGuideProbeGroup flexureGroup = key.group();
                final String groupKey = flexureGroup.getKey();
                if ("ODGW".equals(groupKey) || "FII OIWFS".equals(groupKey)) {
                    gsaoiOrFlamingosFlexure = searchResults;
                } else if ("CWFS".equals(groupKey)) {
                    canopusFlexure = searchResults;
                }
            }
        }

        final List<TiptiltFlexurePair> pairs = new ArrayList<>();
        if (canopusTiptilt != null && gsaoiOrFlamingosFlexure != null) {
            pairs.add(new TiptiltFlexurePair(canopusTiptilt, gsaoiOrFlamingosFlexure));
        }
        if (gsaoiTiptilt != null && canopusFlexure != null) {
            pairs.add(new TiptiltFlexurePair(gsaoiTiptilt, canopusFlexure));
        }
        return pairs;
    }
}

