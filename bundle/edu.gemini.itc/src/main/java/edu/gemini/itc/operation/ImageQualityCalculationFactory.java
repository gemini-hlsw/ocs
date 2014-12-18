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


public class ImageQualityCalculationFactory extends CalculationFactory {
    public Calculatable getCalculationInstance(
            SourceDefinitionParameters sourceDefinitionParameters,
            ObservationDetailsParameters observationDetailsParameters,
            ObservingConditionParameters observingConditionParameters,
            TeleParameters teleParameters,
            Instrument instrument) {

        // Case A The Image quality is defined by the user
        // who has selected a Gaussian Extended source
        // Creates a GaussianImageQualityCalculation

        if (sourceDefinitionParameters.getSourceGeometry().equals(
                SourceDefinitionParameters.EXTENDED_SOURCE)) {

            if (sourceDefinitionParameters.getExtendedSourceType().equals(
                    SourceDefinitionParameters.GAUSSIAN)) {
                return new GaussianImageQualityCalculation(
                        sourceDefinitionParameters.getFWHM());
            }
        }

        // Case B The Image Quality is defined by either of the
        // Probes in conjuction with the Atmosphric Seeing.
        // This case creates an ImageQuality Calculation
        return new ImageQualityCalculation(teleParameters.getWFS(),
                                           observingConditionParameters.getImageQuality(),
                                           observingConditionParameters.getAirmass(),
                                           instrument.getEffectiveWavelength());

    }
}

