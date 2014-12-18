// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
// In the default package
package edu.gemini.itc.gnirs;

// REL-472: Commenting out Altair option for now
//import edu.gemini.itc.altair.AltairParameters;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.parameters.PlottingDetailsParameters;
import edu.gemini.itc.shared.Recipe;

import java.io.IOException;

/**
 * This class runs a calculation without needing servlets, etc.
 * Parameters are hard-coded and then the class is recompiled.
 */
public class Test {
    // Run from command line to perform a calculation with fixed
    // parameters above.
    public static void main(String[] argv) throws IOException {
        try {

            // Create fake parameters  remember .POINT_SOURCE
            SourceDefinitionParameters sdp =
                    new SourceDefinitionParameters(SourceDefinitionParameters.EXTENDED_SOURCE,
                            SourceDefinitionParameters.UNIFORM,
                            20.0,
                            SourceDefinitionParameters.MAG,
                            .35,
                            SourceDefinitionParameters.FILTER,
                            "J",
                            0.,
                            0.,
                            //use:
                            SourceDefinitionParameters.STELLAR_LIB +
                                    "/k0iii.nm",

                            //OR
                            //"modelBlackBody",

                            //OR
                            //"modelEmLine",
                            4805,
                            1.25,
                            150,
                            1e-18,
                            5e-16,
                            SourceDefinitionParameters.WATTS_FLUX,
                            SourceDefinitionParameters.WATTS, -1);

            ObservationDetailsParameters odp =
                    new ObservationDetailsParameters(
                            ObservationDetailsParameters.IMAGING,
                            ObservationDetailsParameters.INTTIME,
                            30,
                            120.,
                            0.5,
                            25.64,
                            ObservationDetailsParameters.IMAGING,
                            ObservationDetailsParameters.AUTO_APER,
                            0.7,
                            3);

            ObservingConditionParameters ocp =
                    new ObservingConditionParameters(1, 2, 4, 3, 1);

            GnirsParameters gp =
                    new GnirsParameters(
                            GnirsParameters.LONG_CAMERA,
                            GnirsParameters.G32,
                            GnirsParameters.X_DISP_OFF,
                            GnirsParameters.LOW_READ_NOISE,
                            "4.7",
                            "2.4",
                            GnirsParameters.SLIT0_1);

            TeleParameters tp =
                    new TeleParameters(TeleParameters.SILVER,
                            TeleParameters.SIDE,
                            TeleParameters.OIWFS);

            PlottingDetailsParameters pdp =
                    new PlottingDetailsParameters(PlottingDetailsParameters.AUTO_LIMITS,
                            .3,
                            .6);

            // REL-472: Commenting out Altair option for now
//            AltairParameters ap = new AltairParameters(5, 10, false);

            // Create GnirsRecipe object with the fake parameters
            // REL-472: Commenting out Altair option for now
            Recipe recipe = new GnirsRecipe(sdp, odp, ocp, gp, tp, /* ap, */ pdp);

            // Perform Calculation
            recipe.writeOutput();
            //recipe = null;
            //System.gc();

        } catch (Exception e) {
            // Any error during calculation will throw an exception and land here.
            e.printStackTrace();  // prints to console
            // Must do more to formulate an error message for the servlet.
        }

    }
}
