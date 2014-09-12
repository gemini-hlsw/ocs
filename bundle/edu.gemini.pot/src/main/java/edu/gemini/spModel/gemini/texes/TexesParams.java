package edu.gemini.spModel.gemini.texes;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SequenceableSpType;


/**
 * This class provides data types for the Texes components.
 */
public final class TexesParams {

    // Make the constructor private.
    private TexesParams() {
    }

    /**
     * Class for Disperser.
     */
    public static enum Disperser implements SequenceableSpType, DisplayableSpType {

        E_D_32_LMM("Echelon + 32 l/mm echelle", 4.0),
        E_D_75_LMM("Echelon + 75 l/mm grating", 1.7),
        D_32_LMM("32 l/mm echelle", 4.0),
        D_75_LMM("75 l/mm grating", 1.7);

        public static final double SLIT_LENGTH = 0.5;

        private final double _slit_width;
        private String _displayValue;

        private Disperser(String name, double slit_witdth) {
            _displayValue = name;
            _slit_width = slit_witdth;
        }

        /** The default Disperser value **/
        public static Disperser DEFAULT = Disperser.D_32_LMM;

        public double getSlitWidth() {
            return _slit_width;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String toString() {
            return displayValue();
        }

        public String sequenceValue() {
            return name();
        }
    }

}
