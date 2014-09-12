package edu.gemini.osgi.tools.app

import Configuration.Distribution
import Configuration.Distribution._

import java.io.{FileReader, FileInputStream, File}
import java.util.jar.{Manifest, JarFile}

object AppBuilder {

  val Instances = Map(
    MacOS   -> MacDistHandler,
    Test    -> TestDistHandler,
    Windows -> WinDistHandler,
    Linux32 -> new GenericUnixDistHandler(true, Some("linux/JRE32_1.6")),
    Linux64 -> new GenericUnixDistHandler(true, Some("linux/JRE1.6")) /*,
    Solaris -> new GenericUnixDistHandler(true, Some("solaris/JRE1.6")) */)

  val LoggingPatternProp = "java.util.logging.FileHandler.pattern"
}

class AppBuilder(rootDir: File, solver: Configuration => Map[BundleSpec, (File, Manifest)], jreDir: Option[File], dists: Set[Distribution] = Distribution.values, log: sbt.Logger, appProjectBaseDir: File) {
  import AppBuilder._

  def build(app: Application) {

      // val manifestMap: List[(File, Manifest)] = 
      //   knownBundles.map(f => (f, new JarFile(f).getManifest))

      // def resolver(bs: BundleSpec): (File, Manifest) =
      //   manifestMap.find(kv => matches(bs.name, bs.version.toString, kv._2)).getOrElse(sys.error("Bundle not found: " + bs))

      def outDir(version:String): File =
        (rootDir /: List(app.id, version))(mkdir)

      def version(c:Configuration): String = 
        app.version
        // solver(c).values.find { case (file, mf) =>
        //   val symName = mf.getMainAttributes.getValue("Bundle-SymbolicName").split(";")(0)
        //   symName == "edu.gemini.spModel.core"
        // } map { case (file, mf) =>
        //   if (app.version.isDefined)
        //     sys.error("Version must NOT be defined for app '" + app.id + "' because the version is provided by 'edu.gemini.spModel.core'.")
        //   val l = new java.net.URLClassLoader(Array(file.toURI.toURL), getClass.getClassLoader)
        //   val c = l.loadClass("edu.gemini.spModel.core.Version")
        //   c.getField("current").get(null).toString
        // } getOrElse {
        //   app.version.map(_.toString).getOrElse(sys.error("app '" + app.id + "' must include 'edu.gemini.spModel.core' OR define <app version=..."))
        // }

      for {
        c <- app.configs
        d <- c.distribution if dists.contains(d)
      } Instances.get(d) match {
        case Some(b) =>
          val v = version(c)
          log.info("> %s %s - %s (%s)".format(app.name, v, d, c.id))
          val dDir = mkdir(outDir(v), d.toString)
          val cDir = {
            val f = new File(dDir, c.id)
            if (f.exists())
              rm(f)
            mkdir(dDir, c.id)
          }
          b.build(cDir, jreDir, app.meta, v, c, d, solver(c), appProjectBaseDir)
        case None => log.warn("no application builder is available for distribution platform " + d)
      }

  }

}

trait DistHandler {

  def build(outDir: File, jreDir: Option[File], meta: ApplicationMeta, version:String, config: Configuration, d: Distribution, solution: Map[BundleSpec, (File, Manifest)], appProjectBaseDir: File): Unit

  protected def buildCommon(rootDir: File, meta: ApplicationMeta, version:String, config: Configuration, d: Configuration.Distribution, solution: Map[BundleSpec, (File, Manifest)], appProjectBaseDir: File) {

    def mapper(b: BundleSpec) = solution(b)._1

    // All bundles required for this configuration
    val fullClosure = solution.keySet

    // fullClosure.foreach(println)

    // Framework bundle
    val (fw, nfw) = fullClosure.partition(bs => config.startLevel(bs) == 0)

    // Must have a framework bundle!
    if (fw.size != 1)
      sys.error("Expected exactly one bundle with startlevel 0; found " + fw)

    // Framework Bundle
    copy(mapper(fw.head), rootDir)

    // App bundles
    val bundleDir = mkdir(rootDir, config.framework.BundleDirName)
    nfw.map(mapper).foreach { f => copy(f, bundleDir) }

    // Config Directory
    val destConfigDir = mkdir(rootDir, config.framework.ConfigDirName)
    write(new File(destConfigDir, config.framework.ConfigFileName)) { os =>

      // BetterProperties keeps things in order and knows how to escape strings
      val props = new BetterProperties
      config.props foreach { case (k, v) => props.put(k, v) }
      config.framework.bundleProps(nfw.toList, solution, config.startLevel).foreach { case (k, v) => props.put(k, v) }
      props.save(os, "%s Configuration for %s v%s (%s)".format(config.framework, meta.name, version, d))
    }

    def cpDir(s: File, d: File): Unit = 
      s.listFiles.foreach { copy(_, d) }

    // Copy everything in the "conf" directory
    val srcConfigDir = new File(appProjectBaseDir, "conf")
    if (srcConfigDir.exists) cpDir(srcConfigDir, destConfigDir)

    // If the logging.properties file exists and the log pattern is specified in
    // the config, copy logging.properties again setting the pattern to the
    // configured value.
    val logProps = new File(srcConfigDir, "logging.properties")
    if (logProps.exists()) {
      config.log foreach { pat =>
        val props = new BetterProperties
        val rdr = new FileReader(logProps)
        try { props.load(rdr)} finally { rdr.close() }

        props.setProperty("java.util.logging.FileHandler.pattern", pat)
        write(new File(destConfigDir, "logging.properties")) { os =>
          props.save(os, s"Updated logging properties with log directory pattern: $pat")
        }
      }
    }

    // Copy everything in the "doc" directory
    val srcDocDir = new File(appProjectBaseDir, "doc")
    if (srcDocDir.exists) cpDir(srcDocDir, mkdir(rootDir, "doc"))
  }

}




