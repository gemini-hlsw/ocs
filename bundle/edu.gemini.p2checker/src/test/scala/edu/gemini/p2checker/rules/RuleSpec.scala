package edu.gemini.p2checker.rules

import java.util.UUID
import edu.gemini.p2checker.api.{IRule, ObservationElements}
import edu.gemini.pot.sp.{ISPFactory, ISPObservation, ISPProgram, Instrument, SPComponentType}
import edu.gemini.pot.util.POTUtil
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.obsComp.TargetObsComp
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz.syntax.id._

/**
 * Generic test bed for arbitrary P2 checker rule tests.
 */
abstract class RuleSpec extends Specification {

  // the ruleset to be tested
  def ruleSet: IRule

  // configurable test setup, feel free to adapt as needed for future tests
  def setup[I <: ISPDataObject](instrument: SPComponentType, programId: String = "")(mod: I => Unit): ISPObservation =
    advancedSetup[I](instrument, programId) { (_, _, i, _) =>
      mod(i)
    }

    // configurable test setup, feel free to adapt as needed for future tests
  def advancedSetup[I <: ISPDataObject](instrument: SPComponentType, programId: String = "")(mod: (ISPProgram, ISPObservation, I, ISPFactory) => Unit): ISPObservation = {
    val f = POTUtil.createFactory(UUID.randomUUID())
    val p = f.createProgram(null, SPProgramID.toProgramID(programId))
    val o = f.createObservation(p, Instrument.none, null) <| p.addObservation
    val i = f.createObsComponent(p, instrument, null) <| o.addObsComponent
    val e = o.findObsComponentByType(SPComponentType.TELESCOPE_TARGETENV).get
    val t = e.getDataObject.asInstanceOf[TargetObsComp]
    e.setDataObject(t)

    f.createSeqComponent(p, SPComponentType.OBSERVER_OBSERVE, null) <| o.getSeqComponent.addSeqComponent

    // Modify the instrument's data object as needed
    val dataObj = i.getDataObject
    mod(p, o, dataObj.asInstanceOf[I], f)
    i.setDataObject(dataObj)

    // This is our observation..
    o
  }

  // exercise the defined rule set
  def executeRules(o: ISPObservation) = {
    val oe = new ObservationElements(o)
    ruleSet.check(oe).getProblems.asScala.map(_.getId)
  }

  // check if expected errors and warnings are part of the result
  def expectAllOf(ids: String*)(o: ISPObservation) = executeRules(o)  must containAllOf(ids)

  // check if expected errors and warnings are part of the result
  def expectNoneOf(ids: String*)(o: ISPObservation) = executeRules(o) must not(containAnyOf(ids))

}
