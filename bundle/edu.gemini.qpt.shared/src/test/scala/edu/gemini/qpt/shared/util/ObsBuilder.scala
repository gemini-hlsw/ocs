package edu.gemini.qpt.shared.util

import edu.gemini.ags.api.AgsAnalysis
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qpt.shared.sp.{Group, Obs, Prog}
import edu.gemini.shared.util.immutable.{None => JNone, DefaultImList}
import edu.gemini.spModel.core.{Declination, RightAscension, Coordinates, SPProgramID}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.obs.plannedtime.{ PlannedStepSummary, SetupTime }
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obs.SPObservation.Priority
import edu.gemini.spModel.obs.{SchedulingBlock, ObservationStatus}
import edu.gemini.spModel.obscomp.SPGroup
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.too.TooType

import scala.collection.JavaConverters._


/**
 * A builder for mini model observations to simplify on-the-fly creations of observations, e.g. for testing.
 * The resulting Obs objects are immutable java instances.
 * NOTE: Equality on Obs objects is defined based on the program and the obs number. Therefore it is important
 * to have at least unique obs numbers when creating observations for a test. Either assign one explicitely
 * or the observation will get an obs number assigned on creation based on a static counter.
 * FURTHER NOTE: Instances always have an empty scheduling block.
 */
case class ObsBuilder(
// TODO: define useful defaults, use options on the scala side of things where applicable
  prog: Prog = new Prog(SPProgramID.toProgramID("GN-2013A-Q-16")),         // what would be a useful default??
  group: Group = new Group("Group", SPGroup.GroupType.DEFAULT, ""),  // needed?
  obsNumber: Int = -1, // default -1 will be overriden with unique obsnumber on creation (see apply)
  title: String = "Title",
  priority: Priority = Priority.DEFAULT,
  tooPriority: TooType = TooType.none,
  obsStatus: ObservationStatus = ObservationStatus.DEFAULT,
  targetEnvironment: TargetEnvironment = TargetEnvironment.create(new SPTarget(0, 0)),
  obsClass: ObsClass = ObsClass.SCIENCE,
  centralWavelength: Double = 0.0,
  steps: PlannedStepSummary = new PlannedStepSummary(SetupTime.ZERO, Array(), Array(), Array()),
  piPlannedTime: Long = 0,
  execPlannedTime: Long = 0,
  elapsedTime: Long = 0,
  quality: SPSiteQuality = new SPSiteQuality(),
  lgs: Boolean = false,
  ao: Boolean = false,
  meanParallacticAngle: Boolean = false,
  customMask: String = null,
  options: Set[Enum[_]] = Set(),
  instrument: Array[SPComponentType] = Array(),
  analysis: List[AgsAnalysis] = Nil
)
{

  def apply: Obs = {
    // create a unique number for this observation (used in equals on Obs object)
    // if none has been set by user
    val nr = if (obsNumber == -1) ObsBuilder.curCounter() else obsNumber
    new Obs(
      prog,
      group,
      nr,
      Obs.createObsId(prog, nr),
      title,
      priority,
      tooPriority,
      obsStatus,
      obsClass,
      targetEnvironment,
      instrument,
      ObsBuilder.javaOptionsSet(options),
      customMask,
      centralWavelength,
      steps,
      piPlannedTime,
      execPlannedTime,
      elapsedTime,
      quality,
      lgs,
      ao,
      meanParallacticAngle,
      DefaultImList.create(analysis.asJavaCollection),
      JNone.instance[SchedulingBlock]
    )
  }

  // -- some code for Java compatibility:
  def this(programId: String) = this(new Prog(SPProgramID.toProgramID(programId)))
  def setProg(p: Prog) = copy(prog = p)
  def setGroup(g: Group) = copy(group = g)
  def setObsNumber(n: Int) = copy(obsNumber = n)
  def setTitle(t: String) = copy(title = t)
  def setPriority(p: Priority) = copy(priority = p)
  def setTooType(t: TooType) = copy(tooPriority = t)
  def setObsStatus(s: ObservationStatus) = copy(obsStatus = s)
  def setObsClass(c: ObsClass) = copy(obsClass = c)
  def setRa(ra: RightAscension) = copy(targetEnvironment = TargetEnvironment.create(new SPTarget(ra.toAngle.toDegrees, 0)))
  def setDec(dec: Declination) = copy(targetEnvironment = TargetEnvironment.create(new SPTarget(0, dec.toAngle.toDegrees)))
  def setCoordinates(coords: Coordinates) = copy(targetEnvironment = TargetEnvironment.create(new SPTarget(coords.ra.toAngle.toDegrees, coords.dec.toAngle.toDegrees)))
  def setTargetEnvironment(t: TargetEnvironment) = copy(targetEnvironment = t)
  def setInstrument(i: Array[SPComponentType]) = copy(instrument = i)
  def setInstrument(i: SPComponentType) = copy(instrument = Array(i))
  def setOptions(o: Set[Enum[_]]) = copy(options = o)
  def setCustomMask(m: String) = copy(customMask = m)
  def setCentralWavelength(w: Double) = copy(centralWavelength = w)
  def setPlannedSteps(s: PlannedStepSummary)  = copy(steps = s)
  def setSiteQuality(q: SPSiteQuality) = copy(quality = q)
  def setLgs(l: Boolean) = copy(lgs = l)
  def setAo(a: Boolean) = copy(ao = a)
  def setMeanParallacticAngle(p: Boolean) = copy(meanParallacticAngle = p)
  def setAgsAnalysis(a : List[AgsAnalysis]) = copy(analysis = a)
}


/** Some static stuff. */
object ObsBuilder {
  def curCounter(): Int = {
    synchronized {
      obsCounter += 1
      obsCounter
    }
  }
  private var obsCounter = 0

  // existential type that represents java's Enum<?>
  private type E = Enum[T] forSome { type T <: Enum[T] }
  def javaOptionsSet(o: Set[Enum[_]]) = new java.util.HashSet[E]() {{ o.foreach(x => add(x.asInstanceOf[E])) }}

}


