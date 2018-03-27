package edu.gemini.spModel.gemini.gpi


import edu.gemini.pot.sp.{Instrument, ISPSeqComponent, SPComponentType, SPNodeKey}
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset
import edu.gemini.spModel.guide.StandardGuideOptions
import edu.gemini.spModel.guide.StandardGuideOptions.Value._
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obsclass.ObsClass._
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.target.obsComp.TargetObsCompConstants
import org.junit.Assert._
import org.junit.Test

object OffsetTest {
  val GuideKey = new ItemKey(TargetObsCompConstants.CONFIG_NAME + ":" + TargetObsCompConstants.GUIDE_WITH_OIWFS_PROP)

  case class Row(p: Int, q: Int) {
    val expect: StandardGuideOptions.Value =
      if ((p == 0) && (q == 0)) guide else freeze
  }

  class Env {
    val odb  = DBLocalDatabase.createTransient()
    val pid  = SPProgramID.toProgramID("GS-2015A-Q-1")
    val prog = odb.getFactory.createProgram(new SPNodeKey(), pid)
    odb.put(prog)

    val obs = odb.getFactory.createObservation(prog, Instrument.none, null)

    val gpi = odb.getFactory.createObsComponent(prog, Gpi.SP_TYPE, null)
    obs.addObsComponent(gpi)

    def addOffsets(c: ObsClass, rows: Seq[Row]): Unit = {
      val off = odb.getFactory.createSeqComponent(prog, SPComponentType.ITERATOR_OFFSET, null)
      val dob = off.getDataObject.asInstanceOf[SeqRepeatOffset]

      val posList = dob.getPosList
      rows.foreach { case Row(p, q) =>  posList.addPosition(p.toDouble, q.toDouble) }

      off.setDataObject(dob)

      obs.getSeqComponent.addSeqComponent(off)

      off.addSeqComponent(makeObserve(c))
    }

    def makeObserve(c: ObsClass): ISPSeqComponent = {
      val observe = odb.getFactory.createSeqComponent(prog, SPComponentType.OBSERVER_OBSERVE, null)
      val dob     = observe.getDataObject.asInstanceOf[SeqRepeatObserve]
      dob.setObsClass(c)
      observe.setDataObject(dob)
      observe
    }

    private val options = new java.util.HashMap[String, Object]()

    def steps: Array[Config] =
      ConfigBridge.extractSequence(obs, options, ConfigValMapInstances.IDENTITY_MAP).getAllSteps

    def shutdown(): Unit = {
      odb.getDBAdmin.shutdown()
    }
  }

  private def doTest(rows: Row*): Unit = doTest(ObsClass.SCIENCE, rows: _*)

  private def doTest(c: ObsClass, rows: Row*): Unit = {
    val env = new Env
    try {
      if (rows.nonEmpty) env.addOffsets(c, rows)
      else env.obs.getSeqComponent.addSeqComponent(env.makeObserve(c))

      val seq = ConfigBridge.extractSequence(env.obs, new java.util.HashMap[String, Object](), ConfigValMapInstances.IDENTITY_MAP)
      val steps = seq.getAllSteps

      // calibration should park
      def mapExpected(normal: StandardGuideOptions.Value): StandardGuideOptions.Value =
        c match {
          case SCIENCE => normal
          case ACQ     => normal
          case _       => park
        }

      if (rows.isEmpty) {
        // Handle the empty sequence differently since there is always one step.
        assertEquals(1, steps.size)
        assertEquals(mapExpected(guide), steps(0).getItemValue(GuideKey))
      } else {
        assertEquals(rows.size, steps.size)
        rows.zip(steps).zipWithIndex.foreach { case ((row, step), index) =>
          val msg = s"$index) p=${row.p}, q=${row.q}"
          assertEquals(msg, mapExpected(row.expect), step.getItemValue(GuideKey))
        }
      }
    } finally {
      env.shutdown()
    }
  }
}

import OffsetTest._

class OffsetTest {
  @Test def noOffsets(): Unit       = doTest()
  @Test def guideOne(): Unit        = doTest(Row(0, 0))
  @Test def freezeOne(): Unit       = doTest(Row(1, 0))
  @Test def guideThenFreeze(): Unit = doTest(Row(0,0), Row(1,0))
  @Test def freezeThenGuide(): Unit = doTest(Row(1,0), Row(0,0))
  @Test def guideMany(): Unit       = doTest(Row(0,0), Row(0,0), Row(0,0))
  @Test def freezeMany(): Unit      = doTest(Row(1,0), Row(0,1), Row(2,2))
  @Test def freezeMiddle(): Unit    = doTest(Row(0,0), Row(1,1), Row(0,0))
  @Test def parkAcqCal(): Unit      = doTest(ObsClass.ACQ_CAL, Row(0, 0))
  @Test def parkAcqCals(): Unit     = doTest(ObsClass.ACQ_CAL, Row(0, 0), Row(1,0))
}