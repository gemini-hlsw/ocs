package edu.gemini.p2checker.rules

import java.util.{Collections, UUID}
import edu.gemini.p2checker.api.{IRule, ObservationElements}
import edu.gemini.pot.sp.{ISPFactory, ISPObservation, ISPProgram, ISPSeqComponent, Instrument, SPComponentType}
import edu.gemini.pot.util.POTUtil
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
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

  def createObserve(c: ObsClass, p: ISPProgram, o: ISPObservation, f: ISPFactory): ISPSeqComponent = {
    val sc   = f.createSeqComponent(p, SPComponentType.OBSERVER_OBSERVE, null)
    val dobj = sc.getDataObject.asInstanceOf[SeqRepeatObserve]
    dobj.setObsClass(c)
    sc.setDataObject(dobj)
    sc
  }

  def addObserve(c: ObsClass, p: ISPProgram, o: ISPObservation, f: ISPFactory): Unit = {
    o.getSeqComponent.addSeqComponent(createObserve(c, p, o, f))
  }

  def setObserve(c: ObsClass, p: ISPProgram, o: ISPObservation, f: ISPFactory): Unit = {
    o.getSeqComponent.setSeqComponents(Collections.singletonList(createObserve(c, p, o, f)));
  }

  def setConditions(
    o:  ISPObservation,
    cc: CloudCover    = CloudCover.ANY,
    iq: ImageQuality  = ImageQuality.ANY,
    sb: SkyBackground = SkyBackground.ANY,
    wv: WaterVapor    = WaterVapor.ANY
  ): Unit = {
    val oc = o.findObsComponentByType(SPComponentType.SCHEDULING_CONDITIONS).get
    val sc = oc.getDataObject().asInstanceOf[SPSiteQuality]
    sc.setCloudCover(cc)
    sc.setImageQuality(iq)
    sc.setSkyBackground(sb)
    sc.setWaterVapor(wv)
    oc.setDataObject(sc)
  }


}
