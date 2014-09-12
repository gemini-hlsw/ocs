package edu.gemini.pit.ui.util

import edu.gemini.pit.ui.ShellAdvisor
import swing._
import edu.gemini.pit.model.Model
import edu.gemini.pit.ui.binding.BoundView
import edu.gemini.model.p1.immutable.Proposal
import scalaz._
import Scalaz._

class StatusBar(shellAdvisor: ShellAdvisor) extends Label with BoundView[Boolean] { panel =>
  val lens = Model.rolled
  val fromSemester = Model.fromSemester

  horizontalAlignment = Alignment.Left

  shellAdvisor.catalogHandler.addListener {
    cs =>
      if (cs.exists(_._2 == None)) {
        icon = SharedIcons.ICON_SPINNER_BLUE
        text = "Performing catalog lookup..."
      } else {
        icon = null
        text = readyText
      }
  }

  def readyText: String = {
    shellAdvisor.context.shell.model.map {
      case m: Model if lens.get(m) => val semester = fromSemester.get(m);s"Ready - This proposal has been converted from the semester ${semester.display}"
      case _                                    => "Ready"
    }.getOrElse("Ready")
  }
}