package edu.gemini.spModel.target.obsComp

import edu.gemini.spModel.core.{HorizonsDesignation, Target}
import edu.gemini.spModel.target.env.Asterism

class TargetObsCompHelper {
  import HorizonsDesignation._

  def targetTag(a: Asterism): String =
    a match {
      case Asterism.Single(t) =>
        t.getTarget.fold(
          _ => "TOO",
          _ => "Target",
          _.horizonsDesignation match {
            case None                      => "Nonsidereal"
            case Some(AsteroidNewStyle(_)) |
                 Some(AsteroidOldStyle(_)) => "Asteroid"
            case Some(Comet(_))            => "Comet"
            case Some(MajorBody(_))        => "Major Body"
          }
        )
      case _ => "Asterism"
    }


}
