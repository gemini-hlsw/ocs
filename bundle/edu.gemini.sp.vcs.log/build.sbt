import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.sp.vcs.log"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/com-mchange-c3p0_2.10-0.9.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scala-slick_2.10-1.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.0.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.0.5.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.sp.vcs.log.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.sp.vcs.log")

        
