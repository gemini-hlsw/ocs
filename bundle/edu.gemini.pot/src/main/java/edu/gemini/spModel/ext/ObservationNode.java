package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.obs.SPObservation;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * An observation node and all of the objects it contains in one convinient
 * local object.
 */
public final class ObservationNode extends AbstractNodeContext<ISPObservation, SPObservation> {
    private static final long serialVersionUID = 5969850561084118010L;

    private final SPObservationID obsId;

    private final TargetNode target;
    private final InstrumentNode instrument;
    private final AoNode ao;
    private final ConstraintsNode constraints;
    private final SequenceNode sequence;

    private final Collection<NodeContext<?, ?>> children;

    private final boolean isTemplate;

    public ObservationNode(ISPObservation observation)  {
        super(observation);

        obsId = observation.getObservationID();

        ISPObsComponent targetComp      = null;
        ISPObsComponent instrumentComp  = null;
        ISPObsComponent aoComp          = null;
        ISPObsComponent constraintsComp = null;

        for (ISPObsComponent obsComp : observation.getObsComponents()) {
            // grim
            final SPComponentBroadType type = obsComp.getType().broadType;
            if (type.equals(SPComponentBroadType.INSTRUMENT)) {
                instrumentComp = obsComp;
            } else if (type.equals(SPSiteQuality.SP_TYPE.broadType)) {
                constraintsComp = obsComp;
            } else if (type.equals(TargetObsComp.SP_TYPE.broadType)) {
                targetComp = obsComp;
            } else if (type.equals(InstAltair.SP_TYPE.broadType)) {
                aoComp = obsComp;
            }
        }

        target      = targetComp == null      ? null : new TargetNode(targetComp);
        instrument  = instrumentComp == null  ? null : new InstrumentNode(instrumentComp);
        ao          = aoComp == null          ? null : new AoNode(aoComp);
        constraints = constraintsComp == null ? null : new ConstraintsNode(constraintsComp);

        final ISPSeqComponent seqComp = observation.getSeqComponent();
        if (seqComp == null) {
            sequence = null;
        } else {
            sequence = new SequenceNode(seqComp);
        }

        final Collection<NodeContext<?, ?>> c = new ArrayList<>();
        if (target != null) c.add(target);
        if (instrument != null) c.add(instrument);
        if (ao != null) c.add(ao);
        if (constraints != null) c.add(constraints);
        if (sequence != null) c.add(sequence);
        children = Collections.unmodifiableCollection(c);

        // Is this observation in a template?
        isTemplate = isInsideTemplate(observation.getParent());
    }

    private static boolean isInsideTemplate(ISPNode node)  {
        return (node != null) && ((node instanceof ISPTemplateFolder) || isInsideTemplate(node.getParent()));
    }

    public SPObservationID getObservationId() {
        return obsId;
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public TargetNode getTarget() {
        return target;
    }

    public InstrumentNode getInstrument() {
        return instrument;
    }

    public AoNode getAdaptiveOptics() {
        return ao;
    }

    public ConstraintsNode getConstraints() {
        return constraints;
    }

    public SequenceNode getSequence() {
        return sequence;
    }

    /**
     * Gets all of the child nodes in a single collection, which is useful
     * for methods that process all the nodes in a generic way.
     */
    public Collection<NodeContext<?, ?>> getChildren() {
        return children;
    }

}
