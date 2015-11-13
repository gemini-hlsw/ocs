package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.AutoAperture;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.itc.shared.UserAperture;

public final class SourceFractionFactory {

    private SourceFractionFactory() {}

    public static SourceFraction calculate(
            final SourceDefinition sdp,
            final ObservationDetails odp,
            final Instrument instrument,
            final double im_qual) {

        if (odp.analysisMethod instanceof AutoAperture) {
            return calculate(
                    sdp.isUniform(),
                    true,
                    0.0,
                    instrument.getPixelSize(),
                    im_qual);
        } else if (odp.analysisMethod instanceof UserAperture){
            return calculate(
                    sdp.isUniform(),
                    false,
                    (((UserAperture) odp.analysisMethod).diameter()),
                    instrument.getPixelSize(),
                    im_qual);
        } else {
            throw new Error("Unsupported analysis method");
        }
    }

    public static SourceFraction calculate(
            final boolean isUniform,
            final boolean isAuto,
            final double ap_diam,
            final double pixSize,
            final double im_qual) {


        if (isUniform) {
            if (isAuto) {
                return new USBSourceFraction(pixSize);
            } else {
                return new USBSourceFraction(ap_diam, pixSize);
            }
        } else {
            if (isAuto) {
                return new PointSourceFraction(pixSize, im_qual);
            } else {
                return new PointSourceFraction(ap_diam, pixSize, im_qual);
            }
        }

    }
}

