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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Local public static final class used for adding new observation nodes to the SP tree.
 */
public final class AddObservationAction extends AbstractViewerAction implements Comparable<AddObservationAction> {

    private final Option<Instrument> instrument;
    private final SPComponentType componentType;
    private final Set<SPComponentType> requires;

    // initialize with the component type
    public AddObservationAction(SPViewer viewer, Option<Instrument> inst, SPComponentType type, Set<SPComponentType> requires) {
        super(viewer, (type != null ? type.readableStr : "Empty") + " Observation");
        this.instrument = inst;
        componentType = type;
        if (requires == null) {
            this.requires = Collections.emptySet();
        } else {
            this.requires = Collections.unmodifiableSet(new HashSet<>(requires));
        }
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
                if (componentType != null && componentType.broadType.equals(SPComponentBroadType.INSTRUMENT)) {
                    o.addObsComponent(viewer.getFactory().createObsComponent(prog, componentType, null));
                    if (requires != null) {
                        for (SPComponentType req : requires) {
                            o.addObsComponent(viewer.getFactory().createObsComponent(prog, req, null));
                        }
                    }

                    // Set an instrument specific title
                    ISPDataObject dataObject = o.getDataObject();
                    dataObject.setTitle(componentType.readableStr + " Observation");
                    o.setDataObject(dataObject);
                }

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

    public SPComponentType getType() {
        return componentType;
    }

    @SuppressWarnings("NullableProblems")
    public int compareTo(AddObservationAction action) {
        return componentType.readableStr.compareToIgnoreCase(action.componentType.readableStr);
    }

    @Override
    public boolean computeEnabledState() {
        return OTOptions.isProgramEditable(getProgram());
    }

}
