package jsky.app.ot.gemini.obslog;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.obs.InstrumentService;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obslog.ObsQaLog;
import jsky.app.ot.editor.OtItemEditor;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Logger;

/**
 * An ObsLog editor component.
 * @author rnorris (then hacked by swalker)
 */
public class EdCompObslog extends OtItemEditor<ISPObsQaLog, ObsQaLog> {
    private static final Logger LOG = Logger.getLogger(EdCompObslog.class.getName());

    private final ObslogGUI gui = new ObslogGUI();
    private ObsQaLog currentLog;
    private Map<DatasetLabel,String> originalComments; // original comments, so we can tell what is dirty
    private ISPNode prev;

    // Watches for changes to the ObsExecLog so that it can update the GUI,
    // showing the latest datasets that have arrived, etc.
    private final PropertyChangeListener execLogListener = evt -> {
        if (SPUtil.getDataObjectPropertyName().equals(evt.getPropertyName())) {
            SwingUtilities.invokeLater(() -> {
                final ISPObsExecLog execLogShell = (ISPObsExecLog) evt.getSource();
                final Option<ISPObservation> obs = ImOption.apply(execLogShell.getContextObservation());
                final Option<Instrument>    inst = obs.flatMap(o -> InstrumentService.lookupInstrument(o));
                final ObsClass                oc = obs.map(o -> ObsClassService.lookupObsClass(o)).getOrElse(ObsClass.SCIENCE);
                gui.setup(inst, oc, new ObsLog(getNode(), getDataObject(), execLogShell, (ObsExecLog) evt.getNewValue()));
            });
        }
    };

    protected void updateEnabledState(boolean enabled) {
        // Do nothing. I will control my own enabled state, thank you.
    }

    public JPanel getWindow() {
        return gui;
    }

    // Forwards events as property change events so that updates are stored
    // in the ISPObsQaLog shell.
    private final ObsQaLog.Listener listener = new ObsQaLog.Listener() {
        @Override public void datasetQaUpdate(ObsQaLog.Event event) {
            currentLog.firePropertyChange(new PropertyChangeEvent(event.getSource(), event.newRec.label.toString(), event.oldRec, event.newRec));
        }
    };

    @Override protected void init() {
        final ISPObservation obs = getContextObservation();
        if (obs == null) {
            // This can happen if the obslog is created locally in the OT, for
            // example when re-importing a pre-2014A program from XML and winds
            // up in the conflict folder.
            LOG.warning("There is no context observation for the observing log.");
            return;
        }

        final ISPObsExecLog oel = obs.getObsExecLog();
        if (oel == null) {
            LOG.warning("There is an Obs QA Log but no Obs Exec Log!");
            return;
        }

        // Add a listener to watch for changes to the ObsExecLog, being sure not
        // to add it multiple times to the same node.
        oel.removePropertyChangeListener(SPUtil.getDataObjectPropertyName(), execLogListener);
        oel.addPropertyChangeListener(SPUtil.getDataObjectPropertyName(), execLogListener);
        final ObsExecLog execLog = (ObsExecLog) oel.getDataObject();

        final ObsQaLog incomingLog = getDataObject();
        final boolean preserveEdits = prev != null && prev.getNodeKey() == getNode().getNodeKey() &&
                                      getNode().getConflicts().dataObjectConflict.isEmpty();
        prev = getNode();

        // It is actually quite common for the framework to call foo.setDataObject(foo.getDataObject()),
        // which isn't a meaningful event for this component. So that case is simply ignored.
        if (currentLog != incomingLog) {
            if (currentLog != null) currentLog.removeDatasetQaRecordListener(listener);

            final Collection<DatasetLabel> labels = execLog.getRecord().getDatasetLabels();

            // Get the initial state of comments so we can later determine what has changed
            final Map<DatasetLabel,String> incomingBaseline = comments(incomingLog, labels);

            // Resolve comment collisions. If preserveEdits is true, currentLog cannot be null.
            if (preserveEdits) {
                assert((currentLog != null) && (originalComments != null));
                for (DatasetLabel lab : labels) {
                    final String currentComment  = currentLog.getComment(lab);
                    final String incomingComment = incomingLog.getComment(lab);
                    final String resolved        = resolveComment(originalComments.get(lab), currentComment, incomingComment);
                    incomingLog.setComment(lab, resolved);
                }
            }

            // Done. Update our state and notify the GUI.
            currentLog       = incomingLog;
            currentLog.addDatasetQaRecordListener(listener);
            originalComments = incomingBaseline;

            final Option<Instrument> inst = InstrumentService.lookupInstrument(obs);
            final ObsClass             oc = ObsClassService.lookupObsClass(obs);
            gui.setup(inst, oc, new ObsLog(getNode(), incomingLog, oel, execLog));
        }
    }

    @Override protected void cleanup() {
        final ISPObservation obs = getContextObservation();
        if (obs != null) {
            final ISPObsExecLog oel = obs.getObsExecLog();
            if (oel == null) return;
            oel.removePropertyChangeListener(SPUtil.getDataObjectPropertyName(), execLogListener);
        }
    }

    private static Map<DatasetLabel, String> comments(ObsQaLog qa, Collection<DatasetLabel> labels) {
        final Map<DatasetLabel,String> res = new HashMap<>(Math.round(labels.size() / 0.75f));
        for (DatasetLabel lab : labels) res.put(lab, qa.getComment(lab));
        return res;
    }

    private String resolveComment(String original, String current, String incoming) {
        // Originally this was kind of a complex set of rules, but it turns out
        // that a simple rule works fine. Just use whichever one is newer. That
        // is, if we have unsaved edits, keep them. Otherwise use the incoming value.
        return equiv(original, current) ? incoming : current;
    }

    private boolean equiv(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
}
