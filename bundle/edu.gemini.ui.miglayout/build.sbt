import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.ui.miglayout"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/miglayout-core-4.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/miglayout-swing-4.2.jar")
)

libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "2.0.0-M2"

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.ui.miglayout")
