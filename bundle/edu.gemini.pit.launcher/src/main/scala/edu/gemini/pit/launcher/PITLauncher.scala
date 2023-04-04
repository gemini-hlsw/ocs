package edu.gemini.pit.launcher

import java.io.File
import java.util.Locale

import edu.gemini.ags.client.impl.AgsHttpClient
import edu.gemini.model.p1.immutable.Semester
import edu.gemini.pit.model.Model
import edu.gemini.pit.ui.ShellAdvisor
import edu.gemini.pit.ui.robot.AgsRobot
import edu.gemini.ui.workspace.impl.Workspace


/**
 * Launcher for the PIT in local mode, used for development
 */
object PITLauncher extends App {
  val locale = Locale.getDefault
  Locale.setDefault(Locale.ENGLISH)

  // Need to set this manually, as we are not inside OSGi
  val version = s"${Semester.current.year}.2.2"
  System.setProperty("edu.gemini.model.p1.schemaVersion", version)
  System.setProperty(classOf[Workspace].getName + ".fonts.shrunk", "true")

  // Set manually AGS
  AgsRobot.ags = Some(AgsHttpClient("gnauxodb.gemini.edu", 8443))

  // Create workspace with a null bundle context, it internally checks if it is null
  val workspace = new Workspace(null)
  workspace.open()

  def newShell(model: Model, file: Option[File]): Unit = {
    val adv = new ShellAdvisor(version, model, file, newShell, locale)
    val shell = workspace.createShell(adv)
    shell.open()
  }
  newShell(Model.empty, None)
}
