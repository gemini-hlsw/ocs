package edu.gemini.spModel.io.impl.migration.to2017B

import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.timeacct._
import edu.gemini.spModel.timeacct.TimeAcctCategory._

import org.specs2.mutable.Specification

class TimeAcctMigrationTest extends Specification with MigrationTest {
  val HRS = 3600000

  def matchAward(a: TimeAcctAward, prog: Double): Boolean =
    a.getProgramAward.toMillis == (prog * HRS).round &&
      a.getPartnerAward.toMillis == 0l

  "2017B Time Accounting Migration" should {
    "Change implicit program award in hours to explicit/milliseconds" in withTestProgram2("timeAcct.xml") { p =>
      val prog  = p.getDataObject.asInstanceOf[SPProgram]
      val alloc = prog.getTimeAcctAllocation

      val ar = alloc.getAward(AR)
      val br = alloc.getAward(BR)
      val cl = alloc.getAward(CL)
      val us = alloc.getAward(US)

      matchAward(alloc.getSum, 5) &&
        matchAward(ar, 2.5) &&
        matchAward(br, 1.0) &&
        matchAward(cl, 1.5)
    }
  }
}
