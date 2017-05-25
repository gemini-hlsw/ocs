package edu.gemini.spModel.target.obsComp

import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.shared.util.immutable.ScalaConverters._
import org.specs2.mutable.Specification

import scalaz._, Scalaz._

class TargetObsCompSpec extends Specification {

  "TargetObsComp" should {

    "Add listener on construction" in {
      val toc  = new TargetObsComp
      val targets = toc.getTargetEnvironment.getTargets.asScalaList;
      targets.foldMap(_.getWatchers.size) must_== targets.length
    }

    "Remove listener when the TargetEnvironment is updated" in {
      val toc  = new TargetObsComp
      val targets = toc.getTargetEnvironment.getTargets.asScalaList;
      toc.setTargetEnvironment(TargetEnvironment.create(new SPTarget))
      targets.foldMap(_.getWatchers.size) must_== 0
    }

  }

}

