package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsCatalogChoice;
import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SiderealTarget;

import java.util.Collections;
import java.util.List;

/**
 * OT-111: model for GemsGuideStarSearchDialog
 */
class GemsGuideStarSearchModel {

    private GemsCatalogChoice    _catalog;
    private boolean              _reviewCandidatesBeforeSearch;
    private boolean              _allowPosAngleAdjustments;
    private List<SiderealTarget> _candidates;
    private List<GemsGuideStars> _gemsGuideStars;

    public GemsCatalogChoice getCatalog() {
        return _catalog;
    }

    public void setCatalog(final GemsCatalogChoice catalog) {
        _catalog = catalog;
    }

    void setReviewCandidatesBeforeSearch(final boolean reviewCanditatesBeforeSearch) {
        _reviewCandidatesBeforeSearch = reviewCanditatesBeforeSearch;
    }

    boolean isReviewCandidatesBeforeSearch() {
        return _reviewCandidatesBeforeSearch;
    }

    void setAllowPosAngleAdjustments(final boolean allowPosAngleAdjustments) {
        _allowPosAngleAdjustments = allowPosAngleAdjustments;
    }

    boolean isAllowPosAngleAdjustments() {
        return _allowPosAngleAdjustments;
    }

    List<SiderealTarget> getCandidates() {
        return _candidates;
    }

    void setCandidates(final List<SiderealTarget> candidates) {
        _candidates = Collections.unmodifiableList(candidates);
    }

    List<GemsGuideStars> getGemsGuideStars() {
        return _gemsGuideStars;
    }

    void setGemsGuideStars(final List<GemsGuideStars> gemsGuideStars) {
        _gemsGuideStars = gemsGuideStars;
    }

    Option<SiderealTarget> targetAt(final int i) {
        return ImOption.fromOptional(_candidates.stream().skip(i).findFirst());
    }
}
