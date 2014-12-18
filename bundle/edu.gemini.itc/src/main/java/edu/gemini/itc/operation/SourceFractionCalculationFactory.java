// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;

import edu.gemini.itc.shared.Instrument;


public class SourceFractionCalculationFactory extends CalculationFactory {
    public Calculatable getCalculationInstance(
            SourceDefinitionParameters sourceDefinitionParameters,
            ObservationDetailsParameters observationDetailsParameters,
            ObservingConditionParameters observingConditionParameters,
            TeleParameters teleParameters,
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

