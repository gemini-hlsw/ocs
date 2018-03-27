package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.util.DefaultSchedulingBlock;
import edu.gemini.spModel.util.SPTreeUtil;
import edu.gemini.shared.util.immutable.Option;
import jsky.app.ot.OTOptions;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
 * Local public static final class used for adding new observation nodes to the SP tree.
 */
public final class AddObservationAction extends AbstractViewerAction implements Comparable<AddObservationAction> {

    public final Option<Instrument> instrument;

    // initialize with the component type
    public AddObservationAction(SPViewer viewer, Option<Instrument> inst) {
        super(viewer, inst.map(i -> i.componentType.readableStr).getOrElse("Empty") + " Observation");
        this.instrument = inst;
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            ISPNode node = viewer.getNode();
            ISPObservationContainer c;
            if (node == null) {
                c = viewer.getProgram();
            } else {
                c = SPTreeUtil.findObservationContainer(node);
            }
            if (c != null) {
                ISPProgram prog = c.getProgram();
                ISPObservation o = viewer.getFactory().createObservation(prog, instrument, null);

                // Set an instrument specific title
                final ISPDataObject dataObject = o.getDataObject();
                dataObject.setTitle(String.format("%sObservation", instrument.map(i -> i.componentType.readableStr + " ").getOrElse("")));
                o.setDataObject(dataObject);

                // Add a default scheduling block.
                final SPObservation spo = (SPObservation) o.getDataObject();
                spo.setSchedulingBlock(ImOption.apply(DefaultSchedulingBlock.forProgram(c)));
                o.setDataObject(spo);

                c.addObservation(o);
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    private static String instrumentName(Option<Instrument> inst) {
        return inst.map(i -> i.componentType.readableStr).getOrElse("");
    }

    @SuppressWarnings("NullableProblems")
    public int compareTo(AddObservationAction action) {
        return instrumentName(instrument).compareToIgnoreCase(instrumentName(action.instrument));
    }

    @Override
    public boolean computeEnabledState() {
        return OTOptions.isProgramEditable(getProgram());
    }

}
