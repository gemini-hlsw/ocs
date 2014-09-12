package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable._
import edu.gemini.phase2.core.model.SkeletonShell
import edu.gemini.spModel.core.SPProgramID

object SkeletonFactory {
  def makeSkeleton(id: SPProgramID, p: Proposal): Either[String, SkeletonShell] =
    for {
      tf <- TemplateFolderFactory.create(p).right
    } yield new SkeletonShell(id, SpProgramFactory.create(p), tf)
}
