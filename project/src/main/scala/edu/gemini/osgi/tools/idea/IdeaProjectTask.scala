package edu.gemini.osgi.tools.idea

import reflect.BeanProperty
import edu.gemini.osgi.tools._
import edu.gemini.osgi.tools.app.Application
import java.io.File

/** A task to generate an Idea project. */
object IdeaProjectTask { 

  def go(base: File, crossTarget: File, imls: List[File], app: Application, configOpt: String, si: sbt.ScalaInstance, javaVersion: String, logger: sbt.Logger): Unit = {

    val idea = new Idea(new ProjectDirs(base), crossTarget, app, Some(configOpt))

    def log(f: File, fileType: String): Unit = {
      def verb(f: File): String = if (f.exists()) "Updating" else "Creating"
      logger.info("%s IDEA %s %s".format(verb(f), fileType, f.getAbsolutePath))
    }

    // Create the project file itself.
    val ip = new IdeaProject(idea, si, imls)
    log(ip.iprFile, "Project")
    if (!idea.projDir.exists() && !idea.projDir.mkdirs()) {
      sys.error("Could not mkdir '%s'".format(idea.projDir))
    }
    FileUtils.save(ip.project(javaVersion), ip.iprFile)

    /*************

        RCN: so, setting up Felix to run in IDEA is an issue because we no longer have bundle manifests
        and can't construct bundle artifacts. There exists an OSGi plugin for IDEA that can do this
        based on information we already have (it uses BND) but it doesn't work in IDEA 13. And we can't
        generate the manifests with the project because BND inspects your imports, which will commonly
        change during development. So we're kind of stuck right now without a way to run in IDEA.
    
    *************/

    // // Create the felix module to run the OSGi app.
    // val ifm = new IdeaFelixModule(idea)
    // log(ifm.imlFile, "felix module")
    // FileUtils.save(ifm.module, ifm.imlFile)

    // // Create the felix properties file.
    // val ifc = new IdeaFelixConfig(idea)
    // log(ifc.configFile, "felix config")
    // FileUtils.save(ifc.formattedConfig, ifc.configFile)

    // // Create all the modules for the source bundles that make up the project.
    // val pd   = ProjectDependencies(base, app)
    // val mods = idea.app.srcBundles map { bl => new IdeaModule(bl.name, bl.version, bl.loc, pd.classpath(bl.toBundleVersion), testLibs(base)) }
    // mods foreach { m =>
    //   log(m.imlFile, "Module")
    //   FileUtils.save(m.module, m.imlFile)
    // }

    // // Copy any extra configuration
    // val srcConfDir = new File(idea.appDir, "conf")
    // if (srcConfDir.exists && srcConfDir.isDirectory) {
    //   logger.info("Copy " + srcConfDir.getPath + " to " + idea.projDir)
    //   edu.gemini.osgi.tools.app.copy(srcConfDir, idea.projDir)
    // }

  }
  
}
