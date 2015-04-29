package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsGuideStarSearchOptions.*;
import edu.gemini.ags.gems.GemsCatalogSearchResults;
import edu.gemini.ags.gems.GemsGuideStars;

import java.util.List;

/**
 * OT-111: model for GemsGuideStarSearchDialog
 */
class GemsGuideStarSearchModel {

    private CatalogChoice _catalog;

    private NirBandChoice _band;
    private AnalyseChoice _analyseChoice;
    private boolean _reviewCandidatesBeforeSearch;
    private boolean _allowPosAngleAdjustments;
    private List<GemsCatalogSearchResults> _gemsCatalogSearchResults;
    private List<GemsGuideStars> _gemsGuideStars;

    public CatalogChoice getCatalog() {
        return _catalog;
    }

    public void setCatalog(CatalogChoice catalog) {
        _catalog = catalog;
    }

    public NirBandChoice getBand() {
        return _band;
    }

    public void setBand(NirBandChoice band) {
        _band = band;
    }

    public AnalyseChoice getAnalyseChoice() {
        return _analyseChoice;
    }

    public void setAnalyseChoice(AnalyseChoice analyseChoice) {
        _analyseChoice = analyseChoice;
    }

    public void setReviewCandidatesBeforeSearch(boolean reviewCanditatesBeforeSearch) {
        _reviewCandidatesBeforeSearch = reviewCanditatesBeforeSearch;
    }

    public boolean isReviewCandidatesBeforeSearch() {
        return _reviewCandidatesBeforeSearch;
    }

    public void setAllowPosAngleAdjustments(boolean allowPosAngleAdjustments) {
        _allowPosAngleAdjustments = allowPosAngleAdjustments;
    }

    public boolean isAllowPosAngleAdjustments() {
        return _allowPosAngleAdjustments;
    }

    public List<GemsCatalogSearchResults> getGemsCatalogSearchResults() {
        return _gemsCatalogSearchResults;
    }

    public void setGemsCatalogSearchResults(List<GemsCatalogSearchResults> gemsCatalogSearchResults) {
        _gemsCatalogSearchResults = gemsCatalogSearchResults;
    }

    public List<GemsGuideStars> getGemsGuideStars() {
        return _gemsGuideStars;
    }

    public void setGemsGuideStars(List<GemsGuideStars> gemsGuideStars) {
        _gemsGuideStars = gemsGuideStars;
    }
}
