package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsGuideStarSearchOptions.*;
import edu.gemini.ags.gems.GemsCatalogSearchResults;
import edu.gemini.ags.gems.GemsGuideStars;
import jsky.catalog.TableQueryResult;

import java.util.List;

/**
 * OT-111: model for GemsGuideStarSearchDialog
 */
class GemsGuideStarSearchModel {

    private CatalogChoice _catalog;

    // If _catalog is USER_CATALOG, this is the file name for the local catalog
    private String _userCatalogFileName;

    // If _catalog is USER_CATALOG, this is the local catalog
    private TableQueryResult _userCatalog;

    private NirBandChoice _band;
    private AnalyseChoice _analyseChoice;
    private boolean _reviewCanditatesBeforeSearch;
    private boolean _allowPosAngleAdjustments;
    private List<GemsCatalogSearchResults> _gemsCatalogSearchResults;
    private List<GemsGuideStars> _gemsGuideStars;

    public CatalogChoice getCatalog() {
        return _catalog;
    }

    public void setCatalog(CatalogChoice catalog) {
        _catalog = catalog;
    }

    public String getUserCatalogFileName() {
        return _userCatalogFileName;
    }

    public void setUserCatalogFileName(String userCatalogFileName) {
        _userCatalogFileName = userCatalogFileName;
    }

    public TableQueryResult getUserCatalog() {
        return _userCatalog;
    }

    public void setUserCatalog(TableQueryResult userCatalog) {
        _userCatalog = userCatalog;
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

    public void setReviewCanditatesBeforeSearch(boolean reviewCanditatesBeforeSearch) {
        _reviewCanditatesBeforeSearch = reviewCanditatesBeforeSearch;
    }

    public boolean isReviewCanditatesBeforeSearch() {
        return _reviewCanditatesBeforeSearch;
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
