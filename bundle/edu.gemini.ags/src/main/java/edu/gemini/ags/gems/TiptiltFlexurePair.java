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
    private GemsCatalogSearchResults tiptiltResults;
    private GemsCatalogSearchResults flexureResults;

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
    public static List<TiptiltFlexurePair> pairs(List<GemsCatalogSearchResults> results) {
        GemsCatalogSearchResults canopusTiptilt = null;
        GemsCatalogSearchResults gsaoiOrFlamingosFlexure = null;

        GemsCatalogSearchResults gsaoiTiptilt = null;
        GemsCatalogSearchResults canopusFlexure = null;

        for (GemsCatalogSearchResults searchResults : results) {
            GemsCatalogSearchCriterion criterion = searchResults.getCriterion();
            GemsCatalogSearchKey key = criterion.key();
            if (key.getType() == GemsGuideStarType.tiptilt) {
                GemsGuideProbeGroup tiptiltGroup = key.getGroup();
                String groupKey = tiptiltGroup.getKey();
                if ("CWFS".equals(groupKey)) {
                    canopusTiptilt = searchResults;
                } else if ("ODGW".equals(groupKey)) {
                    gsaoiTiptilt = searchResults;
                }
            } else {
                GemsGuideProbeGroup flexureGroup = key.getGroup();
                String groupKey = flexureGroup.getKey();
                if ("ODGW".equals(groupKey) || "FII OIWFS".equals(groupKey)) {
                    gsaoiOrFlamingosFlexure = searchResults;
                } else if ("CWFS".equals(groupKey)) {
                    canopusFlexure = searchResults;
                }
            }
        }

        List<TiptiltFlexurePair> pairs = new ArrayList<TiptiltFlexurePair>();
        if (canopusTiptilt != null && gsaoiOrFlamingosFlexure != null) {
            pairs.add(new TiptiltFlexurePair(canopusTiptilt, gsaoiOrFlamingosFlexure));
        }
        if (gsaoiTiptilt != null && canopusFlexure != null) {
            pairs.add(new TiptiltFlexurePair(gsaoiTiptilt, canopusFlexure));
        }
        return pairs;
    }
}

