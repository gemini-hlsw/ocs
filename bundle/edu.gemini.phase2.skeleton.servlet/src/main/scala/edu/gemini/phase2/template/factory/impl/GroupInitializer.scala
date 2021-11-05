package edu.gemini.phase2.template.factory.impl

import edu.gemini.pot.sp.{ISPObservation, SPComponentType, ISPGroup}
import edu.gemini.spModel.template.SpBlueprint
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.obscomp.SPGroup.GroupType
import edu.gemini.spModel.obscomp.SPGroup

/**
 * Trait for a class which knows how to find and setup an ISPGroup for a
 * particular blueprint.
 */
trait GroupInitializer[B <: SpBlueprint] {

  def program:String
  def targetGroup:Seq[Int]
  def baselineFolder:Seq[Int]
  def notes:Seq[String]
  def blueprint:B
  def groupType: GroupType = GroupType.TYPE_SCHEDULING
  def instrumentType:SPComponentType = blueprint.instrumentType

  def initialize(db:TemplateDb):Maybe[ISPGroup] =
    for {
      grp <- db.groups(program, (targetGroup ++ baselineFolder).map(_.toString), notes).right
      _ <- initialize(grp, db).right
    } yield {
      grp.getDataObject() match {
        case g: SPGroup => g.setGroupType(groupType); grp.setDataObject(g)
        case other      => sys.error(s"Unpossible: ISPGroup contains instance of ${other.getClass.getName}")
      }
      grp
    }

  def initialize(group:ISPGroup, db:TemplateDb):Maybe[Unit]

  protected def forObservations(group:ISPGroup, idList:Seq[Int], ini:ISPObservation => Maybe[Unit]):Maybe[Unit] =
    for {
      os <- idList.mapM(group.apply).right
      _ <- os.mapM_(ini).right
    } yield ()

  protected def forObservations(group:ISPGroup, ini:ISPObservation => Maybe[Unit]):Maybe[Unit] =
    group.allObservations.mapM_(ini)

  def when(test: => Boolean)(f: => Either[String, Unit]) = if (test) f else Right(())

  implicit def pimpObs(o:ISPObservation) = new {
    // N.B. the Seq below somehow become Sequence .. no idea
    def memberOf(seq:Seq[Int]) = o.libraryId.exists(seq.map(_.toString).contains)
  }

}

trait TargetFolder[B <: SpBlueprint] extends GroupInitializer[B] {
  override val groupType = SPGroup.GroupType.TYPE_FOLDER
  def targetFolder: Seq[Int]
  final def targetGroup: Seq[Int] = targetFolder
}
