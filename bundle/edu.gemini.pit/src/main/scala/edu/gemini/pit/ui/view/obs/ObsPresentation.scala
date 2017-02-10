package edu.gemini.pit.ui.view.obs

import edu.gemini.pit.ui.util.SharedIcons._

import javax.swing.Icon
import scalaz.Scalaz._
import edu.gemini.model.p1.immutable._

sealed trait ObsPresentation {
  def icon: Icon
  def text: String
  def tooltip: String
}

object ObsPresentation {
  case class Red(text: String = null, tooltip: String = null) extends ObsPresentation {
    def icon = BULLET_RED
  }

  case class Yellow(text: String = null, tooltip: String = null) extends ObsPresentation {
    def icon = BULLET_YELLOW
  }

  case class Green(text: String = null, tooltip: String = null) extends ObsPresentation {
    def icon = BULLET_GREEN
  }

  case class Orange(text: String = null, tooltip: String = null) extends ObsPresentation {
    def icon = BULLET_ORANGE
  }

  case class Grey(text: String = null, tooltip: String = null) extends ObsPresentation {
    def icon = BULLET_GREY
  }

  case object Blank extends ObsPresentation {
    def icon    = ICON_BLANK
    def text    = null
    def tooltip = null
  }

  def guiding(obs: Observation): ObsPresentation = {
    val guide = for {
      m <- obs.meta
      g <- m.guiding
    } yield g

    val text = guide.map(_.toString).getOrElse("")

    def isBestConditions: Boolean =
      obs.condition.exists { conds =>
        conds.cc == CloudCover.BEST && conds.iq == ImageQuality.BEST && conds.sb == SkyBackground.BEST
      }

    def betterConditionsText: String =
      if (isBestConditions) "" else "<br>Try tightening observing conditions?"

    import GuidingEvaluation._

    val percentage = 100 - ~guide.map(_.perc)

    guide.map(_.evaluation match {
      case CAUTION => Yellow(text, s"<html>Some PAs ($percentage%) do not have suitable guide stars.</html>")
      case FAILURE => Red(text,    s"<html>There is a very low probability of finding a guide star for this observation.$betterConditionsText</html>")
      case SUCCESS => Green(text,   "This observation is very likely to have a usable guidestar.")
      case WARNING => Orange(text, s"<html>Many PAs ($percentage%) do not have suitable guide stars. Review if a specific PA is required.</html>")
    }).getOrElse(Grey(text,         "Guide star success estimation pending."))
  }

  def visibility(sem: Semester, obs: Observation): ObsPresentation = {
    def msg(template: String): String =
      template.format(obs.blueprint.get.site.name, sem.display)

    val vis = for {
      m <- obs.meta
      v <- m.visibility
    } yield v

    vis.map {
      case TargetVisibility.Good    => Green(tooltip = msg("This target has good visibility at %s during %s."))
      case TargetVisibility.Limited => Yellow(tooltip = msg("<html>This target has limited visibility at %s during %s.<br>The observation time should be short and/or conditions constraints relaxed.</html>"))
      case _                        => Red(tooltip = msg("<html>This target is inaccessible at %s during %s.<br>Consider an alternative target.</html>"))
    }.getOrElse(Blank)
  }

  def gsa(obs: Observation): ObsPresentation = {
    val dsetCount = for {
      m <- obs.meta
      c <- m.gsa
    } yield c

    def inst: String = obs.blueprint.get match {
      case g: GeminiBlueprintBase => g.instrument.display
      case _                      => "" // shouldn't happen
    }

    (dsetCount collect {
      case 0 => Green(tooltip = s"""There are no $inst datasets in the GOA within 30" of this target.""")
      case c => Yellow(tooltip = "There are %d%s %s datasets in the GOA within 30\" of this target.".format(
                  c, if (c < 50) "" else " or more", inst
                ))
    }).getOrElse(Grey(tooltip = "GOA dataset search result is pending."))
  }
}