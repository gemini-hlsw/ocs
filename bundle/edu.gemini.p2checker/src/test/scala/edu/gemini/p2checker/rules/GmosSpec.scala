package edu.gemini.p2checker.rules

import java.util.UUID

import edu.gemini.p2checker.api.ObservationElements
import edu.gemini.p2checker.rules.gmos.GmosRule
import edu.gemini.pot.sp.{ISPObservation, SPComponentType}
import edu.gemini.pot.util.POTUtil
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.gemini.gmos._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.obsComp.TargetObsComp
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz.syntax.id._

/**
 * Test bed for generic GMOS-N and GMOS-S rules.
 * This can be easily recycled for other instruments by making the rule set configurable (currently GmosRule is used).
 */
class GmosSpec extends Specification {

  // configurable test setup, feel free to adapt as needed for future tests
  def setup[I <: ISPDataObject](programId: String, instrument: SPComponentType)(mod: I => Unit): ISPObservation = {
    val f = POTUtil.createFactory(UUID.randomUUID())
    val p = f.createProgram(null, SPProgramID.toProgramID(programId))
    val o = f.createObservation(p, null) <| p.addObservation
    val i = f.createObsComponent(p, instrument, null) <| o.addObsComponent
    val e = o.findObsComponentByType(SPComponentType.TELESCOPE_TARGETENV).get
    val t = e.getDataObject.asInstanceOf[TargetObsComp]
    e.setDataObject(t)

    // Modify the instrument's data object as needed
    val dataObj = i.getDataObject
    mod(dataObj.asInstanceOf[I])
    i.setDataObject(dataObj)

    // This is our observation..
    o
  }
  
  def executeRules(o: ISPObservation) = {
    val oe = new ObservationElements(o)
    new GmosRule().check(oe).getProblems.asScala.map(_.getId)
  }

  // check if expected errors and warnings are part of the result
  def expectAllOf(ids: String*)(o: ISPObservation) = executeRules(o)  must containAllOf(ids)

  // check if expected errors and warnings are part of the result
  def expectNoneOf(ids: String*)(o: ISPObservation) = executeRules(o) must not(containAnyOf(ids))

  // ============
  
  "No E2V for GMOS-S after 2015B rule" should {

    val E2VErrId = "GmosRule_POST_2015B_GMOS_S_WITH_E2V_RULE"

    "give no error for unknown semester with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth]("", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give no error for semester 2012A with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth]("GS-2012A-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give no error for semester 2015B with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth]("GS-2015B-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give an error for semester 2016A with E2V" in {
      expectAllOf(E2VErrId) { setup[InstGmosSouth]("GS-2016A-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give an error for semester 2017A with E2V" in {
      expectAllOf(E2VErrId) { setup[InstGmosSouth]("GS-2017A-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give no error for semester 2017A with Hamamatsu" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth]("GS-2017A-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.HAMAMATSU)
      }}
    }

    "not affect GMOS-N for semester 2016A with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosNorth]("GN-2016A-Q-34", SPComponentType.INSTRUMENT_GMOS) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }
  }

}

