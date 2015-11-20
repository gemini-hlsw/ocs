package edu.gemini.spModel.target.obsComp

import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import org.specs2.mutable.Specification

class TargetObsCompSpec extends Specification {

  "TargetObsComp" should {

    "Add listener on construction" in {
      val toc  = new TargetObsComp
      val base = toc.getBase
      base.getWatchers.size must_== 1
    }

    "Remove listener when the TargetEnvironment is updated" in {
      val toc  = new TargetObsComp
      val base = toc.getBase
      toc.setTargetEnvironment(TargetEnvironment.create(new SPTarget))
      base.getWatchers.size must_== 0
    }

  }

}

