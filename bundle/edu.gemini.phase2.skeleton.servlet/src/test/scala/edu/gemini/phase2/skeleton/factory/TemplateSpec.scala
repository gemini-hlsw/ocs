package edu.gemini.phase2.skeleton.factory

import java.security.Principal

import edu.gemini.model.p1.immutable.{NifsBlueprintBase, BlueprintBase, Target, TimeAmount, Condition, Meta, Observation, Magnitude, SiderealTarget, NifsBlueprint, ProposalIo, Proposal}
import edu.gemini.model.p1.mutable.{MagnitudeSystem, MagnitudeBand, NifsDisperser}
import edu.gemini.phase2.core.model.SkeletonShell
import edu.gemini.phase2.core.odb.SkeletonStoreService
import edu.gemini.phase2.template.factory.api.TemplateFolderExpansionFactory
import edu.gemini.phase2.template.factory.impl.{TemplateDb, TemplateFactoryImpl}
import edu.gemini.pot.sp.{ISPTemplateGroup, ISPProgram}
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.template.{TemplateGroup, TemplateFolder}
import org.specs2.mutable.Specification
import scala.collection.JavaConverters._
import scalaz._, Scalaz._
import Observation.{blueprint, target, condition, time}
import Proposal.{ targets, observations }

/** 
 * Mixin for a specs test for template expansion. This provides code to expand a Phase 1 Proposal
 * into a skeleton, then test that the expansion was correct.
 * @param xmlName name of the template to test: "NIFS_BP.xml" for example
 */
abstract class TemplateSpec(xmlName: String) { this: Specification =>

  if (System.getProperty("edu.gemini.model.p1.schemaVersion") == null)
    System.setProperty("edu.gemini.model.p1.schemaVersion", "dummy") // grr

  lazy val programId  = SPProgramID.toProgramID("GS-2015A-Q-1")
  lazy val templateDb = TemplateDb.loadWithFilter(java.util.Collections.emptySet[Principal], _ == xmlName).unsafeGet

  implicit class EitherOps[A](e: Either[String, A]) {
    def unsafeGet: A = e.fold(sys.error, identity)
  }

  /** 
   * Expand the given Phase 1 `Proposal`, passing it and its expansion (an `ISPProgram`) to the 
   * provided continuation. You will typically define your specs test inside the passed function.
   */
  def expand[A](p: => Proposal)(func: (Proposal, ISPProgram) => A): A = {
    val db = DBLocalDatabase.createTransient()
    try {
      val f   = Phase1FolderFactory.create(p).unsafeGet
      val ss  = new SkeletonShell(programId, SpProgramFactory.create(p), f)
      val tf  = TemplateFactoryImpl(templateDb)
      val tfe = TemplateFolderExpansionFactory.expand(ss.folder, tf, true).unsafeGet
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

  /** Return the list of all `ISPTemplateGroups`s in the given program. */
  def groups(sp: ISPProgram): List[ISPTemplateGroup] =
    sp.getTemplateFolder
      .getTemplateGroups
      .asScala
      .toList

  /** Assert that the given library IDs are included and excluded; call within a `should`. */
  def checkLibs(tg: ISPTemplateGroup, incl: Set[Int], excl: Set[Int]) = {
    val ls = libs(tg)
    s"Include library observations ${incl.mkString("{", ",", "}")}" in {
      ls.filter(incl) == incl
    }
    s"Exclude library observations ${incl.mkString("{", ",", "}")}" in {
      ls.filter(excl).isEmpty
    }
  }

}





// # Select acquisition and science observation
// IF OCCULTING DISK == None
//    IF target information contains a K magnitude
//       IF BT  then ACQ={3}  # Bright Object
//       IF MT  then ACQ={4}  # Medium Object
//       IF FT  then ACQ={5}  # Faint Object
//       IF BAT then ACQ={23}  # Blind offset
//    ELSE
//       ACQ={3,4,5,23}
//    SCI={6}
// ELSEIF OCCULTING DISK != None
//    IF target information contains a K magnitude
//       IF BT then ACQ={11}   # Bright Object
//       IF MT then ACQ={12}   # Medium Object
//       IF FT then ACQ={12}   # Faint Object
//       IF BAT then ACQ={12}  # Very faint
//    ELSE
//       ACQ={11,12}
//    SCI={13}

object NifsBlueprintTest extends TemplateSpec("NIFS_BP.xml") with Specification {

  // flip exec
  implicit class MoreIdOps[A](a:A) {
    def execState(s: State[A, Unit]): A = s.exec(a)
  }

  def p1Mag(n: Double): Magnitude = 
    Magnitude(n, MagnitudeBand.K, MagnitudeSystem.VEGA)

  def p1Target(ms: List[Magnitude]): SiderealTarget =
    SiderealTarget.empty.copy(name = "test", magnitudes = ms)

  def p1Obs(bp: BlueprintBase, st: SiderealTarget): Observation =
    Observation.empty execState {
      for {
        _ <- blueprint := Some(bp)
        _ <- target    := Some(st)
        _ <- condition := Some(Condition.empty)
        _ <- time      := Some(TimeAmount.empty)
      } yield ()
    }

  def proposal(bp: NifsBlueprintBase, kmags: List[Double]) = {

    val os: List[Observation] = 
      p1Obs(bp, p1Target(Nil)) :: // no mags
      kmags.map(p1Mag)
           .map(m => p1Target(List(m)))
           .map(p1Obs(bp, _))

    Proposal.empty execState {
      for {
        _ <- targets      := os.map(_.target).collect { case Some(t) => t }
        _ <- observations := os
      } yield ()
    }

  }


  expand(proposal(NifsBlueprint(NifsDisperser.H), (0.0 to 25.0 by 0.5).toList)) { (p, sp) =>
    "Science Program" should {
      "have some groups" in {
        groups(sp).length must_== 42
      }
    }
  }


}










