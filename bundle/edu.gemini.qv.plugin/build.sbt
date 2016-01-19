import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.qv.plugin"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-jdesktop-swingx_2.10-1.6.4.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-jfree_2.10-1.0.14.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.1.6.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.qv.plugin.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  )
