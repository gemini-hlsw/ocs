package edu.gemini.phase2.template.factory.api

import edu.gemini.phase2.core.model.TemplateFolderExpansion
import edu.gemini.spModel.template.Phase1Folder

import scala.collection.JavaConverters._
import edu.gemini.spModel.core.SPProgramID

object TemplateFolderExpansionFactory {

  def expand(folder: Phase1Folder, fact: TemplateFactory, preserveLibraryIds: Boolean, pid: SPProgramID): Either[String, TemplateFolderExpansion] =
    for {
      be <- blueprintExpansions(folder, fact, preserveLibraryIds, pid).right
    } yield BlueprintExpansion.toTemplateFolderExpansion(be)

  private def blueprintExpansions(folder: Phase1Folder, fact: TemplateFactory, preserveLibraryIds: Boolean, pid: SPProgramID): Either[String, List[BlueprintExpansion]] = {
    val empty: Either[String, List[BlueprintExpansion]] = Right(Nil)
    (empty/:folder.groups.asScala) {
      case (e, pig) => e.right flatMap { lst =>
        fact.expand(folder.blueprintMap.get(pig.blueprintId), pig, preserveLibraryIds, pid).right map { exp =>
          exp :: lst
        }
      }
    }
  }
}
