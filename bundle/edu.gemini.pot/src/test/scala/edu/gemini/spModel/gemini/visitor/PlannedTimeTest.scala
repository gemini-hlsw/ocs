// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.visitor

import edu.gemini.pot.sp.{ISPObsComponent, SPComponentType}
import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.test.SpModelTestBase
import org.junit.Assert.assertEquals

import java.time.Duration


final class PlannedTimeTest extends SpModelTestBase {

  var obsComp: Option[ISPObsComponent] = None

  var dataObj: Option[VisitorInstrument] = None


  override def setUp(): Unit = {

    super.setUp()

    obsComp = Some(addObsComponent(VisitorInstrument.SP_TYPE))

    dataObj = obsComp.flatMap(oc => Option(oc.getDataObject.asInstanceOf[VisitorInstrument]))

  }

  def totalTime: Duration =
    Duration.ofMillis(PlannedTimeCalculator.instance.calc(getObs).totalTime)

  private def configure(
    cfg:     VisitorConfig,
    expTime: Duration,
    count:   Int
  ): Unit = {

    dataObj.foreach { d =>
      d.setName(cfg.displayValue)
      d.setVisitorConfig(cfg)
      d.setExposureTime(expTime.toMillis.toDouble / 1000.0)
      obsComp.foreach(_.setDataObject(d))
    }

    val sc = addSeqComponent(getObs.getSeqComponent, SPComponentType.OBSERVER_OBSERVE)
    val scDataObj = sc.getDataObject.asInstanceOf[SeqRepeatObserve]
    scDataObj.setStepCount(count)
    sc.setDataObject(scDataObj)

  }

  private def exec(
    cfg:     VisitorConfig,
    expTime: Duration,
    count:   Int
  ): Unit = {

    configure(cfg, expTime, count)

    val setup = cfg.setupTime
    val exp   = expTime.plus(cfg.readoutTime).multipliedBy(count)

    assertEquals(setup.plus(exp), totalTime)
  }

  import VisitorConfig._

  def testGenericNoSteps(): Unit = {
    exec(GenericVisitor, Duration.ofSeconds(1), 1)
  }

  def testAlopekeTwoSteps(): Unit = {
    exec(Alopeke, Duration.ofSeconds(10), 2)
  }

  def testIgrinsOneStep(): Unit = {
    exec(Igrins, Duration.ofSeconds(20), 1)
  }

}
