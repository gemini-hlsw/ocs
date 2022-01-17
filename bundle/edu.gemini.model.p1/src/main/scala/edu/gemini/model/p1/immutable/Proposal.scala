package edu.gemini.model.p1.immutable

import scalaz.Lens

import edu.gemini.model.p1.{mutable => M, schema}
import scala.collection.JavaConverters._
import java.util.logging.{Logger, Level}
import org.xml.sax.SAXException

import scalaz.{Band => SBand, _}
import Scalaz._
import edu.gemini.spModel.core.Coordinates

object Proposal {

  // Lenses
  val meta: Lens[Proposal, Meta] = Lens.lensu((a, b) => a.copy(meta = b), _.meta)
  val semester: Lens[Proposal, Semester] = Lens.lensu((a, b) => a.copy(semester = b), _.semester)
  val title: Lens[Proposal, String] = Lens.lensu((a, b) => a.copy(title = b), _.title)
  val abstrakt: Lens[Proposal, String] = Lens.lensu((a, b) => a.copy(abstrakt = b), _.abstrakt)
  val scheduling: Lens[Proposal, String] = Lens.lensu((a, b) => a.copy(scheduling = b), _.scheduling)
  val category: Lens[Proposal, Option[TacCategory]] = Lens.lensu((a, b) => a.copy(category = b), _.category)
  val investigators: Lens[Proposal, Investigators] = Lens.lensu((a, b) => a.copy(investigators = b), _.investigators)
  val observations: Lens[Proposal, List[Observation]] = Lens.lensu((a, b) => a.copy(observations = clean(b)), _.observations)
  val proposalClass: Lens[Proposal, ProposalClass] = Lens.lensu((a, b) => a.copy(proposalClass = b), _.proposalClass)

  val targets: Lens[Proposal, List[Target]] = Lens.lensu((a, b) => {
    val p = a.copy(targets = b.distinct)
    observations.set(p, p.observations) // force cleanup of empty obs
  }, _.targets)

  // Remove [partial] duplicates from appearing in the observation list
  def clean(os:List[Observation]) = {
    // Calculate what's a partial obs of what. O(N^2) sadly
    val partial:Map[(Observation, Observation), Boolean] = (for {
      o0 <- os
      o1 <- os
    } yield ((o0, o1), o0.isPartialObservationOf(o1))).toMap

    val obsList = os.foldRight(List.empty[Observation]) {(o, os) =>
      if (o.isEmpty || os.exists(x => partial((o, x)))) {
        os
      } else if (os.exists(x => partial((x, o)))) {
        os.map {
          case o0 if partial((o0, o)) => o
          case o0                     => o0
        }
      } else {
        o :: os
      }
    }

    // We always want a non-empty list of observations: the empty observations if nothing.
    nonEmptyObsList(obsList)
  }

  private val validate = Option(System.getProperty("edu.gemini.model.p1.validate")).isDefined
  private val logger = Logger.getLogger(getClass.getName)

  // Read schema version from a system property which in turn is set from
  // the Bundle Context on the activator. The bundle will not start if the property is missing
  // but I'll add this check anyway for unit testing and non-OSGi usage
  lazy val currentSchemaVersion = Option(System.getProperty("edu.gemini.model.p1.schemaVersion")) match {
    case Some(x:String) => x
    case x              => sys.error("Should set schemaVersion property")
  }

  lazy val empty = apply(
    Meta.empty,
    Semester.current,
    "",
    "",
    "",
    None,
    Investigators.empty,
    List.empty[Target],
    List(Observation.empty, Observation.emptyBand3), // REL-3290: Template observations
    ProposalClass.empty,
    currentSchemaVersion)

  /**
   * @param referenceCoordinates a map from nonsidereal target id to reference coordinates, for
   *   cases where the Ephemeris must be overridden for ITAC bucket-filling. This notion applies
   *   only to nonsidereal targets, is used only by ITAC, and does not exist in phase 2.
   */
  def apply(m:M.Proposal, referenceCoordinates: Map[String, Coordinates] = Map.empty) =
    new Proposal(m, referenceCoordinates)

  // REL-3290: Create a non-empty list of observations, i.e. make sure that if there are no defined observations,
  // then an empty one is provided to serve as a dummy observation for users to edit.
  def nonEmptyObsList(olist: List[Observation]): List[Observation] = {
    // Make sure we have one observation for bands 1/2, and one for band 3.
    val band12missing = !olist.exists(o => o.band === M.Band.BAND_1_2 && o.nonEmpty)
    val band3missing  = !olist.exists(o => o.band === M.Band.BAND_3 && o.nonEmpty)
    (band12missing ? List(Observation.empty) | olist.filter(o => o.nonEmpty && o.band === Band.BAND_1_2)) ++
       (band3missing ? List(Observation.emptyBand3) | olist.filter(o =>  o.nonEmpty && o.band === Band.BAND_3))
  }
}

case class Proposal(meta:Meta,
                    semester:Semester,
                    title:String,
                    abstrakt:String,
                    scheduling:String,
                    category:Option[TacCategory],
                    investigators:Investigators,
                    targets:List[Target],
                    observations:List[Observation],
                    proposalClass:ProposalClass,
                    schemaVersion:String) {

  // Here is the evil bit that's explained in a little more detail over in Observation. The idea is that once an
  // Observation is owned by a Proposal, it has to defer to the Proposal's target list. We have to do this with a
  // mutable variable because there's no letrec in Scala; functions can be recursive but data can't. An alternative
  // would be to require a Proposal every time we wanted to get the target out of an Observation, which is a road I
  // went down but it turns out to be pretty bad. This may end up being worse, but mercifully the effect is entirely
  // localized; the public API is entirely immutable and this little tapdance is the only true nugget of evil.
  observations.foreach(_.proposal = Some(this)) // field is package-private

  def isSubmitted = proposalClass.key.isDefined

  def check = {
    observations.forall(_.proposal == Some(this))
  }

  def fix(): Unit = {
    observations.foreach(_.proposal = Some(this)) // field is package-private
  }

  def resetObservationMeta: Proposal = copy(observations = observations.map(_.copy(meta = None)))

  // REL-3290: Now that we always have an observation (an empty one if there are no others), when we access the
  // list of observations for processing, we want to filter this dummy observation out.
  def nonEmptyObservations: List[Observation] = observations.filterNot(_.isEmpty)

  // REL-3290: Check to see if there are observations that are non-empty.
  def hasNonEmptyObservations: Boolean = nonEmptyObservations.nonEmpty

  // see companion apply for explanation of `referenceCoordinates`
  private def this(m:M.Proposal, referenceCoordinates: Map[String, Coordinates]) = this(
    Meta(m.getMeta),
    Semester(m.getSemester),
    m.getTitle,
    m.getAbstract.unwrapLines,
    m.getScheduling,
    Option(m.getTacCategory),
    Investigators(m),
    m.getTargets.getSiderealOrNonsiderealOrToo.asScala.map(t => Target(t, referenceCoordinates.get(t.getId()))).toList,
    Proposal.nonEmptyObsList(m.getObservations.getObservation.asScala.map(o => Observation(o, Option(o.getTarget()).map(_.getId).flatMap(referenceCoordinates.get))).toList),
    Option(m.getProposalClass).map(ProposalClass(_)).getOrElse(ProposalClass.empty),  // TODO: get rid of the empty case
    m.getSchemaVersion)

  def conditions = observations.flatMap(_.condition).distinct
  def blueprints = observations.flatMap(_.blueprint).distinct

  def mutable = {
    val n = new Namer

    val m = Factory.createProposal
    m.setMeta(meta.mutable)
    m.setSemester(semester.mutable)
    m.setTitle(title)
    m.setAbstract(abstrakt)
    m.setScheduling(scheduling)
    m.setTacCategory(category.orNull)
    m.setInvestigators(investigators.mutable(n))
    val ts = Factory.createTargets()
    ts.getSiderealOrNonsiderealOrToo.addAll(targets.map(_.mutable(n)).asJava)
    m.setTargets(ts)

    val bs = Factory.createBlueprints()
    bs.getFlamingos2OrGmosNOrGmosS.addAll(blueprints.map(_.toChoice(n)).asJava)
    m.setBlueprints(bs)

    val cs = Factory.createConditions()
    cs.getCondition.addAll(conditions.map(_.mutable(n)).asJava)
    m.setConditions(cs)

    val os = Factory.createObservations()
    os.getObservation.addAll(nonEmptyObservations.map(_.mutable(n)).asJava)
    m.setObservations(os)

    m.setProposalClass(ProposalClass.mutable(this, n))
    m.setSchemaVersion(schemaVersion)
    m

  }

  import Proposal._

  if (validate) try {
    schema.Validator.validate(mutable)
  } catch {
    case ex:SAXException => logger.warning(ex.getMessage)
    case ex:Exception    => logger.log(Level.WARNING, "Trouble validating proposal object.", ex)
  }

}
