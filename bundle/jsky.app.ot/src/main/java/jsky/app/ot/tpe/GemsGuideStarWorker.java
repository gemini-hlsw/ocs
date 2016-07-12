package jsky.app.ot.tpe;

import edu.gemini.ags.gems.*;
import edu.gemini.ags.gems.mascot.Strehl;
import edu.gemini.ags.gems.mascot.MascotProgress;
import edu.gemini.catalog.votable.CatalogException;
import edu.gemini.catalog.votable.ConeSearchBackend;
import edu.gemini.pot.ModelConverters;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.SkyCoordinates;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.spModel.core.Angle$;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.gems.GemsInstrument;
import edu.gemini.spModel.gems.GemsTipTiltMode;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import jsky.coords.WorldCoords;
import jsky.util.gui.SwingWorker;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ProgressPanel;
import jsky.util.gui.StatusLogger;

import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * OT-36: Automate Gems guide star selection in background thread.
 * Also used for OT-111: GemsGuideStarSearchDialog.
 * Contains static methods to perform Gems guide star selection without a UI or separate thread.
 */
public class GemsGuideStarWorker extends SwingWorker implements MascotProgress {

    private TpeImageWidget tpe;
    private boolean interrupted;

    // Displays messages during background tasks
    private StatusLogger statusLogger;

    private final scala.concurrent.ExecutionContext ec;

    // Thrown if no tiptilt or flexure stars are found
    public static class NoStarsException extends RuntimeException {
        private NoStarsException(String message) {
            super(message);
        }
    }

    /**
     * Create an instance for one time use.
     *
     * @param statusLogger shows feedback panel during background tasks
     */
    public GemsGuideStarWorker(StatusLogger statusLogger, scala.concurrent.ExecutionContext ec) {
        init(statusLogger);
        this.ec = ec;
    }

    /**
     * Create an instance for one time use.
     */
    public GemsGuideStarWorker(scala.concurrent.ExecutionContext ec) {
        ProgressPanel progressPanel = ProgressPanel.makeProgressPanel("GeMS Strehl Calculations",
                TpeManager.create().getImageWidget());
        progressPanel.addActionListener(e -> interrupted = true);
        init(progressPanel);
        this.ec = ec;
    }

    /**
     * Called by constructors
     */
    public void init(StatusLogger statusLogger) {
        this.statusLogger = statusLogger;
        this.tpe = TpeManager.create().getImageWidget();

        statusLogger.logMessage("Finding best asterisms...");
    }

    public Object construct() {
        try {
            return findGuideStars(ec);
        } catch (Exception e) {
            return e;
        }
    }

    public void finished() {
        tpe.setGemsGuideStarWorkerFinished();
        Object o = getValue();
        if (o instanceof CancellationException) {
            DialogUtil.message("The guide star search was canceled.");
        } else if (o instanceof NoStarsException) {
            // In this case, no valid asterisms were found, so clear the selection.
            clearResults();
            DialogUtil.error(((NoStarsException) o).getMessage());
        } else if (o instanceof CatalogException) {
            DialogUtil.error(((CatalogException) o).firstMessage());
        } else if (o instanceof Exception) {
            DialogUtil.error((Exception) o);
        } else {
            GemsGuideStars gemsGuideStars = (GemsGuideStars) o;
            applyResults(gemsGuideStars);
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        interrupted = true;
    }

    public void setInterrupted() {
        statusLogger.interrupt();
        statusLogger.stop();
        interrupted = true;
    }

    /**
     * AGS failed to run, so clear the AGS results.
     */
    private void clearResults() {
        apply(tpe.getContext(), 0.0, GuideGroup.AutomaticInitial());
    }

    /**
     * Used in automatic search.
     * Apply the results of the guide star search to the current observation,
     * setting the selected position angle and guide probe.
     */
    private void applyResults(GemsGuideStars gemsGuideStars) {
        final edu.gemini.spModel.core.Angle posAngle;
        posAngle = Angle$.MODULE$.fromDegrees(gemsGuideStars.pa().toDegrees());
        apply(tpe.getContext(), gemsGuideStars.pa().toDegrees(), GuideGroup.createActive(gemsGuideStars.guideGroup().getAll(), posAngle));
    }

    /**
     * Used in automatic search.
     */
    private static void apply(final TpeContext ctx, final double paInDegrees, final GuideGroup autoGroup) {
        final SPInstObsComp inst = ctx.instrument().orNull();
        if (inst != null) {
            inst.setPosAngleDegrees(paInDegrees);
            ctx.instrument().commit();
        }

        final TargetObsComp targetObsComp = ctx.targets().orNull();
        if (targetObsComp != null) {
            final TargetEnvironment envOld = targetObsComp.getTargetEnvironment();
            final GuideEnvironment gEnvOld = envOld.getGuideEnvironment();

            final GuideEnvironment gEnvNew = gEnvOld.setGroup(0, autoGroup).setPrimaryIndex(0);
            final TargetEnvironment envNew = envOld.setGuideEnvironment(gEnvNew);
            targetObsComp.setTargetEnvironment(envNew);
            ctx.targets().commit();
        }
    }

    /**
     * Used in manual search.
     * Apply the results of the guide star search to the current observation,
     * setting the selected position angle and guide probes.
     *
     * @param selectedAsterisms information used to create the guide groups
     * @param primaryIndex      if more than one item in list, the index of the primary guide group
     */
    public void applyResults(final List<GemsGuideStars> selectedAsterisms, final int primaryIndex) {
        applyResults(tpe.getContext(), selectedAsterisms, primaryIndex);
    }

    private static void applyResults(final TpeContext ctx, final List<GemsGuideStars> selectedAsterisms, final int primaryIndex) {
        final TargetObsComp targetObsComp = ctx.targets().orNull();
        if (targetObsComp != null) {
            final TargetEnvironment env = targetObsComp.getTargetEnvironment();
            final List<GuideGroup> guideGroupList = new ArrayList<>();
            int i = 0;
            for (final GemsGuideStars gemsGuideStars : selectedAsterisms) {
                // TODO: Is this condition correct? i++ == primaryIndex instead?
                if (i++ == 0) {
                    // Set position angle only for first (primary) group
                    final SPInstObsComp inst = ctx.instrument().orNull();
                    if (inst != null) {
                        inst.setPosAngle(gemsGuideStars.pa().toDegrees());
                        ctx.instrument().commit();
                    }
                }
                guideGroupList.add(gemsGuideStars.guideGroup());
            }

            // Note that the index is off by 1 to account for the auto group.
            final GuideEnvironment   geOrig     = env.getGuideEnvironment();
            final ImList<GuideGroup> gemsGroups = DefaultImList.create(guideGroupList);
            final Option<GuideGroup> autoGpOpt  = geOrig.getGroup(0);

            final ImList<GuideGroup> newGroups  = autoGpOpt.map(autoGp ->
                    DefaultImList.create(autoGp).append(gemsGroups)
            ).getOrElse(gemsGroups);

            // If there WAS an index, the primary is now off by 1.
            final int idx = primaryIndex + (autoGpOpt.isDefined() ? 1 : 0);

            if (guideGroupList.size() == 0) {
                targetObsComp.setTargetEnvironment(env.setGuideEnvironment(env.getGuideEnvironment().setOptions(
                        newGroups)));
            } else {
                targetObsComp.setTargetEnvironment(env.setGuideEnvironment(env.getGuideEnvironment().setOptions(
                        newGroups).setPrimaryIndex(idx)));
            }
            ctx.targets().commit();
        }
    }

    /**
     * Returns a set of position angles to use for the search, including the current one
     * used in the given obsContext.
     *
     * @param obsContext used to getthe current pos angle
     */
    private static Set<edu.gemini.spModel.core.Angle> getPosAngles(ObsContext obsContext) {
        final Set<edu.gemini.spModel.core.Angle> posAngles = new TreeSet<>((a1, a2) -> Double.compare(a1.toDegrees(), a2.toDegrees()));

        posAngles.add(ModelConverters.toNewAngle(obsContext.getPositionAngle()));
        posAngles.add(ModelConverters.toNewAngle(new Angle(0., Angle.Unit.DEGREES)));
        posAngles.add(ModelConverters.toNewAngle(new Angle(90., Angle.Unit.DEGREES)));
        posAngles.add(ModelConverters.toNewAngle(new Angle(180., Angle.Unit.DEGREES)));
        posAngles.add(ModelConverters.toNewAngle(new Angle(270., Angle.Unit.DEGREES)));
        return posAngles;
    }

    /**
     * Do the catalog part of the search using the given settings and return the results.
     *
     * @return catalog search results
     */
    public List<GemsCatalogSearchResults> search(GemsGuideStarSearchOptions.CatalogChoice catalog, GemsTipTiltMode tipTiltMode,
                                                 ObsContext obsContext, Set<edu.gemini.spModel.core.Angle> posAngles,
                                                 scala.Option<MagnitudeBand> nirBand,
                                                 scala.concurrent.ExecutionContext ec) throws Exception {
        try {
            interrupted = false;
            startProgress();

            final List<GemsCatalogSearchResults> results = searchUnchecked(catalog, tipTiltMode, obsContext, posAngles, nirBand, ec);
            if (interrupted) {
                throw new CancellationException("Canceled");
            }

            checkResults(results);
            return results;
        } finally {
            stopProgress();
            interrupted = false;
        }
    }

    private static List<GemsCatalogSearchResults> searchUnchecked(GemsGuideStarSearchOptions.CatalogChoice catalog, GemsTipTiltMode tipTiltMode,
                                                         ObsContext obsContext, Set<edu.gemini.spModel.core.Angle> posAngles,
                                                         scala.Option<MagnitudeBand> nirBand,
                                                         scala.concurrent.ExecutionContext ec) throws Exception {
        Coordinates basePos = obsContext.getBaseCoordinates().getOrNull();
        Angle baseRA = new Angle(basePos.getRaDeg(), Angle.Unit.DEGREES);
        Angle baseDec = new Angle(basePos.getDecDeg(), Angle.Unit.DEGREES);
        SkyCoordinates base = new HmsDegCoordinates.Builder(baseRA, baseDec).build();

        SPInstObsComp inst = obsContext.getInstrument();

        GemsInstrument instrument = inst instanceof Flamingos2 ? GemsInstrument.flamingos2 : GemsInstrument.gsaoi;
        GemsGuideStarSearchOptions options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles);

        return new GemsVoTableCatalog(ConeSearchBackend.instance(), catalog.catalog()).search4Java(obsContext, ModelConverters.toCoordinates(base), options, nirBand, 30, ec);
    }


    /**
     * Returns the set of Gems guide stars with the highest ranking using the default settings.
     */
    private GemsGuideStars findGuideStars(scala.concurrent.ExecutionContext ec) throws Exception {
        final WorldCoords basePos = tpe.getBasePos();
        final ObsContext obsContext = getObsContext(basePos.getRaDeg(), basePos.getDecDeg());
        final Set<edu.gemini.spModel.core.Angle> posAngles = getPosAngles(obsContext);
        final List<GemsCatalogSearchResults> results = search(GemsGuideStarSearchOptions.DEFAULT, GemsTipTiltMode.canopus, obsContext, posAngles,
                scala.Option.empty(), ec);
        return findGuideStars(obsContext, posAngles, results);
    }

    /**
     * Returns the set of Gems guide stars with the highest ranking using the given settings
     */
    private GemsGuideStars findGuideStars(ObsContext obsContext, Set<edu.gemini.spModel.core.Angle> posAngles,
                                         List<GemsCatalogSearchResults> results) throws Exception {

        interrupted = false;
        try {
            startProgress();
            List<GemsGuideStars> gemsResults = GemsResultsAnalyzer.instance().analyze(obsContext, posAngles, results, new scala.Some<>(this));
            if (interrupted && gemsResults.size() == 0) {
                throw new CancellationException("Canceled");
            }
            interrupted = false;
            if (gemsResults.size() > 0) return gemsResults.get(0);
            throw new NoStarsException("No guide stars were found");
        } finally {
            stopProgress();
            interrupted = false;
        }
    }

    /**
     * Returns a list of all possible Gems guide star sets.
     */
    public List<GemsGuideStars> findAllGuideStars(ObsContext obsContext, Set<edu.gemini.spModel.core.Angle> posAngles,
                                                  List<GemsCatalogSearchResults> results) throws Exception {
        interrupted = false;
        try {
            startProgress();
            List<GemsGuideStars> gemsResults = GemsResultsAnalyzer.instance().analyze(obsContext, posAngles, results, new scala.Some<>(this));
            if (interrupted && gemsResults.size() == 0) {
                throw new CancellationException("Canceled");
            }
            return filterByPosAngle(gemsResults);
        } finally {
            stopProgress();
            interrupted = false;
        }
    }

    // OT-111: The candidate asterisms table should present only the options with the same position
    // angle as the "best" candidate.
    private static List<GemsGuideStars> filterByPosAngle(final List<GemsGuideStars> gemsGuideStarsList) {
        if (gemsGuideStarsList.size() == 0) {
            return gemsGuideStarsList;
        }
        final edu.gemini.spModel.core.Angle positionAngle = gemsGuideStarsList.get(0).pa();

        return gemsGuideStarsList.stream().collect(ArrayList<GemsGuideStars>::new, (alst, ggs) -> {
            if (positionAngle.equals(ggs.pa())) alst.add(ggs);
        }, ArrayList::addAll);
    }


    // Checks that the results are valid: There must be at least 1 valid tiptilt and flexure star each
    private static void checkResults(List<GemsCatalogSearchResults> results) {
        final Map<String, Boolean> keyMap = new HashMap<>();
        for (final GemsCatalogSearchResults searchResults : results) {
            final String key = searchResults.criterion().key().group().getKey();
            if (searchResults.results().size() != 0) {
                keyMap.put(key, true);
            } else if (!keyMap.containsKey(key)) {
                keyMap.put(key, false);
            }
        }
        for (final String key : keyMap.keySet()) {
            if (!keyMap.get(key)) {
                throw new NoStarsException("No "
                        + key
                        + " guide stars were found");
            }
        }
    }

    // Returns a copy of the ObsContext with the target list removed, since the
    // existing targets affect validation.
    public ObsContext getObsContext(final double raDeg, final double decDeg) {
        final Option<ObsContext> obsContextOpt = tpe.getObsContext();
        if (obsContextOpt.isEmpty())
            throw new RuntimeException("Please select an observation");
        final ObsContext obsContext = obsContextOpt.getValue();
        return obsContext.withTargets(TargetEnvironment.create(new SPTarget(raDeg, decDeg)));
    }

    @Override
    public boolean progress(Strehl s, int count, int total, boolean usable) {
        statusLogger.setProgress((int) ((double) count / (double) total * 100));

        String msg = "Asterism #" + count + " of " + total
                + String.format(",  average Strehl: %.1f", s.avgstrehl() * 100)
                + (usable ? ": OK" : ": -");

        statusLogger.logMessage(msg);
        return !interrupted;
    }

    @Override
    public void setProgressTitle(String s) {
        if (statusLogger instanceof ProgressPanel) {
            ((ProgressPanel) statusLogger).setTitle(s);
        } else {
            statusLogger.logMessage(s);
        }
    }

    private void startProgress() {
        statusLogger.start();
    }

    private void stopProgress() {
        statusLogger.stop();
    }
}
