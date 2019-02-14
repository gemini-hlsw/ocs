package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.telescope.IssPort;

trait ArbTelescopeDetails {
  import core._

  val genTelescopeDetails: Gen[TelescopeDetails] =
    for {
      c   <- arbitrary[TelescopeDetails.Coating]
      iss <- arbitrary[IssPort]
      wfs <- arbitrary[GuideProbe.Type]
    } yield new TelescopeDetails(c, iss, wfs)

  implicit val arbTelescopeDetails: Arbitrary[TelescopeDetails] =
    Arbitrary(genTelescopeDetails)

}

object telescopedetails extends ArbTelescopeDetails