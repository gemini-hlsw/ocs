package edu.gemini.spModel.telescope;


import edu.gemini.shared.util.immutable.ImList;

/**
 * An interface for instruments that support position angle constraints.
 *
 * NOTE that an instrument that implements this interface MUST have an entry in the PosAngleConstraintRegistrar
 *      giving the PosAngleConstraints that this instrument supports. This is done because we need this information
 *      statically, which Java does not allow due to no static method overriding.
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
