package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbItcParameters {
  import instrumentdetails._
  import observationdetails._
  import observingconditions._
  import sourcedefinition._
  import telescopedetails._

  val genItcParamaters: Gen[ItcParameters] =
    for {
      s <- arbitrary[SourceDefinition]
      o <- arbitrary[ObservationDetails]
      c <- arbitrary[ObservingConditions]
      t <- arbitrary[TelescopeDetails]
      i <- arbitrary[InstrumentDetails]
    } yield ItcParameters(s, o, c, t, i)

  implicit val arbItcParameters: Arbitrary[ItcParameters] =
    Arbitrary(genItcParamaters)

}

object itcparameters extends ArbItcParameters