package edu.gemini.phase2.template.factory.api

import edu.gemini.phase2.core.model.TemplateFolderExpansion
import edu.gemini.spModel.template.Phase1Folder

import scala.collection.JavaConverters._

object TemplateFolderExpansionFactory {
  def expand(folder: Phase1Folder, fact: TemplateFactory): Either[String, TemplateFolderExpansion] =
    for {
      be <- blueprintExpansions(folder, fact).right
    } yield BlueprintExpansion.toTemplateFolderExpansion(be)

  private def blueprintExpansions(folder: Phase1Folder, fact: TemplateFactory): Either[String, List[BlueprintExpansion]] = {
    val empty: Either[String, List[BlueprintExpansion]] = Right(Nil)
    (empty/:folder.groups.asScala) {
      case (e, pig) => e.right flatMap { lst =>
        fact.expand(folder.blueprintMap.get(pig.blueprintId), pig).right map { exp =>
          exp :: lst
        }
      }
    }
  }
}
