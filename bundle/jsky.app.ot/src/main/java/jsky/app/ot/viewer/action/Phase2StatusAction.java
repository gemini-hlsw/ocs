package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.obs.ObsPhase2Status;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

public final class Phase2StatusAction extends AbstractViewerAction {

    public Phase2StatusAction(SPViewer viewer) {
        super(viewer, "Set Phase 2 Status...");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            final ISPObservation[] obs = viewer.getSelectedObservations();
            final ObsPhase2Status[] choices = viewer.getObsStatusChoices(obs);
            final ObsPhase2Status status =
                    (ObsPhase2Status) DialogUtil.input(viewer,
                            "Set the phase 2 status for all of the selected observations to:",
                            choices,
                            choices[0]);
            if (status != null) {
                viewer.setPhase2Status(status); // TODO: remove this logic from the viewer
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        final ISPObservation[] os = viewer.getSelectedObservations();
        if (os != null && os.length > 0) {
            final ObsPhase2Status[] choices = viewer.getObsStatusChoices(os);
            return choices != null && choices.length > 0;
        }
        return false;
    }

}
