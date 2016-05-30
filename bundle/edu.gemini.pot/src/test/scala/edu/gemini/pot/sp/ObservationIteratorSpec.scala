package edu.gemini.pot.sp

import edu.gemini.pot.sp.ProgramGen.genProg
import org.scalacheck.Gen

import scala.collection.JavaConverters._

object ObservationIteratorSpec extends ProgramTestSupport {

  val genTestProg: Gen[ISPFactory => ISPProgram] = genProg

  "ObservationIterator" should {
    "include all of a program's observations" ! forAllPrograms { (odb, progs) =>
      val obsIds0 = progs.map { p =>
        p.getAllObservations.asScala.map(_.getNodeKey).toSet
      }

      val obsIds1 = progs.map { p =>
        ObservationIterator.apply(p).asScala.map(_.getNodeKey).toSet
      }

      obsIds0 == obsIds1
    }
  }

}
