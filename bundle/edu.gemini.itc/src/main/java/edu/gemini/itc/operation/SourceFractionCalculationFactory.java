package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.shared.Instrument;

public final class SourceFractionCalculationFactory {

    private SourceFractionCalculationFactory() {
    }

    public static SourceFractionCalculatable getCalculationInstance(
            SourceDefinitionParameters sourceDefinitionParameters,
            ObservationDetailsParameters observationDetailsParameters,
            Instrument instrument) {

        String ap_type = observationDetailsParameters.getApertureType();
        double ap_diam =
                observationDetailsParameters.getApertureDiameter();

        //Case A if a point Source or a Gaussian use the same code
        // Creates a PointSourceFractionCalculation object

        if (sourceDefinitionParameters.getSourceGeometry().
                equals(SourceDefinitionParameters.POINT_SOURCE) ||
                sourceDefinitionParameters.getExtendedSourceType().
                        equals(SourceDefinitionParameters.GAUSSIAN))

            return new PointSourceFractionCalculation(
                    ap_type,
                    ap_diam,
                    instrument.getPixelSize());

            // Case B if sdParams.getExtendedSourceType = UNIFORM
            // This means the User has selected USB Calc
        else
            return new USBSourceFractionCalculation(
                    ap_type,
                    ap_diam,
                    instrument.getPixelSize());
    }
}

