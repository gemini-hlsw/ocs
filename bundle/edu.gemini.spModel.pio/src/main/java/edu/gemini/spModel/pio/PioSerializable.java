package edu.gemini.spModel.pio;

/**
 * Marks an object as serializable to a PIO ParamSet and provides the method
 * to do so.
 */
public interface PioSerializable {
    ParamSet getParamSet(PioFactory factory, String name);
}
