package edu.gemini.spModel.telescope;

/**
 * An interface for instruments that support position angle constraints.
 */
public interface PosAngleConstraintAware {
    PosAngleConstraint getPosAngleConstraint();
    void setPosAngleConstraint(PosAngleConstraint pac);
}
