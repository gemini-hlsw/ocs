// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: Test.java,v 1.5 2004/01/12 16:46:25 bwalls Exp $
//
// In the default package
package edu.gemini.itc.niri;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.parameters.PlottingDetailsParameters;
import edu.gemini.itc.altair.AltairParameters;

import edu.gemini.itc.shared.Recipe;

import java.io.IOException;

/**
 * This class runs a calculation without needing servlets, etc.
 * Parameters are hard-coded and then the class is recompiled.
 */
public class Test
{
   // Run from command line to perform a calculation with fixed
   // parameters above.
   public static void main(String[] argv) throws IOException
   {
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
				     SourceDefinitionParameters.WATTS,
				     -1);

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

	 NiriParameters np =
	    new NiriParameters("J",
					 "none",
			         NiriParameters.F6,
			         NiriParameters.LOW_READ_NOISE,
			         NiriParameters.HIGH_WELL_DEPTH,
			         NiriParameters.NO_SLIT);

	TeleParameters tp =
		new TeleParameters(TeleParameters.SILVER,
					 TeleParameters.SIDE,
			         TeleParameters.OIWFS);


	PlottingDetailsParameters pdp =
		new PlottingDetailsParameters(PlottingDetailsParameters.AUTO_LIMITS,
							3,
							4);
        
        AltairParameters ap = new AltairParameters(5, 10, "IN", "NGS", false);



	 // Create NiriRecipe object with the fake parameters
	 Recipe recipe = new NiriRecipe(sdp, odp, ocp, np,tp, ap, pdp, null);

	 // Perform Calculation
         for(int i=0;i<50;i++)
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
