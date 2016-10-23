package jsky.app.ot.tpe.gems;

import edu.gemini.catalog.ui.tpe.CatalogImageDisplay;
import edu.gemini.pot.ModelConverters;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.gems.GemsTipTiltMode;
import edu.gemini.ags.gems.GemsCatalogSearchResults;
import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.spModel.obs.context.ObsContext;
import jsky.app.ot.tpe.GemsGuideStarWorker;
import jsky.app.ot.tpe.TpeImageWidget;
import jsky.app.ot.tpe.TpeManager;
import jsky.coords.WorldCoords;
import jsky.util.gui.DialogUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OT-111: Controller for GemsGuideStarSearchDialog
 */
class GemsGuideStarSearchController {
    private final GemsGuideStarSearchModel _model;
    private final GemsGuideStarWorker _worker;
    private final GemsGuideStarSearchDialog _dialog;
    private final CatalogImageDisplay _tpe;

    /**
     * Constructor
     * @param model the overall GUI model
     * @param worker does the background tasks like query, analyze
     * @param dialog the main dialog
     * @param imageDisplay the Tpe reference
     */
    GemsGuideStarSearchController(GemsGuideStarSearchModel model, GemsGuideStarWorker worker,
                                  GemsGuideStarSearchDialog dialog, CatalogImageDisplay imageDisplay) {
        _model = model;
        _worker = worker;
        _dialog = dialog;
        _tpe = imageDisplay;
    }

    /**
     * Searches for guide star candidates and saves the results in the model
     */
    public void query(scala.concurrent.ExecutionContext ec) throws Exception {
        WorldCoords basePos = _tpe.getBasePos();
        ObsContext obsContext = _worker.getObsContext(basePos.getRaDeg(), basePos.getDecDeg());
        Set<edu.gemini.spModel.core.Angle> posAngles = getPosAngles(obsContext);

        MagnitudeBand nirBand = _model.getBand().getBand();

        GemsTipTiltMode tipTiltMode = _model.getAnalyseChoice().getGemsTipTiltMode();
        List<GemsCatalogSearchResults> results;
        try {
            results = _worker.search(_model.getCatalog(), tipTiltMode, obsContext, posAngles,
                    new scala.Some<>(nirBand), ec);
        } catch(Exception e) {
            DialogUtil.error(_dialog, e);
            results = new ArrayList<>();
            _dialog.setState(GemsGuideStarSearchDialog.State.PRE_QUERY);
        }

        if (_model.isReviewCandidatesBeforeSearch()) {
            _model.setGemsCatalogSearchResults(results);
        } else {
            _model.setGemsCatalogSearchResults(results);
            _model.setGemsGuideStars(_worker.findAllGuideStars(obsContext, posAngles, results));
        }
    }

    private Set<edu.gemini.spModel.core.Angle> getPosAngles(ObsContext obsContext) {
        Set<edu.gemini.spModel.core.Angle> posAngles = new HashSet<>();
        posAngles.add(ModelConverters.toNewAngle(obsContext.getPositionAngle()));
        if (_model.isAllowPosAngleAdjustments()) {
            posAngles.add(ModelConverters.toNewAngle(new Angle(0., Angle.Unit.DEGREES)));
            posAngles.add(ModelConverters.toNewAngle(new Angle(90., Angle.Unit.DEGREES)));
            posAngles.add(ModelConverters.toNewAngle(new Angle(180., Angle.Unit.DEGREES)));
            posAngles.add(ModelConverters.toNewAngle(new Angle(270., Angle.Unit.DEGREES)));
        }
        return posAngles;
    }

    /**
     * Analyzes the search results and saves a list of possible guide star configurations to the model.
     * @param excludeCandidates list of SkyObjects to exclude
     */
    // Called from the TPE
    void analyze(List<SiderealTarget> excludeCandidates) throws Exception {
        TpeImageWidget tpe = TpeManager.create().getImageWidget();
        WorldCoords basePos = tpe.getBasePos();
        ObsContext obsContext = _worker.getObsContext(basePos.getRaDeg(), basePos.getDecDeg());
        Set<edu.gemini.spModel.core.Angle> posAngles = getPosAngles(obsContext);
        _model.setGemsGuideStars(_worker.findAllGuideStars(obsContext, posAngles,
                filter(excludeCandidates, _model.getGemsCatalogSearchResults())));
    }

    // Returns a list of the given gemsCatalogSearchResults, with any SiderealTargets removed that are not
    // in the candidates list.
    private List<GemsCatalogSearchResults> filter(List<SiderealTarget> excludeCandidates,
                                                  List<GemsCatalogSearchResults> gemsCatalogSearchResults) {
        List<GemsCatalogSearchResults> results = new ArrayList<>();
        for (GemsCatalogSearchResults in : gemsCatalogSearchResults) {
            List<SiderealTarget> siderealTargets = new ArrayList<>(in.results().size());
            siderealTargets.addAll(in.resultsAsJava());
            siderealTargets = removeAll(siderealTargets, excludeCandidates);
            if (!siderealTargets.isEmpty()) {
                GemsCatalogSearchResults out = new GemsCatalogSearchResults(siderealTargets, in.criterion());
                results.add(out);
            }
        }
        return results;
    }

    // Removes all the objects in the targets list that are also in the excludeCandidates list by comparing names
    private List<SiderealTarget> removeAll(List<SiderealTarget> targets, List<SiderealTarget> excludeCandidates) {
        return targets.stream().filter(siderealTarget -> !contains(excludeCandidates, siderealTarget)).collect(Collectors.toList());
    }

    // Returns true if a SkyObject with the same name is in the list
    private boolean contains(List<SiderealTarget> targets, SiderealTarget target) {
        String name = target.name();
        if (name != null) {
            for (SiderealTarget s : targets) {
                if (name.equals(s.name())) return true;
            }
        }
        return false;
    }

    /**
     * Adds the given asterisms as guide groups to the current observation's target list.
     * @param selectedAsterisms information used to create the guide groups
     * @param primaryIndex if more than one item in list, the index of the primary guide group
     */
    public void add(List<GemsGuideStars> selectedAsterisms, int primaryIndex) {
        _worker.applyResults(selectedAsterisms, primaryIndex);
    }
}
