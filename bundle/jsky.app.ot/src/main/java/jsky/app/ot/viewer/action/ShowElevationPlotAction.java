package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.spModel.core.Site;
import jsky.app.ot.plot.ObsTargetDesc;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.userprefs.observer.ObservingSite;
import jsky.app.ot.viewer.SPElevationPlotPlugin;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.SPViewerActions;
import jsky.plot.TargetDesc;
import jsky.plot.ElevationPlotManager;
import jsky.util.gui.DialogUtil;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public final class ShowElevationPlotAction extends AbstractViewerAction {

    public ShowElevationPlotAction(SPViewer viewer) {
        super(viewer, "Elevation Plot", jsky.util.Resources.getIcon("Plot24.gif", SPViewerActions.class));
        putValue(SHORT_NAME, "Plot");
        putValue(SHORT_DESCRIPTION, "Show an elevation plot for the observation's base position.");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            final ISPObservation[] obs = viewer.getSelectedObservations();
            if (obs == null || obs.length == 0) {
                DialogUtil.error(
                        "Please select one or more observations or the program node and then the Plot button");
            } else {
                showElevationPlot(obs);
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    // Display an elevation plot for the selected observations
    private void showElevationPlot(final ISPObservation[] obs) {
        SPElevationPlotPlugin plugin = SPElevationPlotPlugin.getInstance();
        plugin.setSelectedObservations(obs);
        Pair<TargetDesc[], ISPObservation[]> pair = getTargets(obs, plugin.useTargetName());
        TargetDesc[] targets = pair._1();
        ISPObservation[] ignoredObs = pair._2();

        Site site = Site.GN;
        final Site preferredSite = ObservingSite.getOrNull();
        if (preferredSite != null) site = preferredSite;

        ElevationPlotManager.show(targets, site, plugin);
        ignoredObsWarning(ignoredObs); // REL-371

        // Redisplay elevation plot if plugin settings change
        plugin.setChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                try {
                    showElevationPlot(obs);
                } catch (Exception ex) {
                    DialogUtil.error(ex);
                }
            }
        });
    }


    // REL-371: Displays a warning message if observations were ignored because they did not contain target lists
    private void ignoredObsWarning(ISPObservation[] ignoredObs) {
        if (ignoredObs.length != 0) {
            final StringBuilder sb = new StringBuilder();
            if (ignoredObs.length > 5) {
                sb.append(ignoredObs.length).append(" observations were ignored because they do not contain target lists");
            } else {
                sb.append("<html>The following observations were ignored because they do not contain a target list:<br/><ul> ");
                int i = 0;
                for(ISPObservation obs: ignoredObs) {
                    sb.append("<li>");
                    sb.append(obs.getObservationIDAsString("Obs" + obs.getObservationNumber()));
                    sb.append("</li>");
                    if (++i > 5) {
                        sb.append("</ul>and ").append(ignoredObs.length - i).append(" others...");
                        break;
                    }
                }
                sb.append("</ul>");
            }
            DialogUtil.message(sb.toString());
        }
    }


    // Returns a pair containing an array of targets and an array of observations with no targets, given an array of observations.
    private Pair<TargetDesc[], ISPObservation[]> getTargets(ISPObservation[] obs, boolean useTargetName) {
        final ArrayList<TargetDesc> result = new ArrayList<TargetDesc>();
        final ArrayList<ISPObservation> ignored = new ArrayList<ISPObservation>();
        for (ISPObservation ob : obs) {
            final TargetDesc targetDesc = ObsTargetDesc.getTargetDesc(viewer.getDatabase(), ob, useTargetName);
            if (targetDesc != null)
                result.add(targetDesc);
            else
                ignored.add(ob);
        }
        final TargetDesc[] targets = new TargetDesc[result.size()];
        result.toArray(targets);
        final ISPObservation[] ignoredObs = new ISPObservation[ignored.size()];
        ignored.toArray(ignoredObs);
        return new Pair<TargetDesc[], ISPObservation[]>(targets, ignoredObs);
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        // Enabled if at least one observation has a target component.
        final ISPObservation[] obsArray = viewer.getSelectedObservations();
        if (obsArray != null) {
            for (ISPObservation obs : obsArray) {
                if (SPTreeUtil.findTargetEnvNode(obs) != null) return true;
            }
        }
        return false;
    }

}
