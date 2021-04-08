package edu.gemini.phase2.skeleton.factory

import java.security.Principal

import edu.gemini.model.p1.immutable.{Target, BlueprintBase, TimeAmount, Condition, Observation, SiderealTarget, Proposal}
import edu.gemini.phase2.core.model.SkeletonShell
import edu.gemini.phase2.core.odb.SkeletonStoreService
import edu.gemini.phase2.template.factory.api.TemplateFolderExpansionFactory
import edu.gemini.phase2.template.factory.impl.{TemplateDb, TemplateFactoryImpl}
import edu.gemini.pot.sp.{ISPObservation, ISPTemplateGroup, ISPProgram}
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.core.{MagnitudeBand, Magnitude, SPProgramID}
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.template.TemplateParameters
import org.specs2.mutable.SpecificationLike
import scala.collection.JavaConverters._
import scalaz._, Scalaz._
import Observation.{blueprint, target, condition, progTime}
import Proposal.{ targets, observations }

/**
 * Mixin for a specs test for template expansion. This provides code to expand a Phase 1 Proposal
 * into a skeleton, then test that the expansion was correct.
 *
 * @param xmlName name of the template to test: "NIFS_BP.xml" for example
 */
abstract class TemplateSpec(xmlName: String) { this: SpecificationLike =>

  // Required when constructing phase 1 proposals
  if (System.getProperty("edu.gemini.model.p1.schemaVersion") == null)
    System.setProperty("edu.gemini.model.p1.schemaVersion", "dummy") // grr

  // Expensive to construct, and read-only. So we can reuse.
  lazy val templateDb = TemplateDb.loadWithFilter(java.util.Collections.emptySet[Principal], _ == xmlName).unsafeGet

  private implicit class EitherOps[A](e: Either[String, A]) {
    def unsafeGet: A = e.fold(sys.error, identity)
  }

  private implicit class MoreIdOps[A](a:A) {
    def execState(s: State[A, Unit]): A = s.exec(a)
  }

  /**
   * Expand the given Phase 1 `Proposal`, passing it and its expansion (an `ISPProgram`) to the
   * provided continuation. You will typically define your specs test inside the passed function.
   */
  def expand[A](p: => Proposal)(func: (Proposal, ISPProgram) => A): A = {
    val db = DBLocalDatabase.createTransient()
    try {
      val pid = SPProgramID.toProgramID("GS-2015A-Q-1")
      val f   = Phase1FolderFactory.create(pid.site, p).unsafeGet
      val ss  = new SkeletonShell(pid, SpProgramFactory.create(p), f)
      val tf  = TemplateFactoryImpl(templateDb)
      val tfe = TemplateFolderExpansionFactory.expand(ss.folder, tf, preserveLibraryIds = true).unsafeGet
      func(p, SkeletonStoreService.store(ss, tfe, db).program)
    } catch {

      // Because of the way specs works, any exception thrown in test construction will be reported
      // as a missing constructor, which is confusing and unhelpful. So we'll spit out the
      // exception here just so there's something useful to look at, and re-throw.
      case t: Throwable =>
        t.printStackTrace()
        throw t

    } finally {
      db.getDBAdmin.shutdown()
    }
  }

  /** Return the set of all integer library IDs that appear in the group. */
  def libs(tg: ISPTemplateGroup): Set[Int] =
    tg.getAllObservations
      .asScala
      .map(_.getDataObject.asInstanceOf[SPObservation])
      .map(_.getLibraryId.parseInt)
      .collect { case Success(n) => n }
      .toSet

  /**
   * Return a map from integer library ID to observation, for all library observations in the
   * specified target group.
   */
  def libsMap(tg: ISPTemplateGroup): Map[Int, ISPObservation] =
    tg.getAllObservations.asScala.toList.flatMap { o =>
      o.getDataObject.asInstanceOf[SPObservation].getLibraryId.parseInt match {
        case Success(n) => List((n, o))
        case Failure(_) => Nil
      }
    }.toMap

  /** Return the list of all `ISPTemplateGroups`s in the given program. */
  def groups(sp: ISPProgram): List[ISPTemplateGroup] =
    sp.getTemplateFolder
      .getTemplateGroups
      .asScala
      .toList

  /** Return the list of all `ISPObservation`s in the template groups of the program. */
  def templateObservations(sp: ISPProgram): List[ISPObservation] =
    groups(sp).flatMap(_.getAllObservations.asScala.toList)

  /** Assert that the given library IDs are included and excluded; call within a `should`. */
  def checkLibs(label: String, tg: ISPTemplateGroup, incl: Set[Int], excl: Set[Int]) = {
    val ls = libs(tg)
    s"$label should include ${incl.mkString("{", ",", "}")} and exclude ${excl.mkString("{", ",", "}")}." in {
      if (ls.filter(incl) == incl && !ls.exists(excl)) ok
      else sys.error("Failed: Actual library ids: " + ls)
    }
  }

  /** Retrieve the phase-2 targets from the given template group. */
  def p2targets(g: ISPTemplateGroup): List[SPTarget] =
    g.getTemplateParameters
      .asScala.toList
      .map(_.getDataObject.asInstanceOf[TemplateParameters])
      .map(_.getTarget)

  /** Retrieve the phase-2 magnitude in the given band, if any, from the given target. */
  def p2mag(t: SPTarget, b: MagnitudeBand): Option[Double] =
    t.getMagnitude(b).map(_.value)

  /** True if a note with the given title exists at the root of the given template group. */
  def existsNote(tg: ISPTemplateGroup, title: String) =
    tg.getObsComponents
      .asScala.toList
      .map(_.getDataObject)
      .collect { case n: SPNote => n }
      .exists(_.getTitle == title)

  /** Construct a phase-1 magnitude in the VEGA system. */
  def p1Mag(n: Double, b: MagnitudeBand): Magnitude =
    new Magnitude(n, b)

  /** Construct an empty phase-1 sidereal target with the given magnitudes. */
  def p1Target(ms: List[Magnitude]): SiderealTarget =
    SiderealTarget.empty.copy(name = "test", magnitudes = ms)

  /** Construct a phase-1 observation with the given blueprint and target. */
  def p1Obs(bp: BlueprintBase, st: Target): Observation =
    Observation.empty execState {
      for {
        _ <- blueprint := Some(bp)
        _ <- target    := Some(st)
        _ <- condition := Some(Condition.empty)
        _ <- progTime  := Some(TimeAmount.empty)
      } yield ()
    }

  /**
   * Construct a phase-1 proposal with a target for each magnitude in the given band, plus a target
   * with no magnitude information, with an observation for each using the supplied blueprint.
   */
  def proposal(bp: BlueprintBase, kmags: List[Double], band: MagnitudeBand): Proposal = {

    val os: List[Observation] =
      p1Obs(bp, p1Target(Nil)) :: // one obs with no mags
      kmags.map(p1Mag(_, band))
        .map(m => p1Target(List(m)))
        .map(p1Obs(bp, _))

    Proposal.empty execState {
      for {
        _ <- targets      := os.map(_.target).collect { case Some(t) => t }
        _ <- observations := os
      } yield ()
    }

  }

}
