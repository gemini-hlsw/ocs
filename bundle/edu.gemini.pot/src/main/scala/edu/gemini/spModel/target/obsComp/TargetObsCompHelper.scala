package edu.gemini.spModel.target.obsComp

import edu.gemini.spModel.core.{HorizonsDesignation, Target}
import edu.gemini.spModel.target.env.Asterism

import scalaz.{INil, NonEmptyList}

class TargetObsCompHelper {
  import HorizonsDesignation._

  def targetTag(a: Asterism): String =
    a.allTargets match {
      case NonEmptyList(t, INil()) => targetTag(t)
      case _ => "Asterism"
    }

  def targetTag(t: Target): String =
    t.fold(
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

}
