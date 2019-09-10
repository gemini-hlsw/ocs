package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.Ngs2Result;
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay;
import edu.gemini.pot.ModelConverters;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.SiderealTarget;
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
    GemsGuideStarSearchController(final GemsGuideStarSearchModel model, final GemsGuideStarWorker worker,
                                  final GemsGuideStarSearchDialog dialog, final CatalogImageDisplay imageDisplay) {
        _model = model;
        _worker = worker;
        _dialog = dialog;
        _tpe = imageDisplay;
    }

    /**
     * Searches for guide star candidates and saves the results in the model
     */
    public void query(final scala.concurrent.ExecutionContext ec) {
        final WorldCoords basePos = _tpe.getBasePos();
        final ObsContext obsContext = _worker.getObsContext(basePos.getRaDeg(), basePos.getDecDeg());
        final Set<edu.gemini.spModel.core.Angle> posAngles = getPosAngles(obsContext);

        Ngs2Result result;
        try {
            result = _worker.search(_model.getCatalog(), scala.Option.empty(), obsContext, posAngles, ec);
        } catch (final Exception e) {
            DialogUtil.error(_dialog, e);
            result = Ngs2Result.Empty();
            _dialog.setState(GemsGuideStarSearchDialog.State.PRE_QUERY);
        }

        // TODO-NGS2: This is where I am stuck for manual mode. I went further and had to undo my changes.
        // TODO-NGS2: We now have Canopus and PWFS1 SFS results stored in the NGS2Results object.
        // TODO-NGS2: We have to modify the GemsGuideStarSearchModel to accommodate them.
        // TODO-NGS2: Right now, we just drop the PWFS1 guide star and only include the Canopus guide stars.
        _model.setNGS2Result(result);
        if (!_model.isReviewCandidatesBeforeSearch()) {
            _model.setGemsGuideStars(_worker.findAllGuideStars(obsContext, posAngles, result.cwfsCandidatesAsJava()));
        }
    }

    private Set<edu.gemini.spModel.core.Angle> getPosAngles(final ObsContext obsContext) {
        final Set<edu.gemini.spModel.core.Angle> posAngles = new HashSet<>();
        posAngles.add(obsContext.getPositionAngle());
        if (_model.isAllowPosAngleAdjustments()) {
            posAngles.add(ModelConverters.toNewAngle(new Angle(  0., Angle.Unit.DEGREES)));
            posAngles.add(ModelConverters.toNewAngle(new Angle( 90., Angle.Unit.DEGREES)));
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
    void analyze(final List<SiderealTarget> excludeCandidates) {
        final TpeImageWidget    tpe = TpeManager.create().getImageWidget();
        final WorldCoords   basePos = tpe.getBasePos();
        final ObsContext obsContext = _worker.getObsContext(basePos.getRaDeg(), basePos.getDecDeg());
        final Set<edu.gemini.spModel.core.Angle> posAngles = getPosAngles(obsContext);

        final List<SiderealTarget> candidates =
            new ArrayList<>(_model.getNGS2Result().cwfsCandidatesAsJava());

        candidates.removeAll(excludeCandidates);

        _model.setGemsGuideStars(
            _worker.findAllGuideStars(obsContext, posAngles, candidates)
        );
    }

    /**
     * Adds the given asterisms as guide groups to the current observation's target list.
     * @param selectedAsterisms information used to create the guide groups
     * @param primaryIndex if more than one item in list, the index of the primary guide group
     */
    public void add(
        final List<GemsGuideStars> selectedAsterisms,
        final int                  primaryIndex,
        final SiderealTarget       slowFocusSensor
    ) {
        _worker.applyResults(selectedAsterisms, primaryIndex, slowFocusSensor);
    }
}
