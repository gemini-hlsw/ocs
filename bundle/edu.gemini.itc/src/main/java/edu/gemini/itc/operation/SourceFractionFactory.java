package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.SourceDefinition;

public final class SourceFractionFactory {

    private SourceFractionFactory() {}

    public static SourceFraction calculate(
            final SourceDefinition sdp,
            final ObservationDetails odp,
            final Instrument instrument,
            final double im_qual) {

        return calculate(
                sdp.isUniform(),
                odp.isAutoAperture(),
                odp.getApertureDiameter(),
                instrument.getPixelSize(),
                im_qual);
    }

    public static SourceFraction calculate(
            final boolean isUniform,
            final boolean isAuto,
            final double ap_diam,
            final double pixSize,
            final double im_qual) {


        if (isUniform) {
            // Case B if sdParams.getExtendedSourceType = UNIFORM
            // This means the User has selected USB Calc
            return new USBSourceFraction(
                    isAuto,
                    ap_diam,
                    pixSize);
        } else {
            //Case A if a point Source or a Gaussian use the same code
            // Creates a PointSourceFractionCalculation object
            return new PointSourceFraction(
                    isAuto,
                    ap_diam,
                    pixSize,
                    im_qual);
        }

    }
}

