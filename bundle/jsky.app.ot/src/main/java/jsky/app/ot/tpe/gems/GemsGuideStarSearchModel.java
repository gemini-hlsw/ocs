package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsGuideStarSearchOptions.*;
import edu.gemini.ags.gems.GemsCatalogSearchResults;
import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SiderealTarget;

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

    AnalyseChoice getAnalyseChoice() {
        return _analyseChoice;
    }

    void setAnalyseChoice(AnalyseChoice analyseChoice) {
        _analyseChoice = analyseChoice;
    }

    void setReviewCandidatesBeforeSearch(boolean reviewCanditatesBeforeSearch) {
        _reviewCandidatesBeforeSearch = reviewCanditatesBeforeSearch;
    }

    boolean isReviewCandidatesBeforeSearch() {
        return _reviewCandidatesBeforeSearch;
    }

    void setAllowPosAngleAdjustments(boolean allowPosAngleAdjustments) {
        _allowPosAngleAdjustments = allowPosAngleAdjustments;
    }

    boolean isAllowPosAngleAdjustments() {
        return _allowPosAngleAdjustments;
    }

    List<GemsCatalogSearchResults> getGemsCatalogSearchResults() {
        return _gemsCatalogSearchResults;
    }

    void setGemsCatalogSearchResults(List<GemsCatalogSearchResults> gemsCatalogSearchResults) {
        _gemsCatalogSearchResults = gemsCatalogSearchResults;
    }

    List<GemsGuideStars> getGemsGuideStars() {
        return _gemsGuideStars;
    }

    void setGemsGuideStars(List<GemsGuideStars> gemsGuideStars) {
        _gemsGuideStars = gemsGuideStars;
    }

    Option<SiderealTarget> targetAt(int i) {
        return ImOption.apply(_gemsCatalogSearchResults.get(0)).flatMap(c -> c.targetAt(i));
    }
}
