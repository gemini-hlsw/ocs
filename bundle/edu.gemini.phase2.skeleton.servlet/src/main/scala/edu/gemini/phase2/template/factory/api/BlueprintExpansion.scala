package edu.gemini.phase2.template.factory.api

import edu.gemini.phase2.core.model.{TemplateFolderExpansion, GroupShell, TemplateGroupShell}

import scala.collection.JavaConverters._

object BlueprintExpansion {
  def toTemplateFolderExpansion(lst: List[BlueprintExpansion]): TemplateFolderExpansion = {
    val (templates, baselines) = lst.map(be => (be.template, be.baseline)).unzip
    new TemplateFolderExpansion(templates.asJava, baselines.asJava)
  }
}
case class BlueprintExpansion(template: TemplateGroupShell, baseline: GroupShell)
