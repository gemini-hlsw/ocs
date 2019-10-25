package edu.gemini.ags.gems

import org.junit.Test
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, Conditions, ImageQuality, SkyBackground, WaterVapor}

class GemsMagnitudeTableTest {

  // Just a sanity check to make sure all Conditions values are accounted for.
  // If we were to add a new ImageQuality without updating the GemsMagnitudeTable,
  // for example, this would fail.
  
  @Test def testCompletion(): Unit =
    CloudCover.values.foreach { cc =>
      ImageQuality.values.foreach { iq =>
        SkyBackground.values.foreach { sb =>
          WaterVapor.values.foreach { wv =>
            GemsMagnitudeTable.CwfsConstraints(new Conditions(cc, iq, sb, wv))
          }
        }
      }
    }

}
