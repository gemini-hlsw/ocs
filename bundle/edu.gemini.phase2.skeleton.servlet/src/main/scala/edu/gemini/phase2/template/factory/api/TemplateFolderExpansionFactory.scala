package edu.gemini.phase2.template.factory.api

import edu.gemini.phase2.core.model.TemplateFolderExpansion
import edu.gemini.spModel.template.TemplateFolder

import scala.collection.JavaConverters._

object TemplateFolderExpansionFactory {
  def expand(folder: TemplateFolder, fact: TemplateFactory): Either[String, TemplateFolderExpansion] =
    for {
      be <- blueprintExpansions(folder, fact).right
    } yield BlueprintExpansion.toTemplateFolderExpansion(be)

  private def blueprintExpansions(folder: TemplateFolder, fact: TemplateFactory): Either[String, List[BlueprintExpansion]] = {
    val empty: Either[String, List[BlueprintExpansion]] = Right(Nil)
    (empty/:folder.getGroups.asScala) {
      case (e, pig) => e.right flatMap { lst =>
        fact.expand(folder.getBlueprints.get(pig.blueprintId), pig, folder.getTargets.asScala.toMap).right map { exp =>
          exp :: lst
        }
      }
    }
  }
}
