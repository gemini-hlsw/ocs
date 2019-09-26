package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.ags.gems.GemsCandidates;
import edu.gemini.ags.gems.GemsCandidates$;
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay;
import edu.gemini.pot.ModelConverters;
import edu.gemini.skycalc.Angle;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.obs.context.ObsContext;
import jsky.app.ot.tpe.GemsGuideStarWorker;
import jsky.app.ot.tpe.TpeImageWidget;
import jsky.app.ot.tpe.TpeManager;
import jsky.coords.WorldCoords;
import jsky.util.gui.DialogUtil;

import java.util.ArrayList;
import java.util.Collections;
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

        List<SiderealTarget> candidates;
        try {
            candidates = _worker.search(_model.getCatalog(), scala.Option.empty(), obsContext, ec);
        } catch (final Exception e) {
            DialogUtil.error(_dialog, e);
            candidates = Collections.<SiderealTarget>emptyList();
            _dialog.setState(GemsGuideStarSearchDialog.State.PRE_QUERY);
        }

        _model.setCandidates(obsContext, posAngles, candidates);

        if (!_model.isReviewCandidatesBeforeSearch()) {
            _model.setGemsGuideStars(_worker.findAllGuideStars(obsContext, posAngles, candidates));
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
            new ArrayList<>(_model.getAllCandidates());

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
        final int                  primaryIndex
    ) {
        _worker.applyResults(selectedAsterisms, primaryIndex);
    }
}
