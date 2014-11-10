package edu.gemini.spModel.telescope;


import edu.gemini.shared.util.immutable.ImList;

/**
 * An interface for instruments that support position angle constraints.
 */
public interface PosAngleConstraintAware {
    PosAngleConstraint getPosAngleConstraint();
    void setPosAngleConstraint(PosAngleConstraint pac);

    /**
     * Used to configure property change listeners.
     * @return the descriptor corresponding to the position angle constraint.
     */
    String getPosAngleConstraintDescriptorKey();

    /**
     * The list of valid position angle constraints for this instrument.
     * @return a list of the supported position angle constraints.
     */
    public ImList<PosAngleConstraint> getSupportedPosAngleConstraints();

    /**
     * Return true if this instrument and its configuration allows calculation of best position angle,
     * and false otherwise.
     */
    public boolean allowUnboundedPositionAngle();
}
