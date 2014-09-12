package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obsrecord.ObsExecStatus;
import jsky.app.ot.OTOptions;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

public final class ExecStatusAction extends AbstractViewerAction {
    public ExecStatusAction(SPViewer viewer) {
        super(viewer, "Set Exec Status...");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            final String[] choices = new String[ObsExecStatus.values().length + 1];
            choices[0] = "Automatically Set";
            int i = 1;
            for (ObsExecStatus s : ObsExecStatus.values()) choices[i++] = s.displayValue();
            final String sel =
                (String) DialogUtil.input(viewer,
                    "Set the execution status for all of the selected observations to:",
                     choices, choices[0]);
            if (sel != null) {
                Option<ObsExecStatus> over = None.instance();
                for (ObsExecStatus s : ObsExecStatus.values()) {
                    if (s.displayValue().equals(sel)) {
                        over = new Some<ObsExecStatus>(s);
                        break;
                    }
                }
                for (ISPObservation o : viewer.getSelectedObservations()) {
                    final SPObservation obs = (SPObservation) o.getDataObject();
                    obs.setExecStatusOverride(over);
                    o.setDataObject(obs);
                }
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        final ISPProgram p = viewer.getProgram();
        if (p == null) return false;

        final SPProgramID pid = p.getProgramID();
        if (pid == null) return false;

        if (!OTOptions.isStaff(pid)) return false;

        final ISPObservation[] os = viewer.getSelectedObservations();
        if (os == null || os.length == 0) return false;

        for (ISPObservation o : os) {
            final SPObservation obs = (SPObservation) o.getDataObject();
            if (obs.getPhase2Status() != ObsPhase2Status.PHASE_2_COMPLETE) return false;
        }

        return true;
    }

}
