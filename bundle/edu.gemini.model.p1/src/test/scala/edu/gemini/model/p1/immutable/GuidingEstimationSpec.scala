package edu.gemini.model.p1.immutable

import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import scala.xml.XML

class GuidingEstimationSpec extends SpecificationWithJUnit with SemesterProperties {

  "The GuidingEstimation evaluation" should {
    // trivial sanity tests
    "be FAILURE for 0 probability" in {
      GuidingEstimation(0).evaluation must beEqualTo(GuidingEvaluation.FAILURE)
    }
    "be SUCCESS for 100 probability" in {
      GuidingEstimation(100).evaluation must beEqualTo(GuidingEvaluation.SUCCESS)
    }
    "be WARNING with 0 < probability <] 50" in {
      GuidingEstimation(1).evaluation must beEqualTo(GuidingEvaluation.WARNING)
      GuidingEstimation(25).evaluation must beEqualTo(GuidingEvaluation.WARNING)
      GuidingEstimation(50).evaluation must beEqualTo(GuidingEvaluation.WARNING)
    }
    "be CAUTION with 50 < probability < 100" in {
      GuidingEstimation(51).evaluation must beEqualTo(GuidingEvaluation.CAUTION)
      GuidingEstimation(75).evaluation must beEqualTo(GuidingEvaluation.CAUTION)
      GuidingEstimation(99).evaluation must beEqualTo(GuidingEvaluation.CAUTION)
    }
  }

  "The GuidingEstimation XML serialization" should {
    "support CAUTION level" in {
      val meta = ObservationMeta(Some(GuidingEstimation(99)), None, None)
      val observation = Observation(None, None, None, Band.BAND_1_2, None).copy(meta = Some(meta))

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("evaluation") \> ("Caution")
    }
    "support WARNING level" in {
      val meta = ObservationMeta(Some(GuidingEstimation(1)), None, None)
      val observation = Observation(None, None, None, Band.BAND_1_2, None).copy(meta = Some(meta))

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("evaluation") \> ("Warning")
    }
  }

}