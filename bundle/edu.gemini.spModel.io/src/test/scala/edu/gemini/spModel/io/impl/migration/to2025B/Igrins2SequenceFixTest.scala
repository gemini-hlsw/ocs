package edu.gemini.spModel.io.impl.migration
package to2025B

import edu.gemini.pot.sp.{ISPProgram, ISPSeqComponent, SPComponentType}
import edu.gemini.spModel.io.impl.migration.MigrationTest

import org.specs2.mutable.Specification
import edu.gemini.spModel.rich.pot.sp._

class Igrins2SequenceFixTest extends Specification with MigrationTest {

  implicit class MoreISPProgramOps(p: ISPProgram) {
    def fingIGRINS2Sequence: List[ISPSeqComponent] =
      p.allObservations
        .flatMap(_.findSeqComponentsByType(SPComponentType.ITERATOR_IGRINS2))
  }

  // Update the sequence
  "2025B IGRINS-2 sequence rename" should {
    "Rename the sequence node" in withTestProgram2("GN-2025A-Q-1.xml") { r =>
      r.fingIGRINS2Sequence must haveSize(1)
      r.fingIGRINS2Sequence must contain(allOf((x: ISPSeqComponent) => x.getType must beEqualTo(SPComponentType.ITERATOR_IGRINS2)))
    }

    "Rename nested sequence nodes" in withTestProgram2("GN-2025A-Q-2.xml") { r =>
      r.fingIGRINS2Sequence must haveSize(2)
      r.fingIGRINS2Sequence must contain(allOf((x: ISPSeqComponent) => x.getType must beEqualTo(SPComponentType.ITERATOR_IGRINS2)))
    }
  }

}