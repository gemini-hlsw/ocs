package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsGuideStarSearchOptions.*;
import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.ags.gems.Ngs2Result;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SiderealTarget;

import java.util.List;

/**
 * OT-111: model for GemsGuideStarSearchDialog
 */
class GemsGuideStarSearchModel {

    private CatalogChoice        _catalog;
    private boolean              _reviewCandidatesBeforeSearch;
    private boolean              _allowPosAngleAdjustments;
    private Ngs2Result           _ngs2Result;
    private List<GemsGuideStars> _gemsGuideStars;

    public CatalogChoice getCatalog() {
        return _catalog;
    }

    public void setCatalog(final CatalogChoice catalog) {
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

    Ngs2Result getNGS2Result() {
        return _ngs2Result;
    }

    void setNGS2Result(final Ngs2Result ngs2Result) {
        _ngs2Result = ngs2Result;
    }

    List<GemsGuideStars> getGemsGuideStars() {
        return _gemsGuideStars;
    }

    void setGemsGuideStars(final List<GemsGuideStars> gemsGuideStars) {
        _gemsGuideStars = gemsGuideStars;
    }

    Option<SiderealTarget> targetAt(final int i) {
        return ImOption.fromOptional(
            _ngs2Result.cwfsCandidatesAsJava()
                       .stream()
                       .skip(i)
                       .findFirst()
        );
    }
}
