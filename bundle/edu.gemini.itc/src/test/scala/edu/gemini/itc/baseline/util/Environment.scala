package edu.gemini.itc.baseline.util

import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.parameters._
import org.scalacheck.{Arbitrary, Gen}

case class Environment(
                        src: SourceDefinitionParameters,
                        ocp: ObservingConditionParameters,
                        tep: TeleParameters,
                        pdp: PlottingDetailsParameters) {
  val hash = Hash.calc(src) + Hash.calc(ocp) + Hash.calc(tep)
}

/**
 * Test utilities
 */
object Environment {

  implicit val arbEnvironment: Arbitrary[Environment] =
    Arbitrary {
      for {
        src <- Gen.oneOf(Sources)
        ocp <- Gen.oneOf(ObservingConditions)
        tep <- Gen.oneOf(TelescopeConfigurations)
        pdp <- Gen.oneOf(PlottingParameters)
      } yield Environment(src, ocp, tep, pdp)
    }

  // ================
  // In order to reduce the total number of tests we try to define some sensible subsets and combinations
  // of parameters to be used in the tests.

  // Defines a set of relevant weather conditions (not all combinations make sense)
  // NOTE:
  //    IQ(1,2,3,4)  =IQ(20%,70%,85%,Any)
  //    CC(1,2,3,4,5)=CC(20%,50%,70%,80%,Any)
  //    WV(1,2,3,4)  =WV(20%,50%,80%,Any)
  private val weatherConditions = List[Tuple3[Int,Int,Int]](
    (1,2,2), // IQ20,CC50,WV50
    (1,2,4), // IQ20,CC50,Any
    (1,3,4), // IQ20,CC70,-
    (2,2,2), // IQ70,CC50,WV50
    (2,2,4), // IQ70,CC50,Any
    (2,3,4), // IQ70,CC70,-
    (3,2,2), // IQ85,CC50,WV50
    (3,3,4), // IQ85,CC70,-
    (4,5,4)  // Any ,-   ,-
  )
  // ================

  // Different sources
  val Sources = List(
    new SourceDefinitionParameters(
      SourceDefinitionParameters.POINT_SOURCE,
      SourceDefinitionParameters.UNIFORM,
      20.0,
      SourceDefinitionParameters.MAG,
      .35,
      SourceDefinitionParameters.FILTER,
      "J",
      0.0,
      0.0,
      SourceDefinitionParameters.STELLAR_LIB + "/k0iii.nm",
      4805,
      1.25,
      150,
      1e-18,
      5e-16,
      SourceDefinitionParameters.WATTS_FLUX,
      SourceDefinitionParameters.WATTS,
      -1)//,
//    new SourceDefinitionParameters(
//      SourceDefinitionParameters.EXTENDED_SOURCE,
//      SourceDefinitionParameters.UNIFORM,
//      20.0,
//      SourceDefinitionParameters.MAG,
//      .35,
//      SourceDefinitionParameters.FILTER,
//      "J",
//      0.0,
//      0.0,
//      SourceDefinitionParameters.STELLAR_LIB + "/k0iii.nm",
//      4805,
//      1.25,
//      150,
//      1e-18,
//      5e-16,
//      SourceDefinitionParameters.WATTS_FLUX,
//      SourceDefinitionParameters.WATTS,
//      -1),
//    new SourceDefinitionParameters(
//      SourceDefinitionParameters.EXTENDED_SOURCE,
//      SourceDefinitionParameters.GAUSSIAN,
//      20.0,
//      SourceDefinitionParameters.MAG,
//      .35,
//      SourceDefinitionParameters.FILTER,
//      "J",
//      0.0,
//      0.0,
//      SourceDefinitionParameters.STELLAR_LIB + "/k0iii.nm",
//      4805,
//      1.25,
//      150,
//      1e-18,
//      5e-16,
//      SourceDefinitionParameters.WATTS_FLUX,
//      SourceDefinitionParameters.WATTS,
//      -1)
  )

  // Defines a set of relevant observing conditions; total 9*4*3=108 conditions
  val ObservingConditions =
    for {
      (iq, cc, wv)  <- List((1,1,1),(1,1,2))//weatherConditions
      sb            <- List(1)              //List(1,2,3,4)       // SB20, SB50, SB80, ANY
      am            <- List(1.0,1.5,2.0)   // airmass 1.0, 1.5, 2.0
    } yield new ObservingConditionParameters(iq, cc, wv, sb, am)

  // Defines a set of relevant telescope configurations; total 1*2*2=4 configurations
  // NOTE: looking at TeleParameters.getWFS() it seems that AOWFS is always replaced with OIWFS
  val TelescopeConfigurations =
    for {
      coating       <- List(TeleParameters.SILVER) // don't test aluminium coating
      port          <- List(TeleParameters.SIDE, TeleParameters.UP)
      wfs           <- List(TeleParameters.OIWFS, TeleParameters.PWFS) // don't use AOWFS (?)
    } yield new TeleParameters(coating, port, wfs)

  val PlottingParameters = List(
    new PlottingDetailsParameters(PlottingDetailsParameters.AUTO_LIMITS, .3, .6)
  )

  val AltairConfigurations = List(
    new AltairParameters(5, 10, "IN", "NGS", false)  // altair used: no
    //new AltairParameters(5, 10, "IN", "NGS", true)   // altair used: yes
  )
}
