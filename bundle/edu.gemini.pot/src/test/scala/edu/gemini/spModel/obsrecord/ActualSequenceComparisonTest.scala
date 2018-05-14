package edu.gemini.spModel.obsrecord

import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.xml.PioXmlUtil

import org.junit.Test
import org.junit.Assert._

import java.io.{ BufferedReader, InputStreamReader }

import scala.collection.JavaConverters._

/**
 * A sanity check.  Loads XML describing an actual observation record and
 * compares the time accounting calculation.
 */
class ActualSequenceComparisonTest {

  private def loadXml(name: String): ParamSet = {
    val rdr = new BufferedReader(new InputStreamReader(getClass.getResourceAsStream(name)))
    try {
      PioXmlUtil.read(rdr).asInstanceOf[ParamSet]
    } finally {
      rdr.close
    }
  }

  def verifyCalculationsFor(obsId: String): Unit = {

    val qaParamSet = loadXml(s"$obsId.qa.xml").getParamSet("obsQaRecord")
    val exParamSet = loadXml(s"$obsId.ex.xml").getParamSet("obsExecRecord")

    val qa = ObsQaRecord.fromParamSet(qaParamSet)
    val ex = new ObsExecRecord(exParamSet)
    val cs = new CompressedConfigStore(exParamSet.getParamSet("configMap"))

    ex.getVisits(qa).toList.foreach { v =>
      val evts = v.getEvents.toVector

      val oldTa = OldTimeAccounting.getTimeCharges(evts.asJava, qa, cs)
      val newTa = TimeAccounting.calcAsJava(evts.asJava, qa, cs)

      assertEquals(oldTa, newTa)
    }
  }

  @Test
  def testN17BQ1_23(): Unit = {
    verifyCalculationsFor("GN-2017B-Q-1-23")
  }

  @Test
  def testN17BQ56_104(): Unit = {
    verifyCalculationsFor("GN-2017B-Q-56-104")
  }
}
