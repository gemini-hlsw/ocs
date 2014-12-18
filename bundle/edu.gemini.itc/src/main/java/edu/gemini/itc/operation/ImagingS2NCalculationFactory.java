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


public class ImagingS2NCalculationFactory extends CalculationFactory {
    public Calculatable getCalculationInstance(
            SourceDefinitionParameters sourceDefinitionParameters,
            ObservationDetailsParameters observationDetailsParameters,
            ObservingConditionParameters observingConditionParameters,
            TeleParameters teleParameters,
            Instrument instrument) {

        //Method A if a S2N
        //

        if (observationDetailsParameters.getCalculationMethod().
                equals(ObservationDetailsParameters.S2N)) {
	    System.out.println("ENTERING METHOD A S2N CALCULATION");
            return new ImagingS2NMethodACalculation(
                    observationDetailsParameters.getNumExposures(),
                    observationDetailsParameters.getSourceFraction(),
                    observationDetailsParameters.getExposureTime(),
                    instrument.getReadNoise(),
                    instrument.getPixelSize());

            // Case B if Int time
        } else if (observationDetailsParameters.getCalculationMethod().
                equals(ObservationDetailsParameters.INTTIME)) {
            if (sourceDefinitionParameters.getSourceGeometry().equals(
                    SourceDefinitionParameters.EXTENDED_SOURCE)) {
                if (sourceDefinitionParameters.getExtendedSourceType().
                        equals(SourceDefinitionParameters.UNIFORM)) {
                    return new ImagingUSBS2NMethodBCalculation(
                            observationDetailsParameters.getNumExposures(),
                            observationDetailsParameters.getSourceFraction(),
                            observationDetailsParameters.getExposureTime(),
                            instrument.getReadNoise(),
                            observationDetailsParameters.getSNRatio(),
                            instrument.getPixelSize());
                }
            } else {

                return new ImagingPointS2NMethodBCalculation(
                        observationDetailsParameters.getNumExposures(),
                        observationDetailsParameters.getSourceFraction(),
                        observationDetailsParameters.getExposureTime(),
                        instrument.getReadNoise(),
                        observationDetailsParameters.getSNRatio(),
                        instrument.getPixelSize());
            }
            // Case C SHould be method C

        } else {
            if (sourceDefinitionParameters.getSourceGeometry().equals(
                    SourceDefinitionParameters.EXTENDED_SOURCE)) {
                if (sourceDefinitionParameters.getExtendedSourceType().
                        equals(SourceDefinitionParameters.UNIFORM)) {
                    return new ImagingUSBS2NMethodCCalculation(
                            observationDetailsParameters.getNumExposures(),
                            observationDetailsParameters.getSourceFraction(),
                            observationDetailsParameters.getExposureTime(),
                            instrument.getReadNoise(),
                            observationDetailsParameters.getSNRatio(),
                            instrument.getPixelSize());
                }
            }
        }
	System.out.println("ENTERING METHOD C CALCULATION");
        return new ImagingPointS2NMethodCCalculation(
                observationDetailsParameters.getNumExposures(),
                observationDetailsParameters.getSourceFraction(),
                observationDetailsParameters.getExposureTime(),
                instrument.getReadNoise(),
                observationDetailsParameters.getSNRatio(),
                instrument.getPixelSize());


    }
}

