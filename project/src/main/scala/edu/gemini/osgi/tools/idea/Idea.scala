package edu.gemini.osgi.tools.idea

import java.io.File
import edu.gemini.osgi.tools.ProjectDirs
import edu.gemini.osgi.tools.app.Application

/** Contains all the context relevant to setting up an Idea project. */
class Idea(val pd: ProjectDirs, crossTarget: File, appl: Application, val configIdOpt: Option[String]) {

  val appName       = appl.name
  val appDir        = pd.app(appl.id)
  val app           = new IdeaApp(pd.bundle, pd.libRoot, appl, configIdOpt)
  val projDir       = new File(appDir, "idea")
  val distDir       = projDir //new File(projDir, "bin")
  val distBundleDir = new File(distDir, "bundle")
  val distOutDir    = new File(distDir, "out")

  def distBundleFile(bl: BundleLoc): File =
    new File(distBundleDir, "%s-%s.jar".format(bl.name, bl.version))

}

