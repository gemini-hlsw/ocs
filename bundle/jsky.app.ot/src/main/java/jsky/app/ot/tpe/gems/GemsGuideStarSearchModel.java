package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsCandidates;
import edu.gemini.ags.gems.GemsCandidates$;
import edu.gemini.ags.gems.GemsCatalogChoice;
import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.obs.context.ObsContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * OT-111: model for GemsGuideStarSearchDialog
 */
class GemsGuideStarSearchModel {

    private GemsCatalogChoice    _catalog;
    private boolean              _reviewCandidatesBeforeSearch;
    private boolean              _allowPosAngleAdjustments;
    private List<SiderealTarget> _allCandidates;  // includes PWFS / SFS candidates
    private List<SiderealTarget> _cwfsCandidates; // subset of just CWFS NGS candidates
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

    List<SiderealTarget> getAllCandidates() {
        return _allCandidates;
    }

    List<SiderealTarget> getCwfsCandidates() {
        return _cwfsCandidates;
    }

    void setCandidates(
        final ObsContext           obsContext,
        final Set<Angle>           posAngles,
        final List<SiderealTarget> candidates
    ) {
        _allCandidates  = Collections.unmodifiableList(candidates);
        _cwfsCandidates =
            Collections.unmodifiableList(
                DefaultImList.create(
                    GemsCandidates$.MODULE$.groupAndValidateForJava(obsContext, posAngles, _allCandidates)
                ).flatMap(gc -> DefaultImList.create(gc.cwfsCandidatesAsJava()))
                 .toList()
            );
    }

    void resetCandidates() {
        _allCandidates  = Collections.emptyList();
        _cwfsCandidates = Collections.emptyList();
    }

    List<GemsGuideStars> getGemsGuideStars() {
        return _gemsGuideStars;
    }

    void setGemsGuideStars(final List<GemsGuideStars> gemsGuideStars) {
        _gemsGuideStars = gemsGuideStars;
    }

    Option<SiderealTarget> cwfsTargetAt(final int i) {
        return ImOption.fromOptional(_cwfsCandidates.stream().skip(i).findFirst());
    }
}
