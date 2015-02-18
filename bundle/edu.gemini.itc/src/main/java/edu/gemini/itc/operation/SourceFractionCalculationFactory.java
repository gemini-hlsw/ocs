package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.shared.AnalysisMethod;
import edu.gemini.itc.shared.AutoAperture;
import edu.gemini.itc.shared.Instrument;

public final class SourceFractionCalculationFactory {

    private SourceFractionCalculationFactory() {
    }

    public static SourceFractionCalculatable getCalculationInstance(
            SourceDefinitionParameters sourceDefinitionParameters,
            ObservationDetailsParameters observationDetailsParameters,
            Instrument instrument) {

        AnalysisMethod ap_type = observationDetailsParameters.getAnalysis();
        double ap_diam = observationDetailsParameters.getApertureDiameter();


        if (sourceDefinitionParameters.isUniform()) {
            // Case B if sdParams.getExtendedSourceType = UNIFORM
            // This means the User has selected USB Calc
            return new USBSourceFractionCalculation(
                    ap_type instanceof AutoAperture,
                    ap_diam,
                    instrument.getPixelSize());
        } else {
            //Case A if a point Source or a Gaussian use the same code
            // Creates a PointSourceFractionCalculation object
            return new PointSourceFractionCalculation(
                    ap_type instanceof AutoAperture,
                    ap_diam,
                    instrument.getPixelSize());
        }

    }
}

