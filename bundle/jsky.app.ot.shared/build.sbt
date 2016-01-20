import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.app.ot.shared"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"))

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.6")

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("jsky.app.ot.shared.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "jsky.app.ot.shared.gemini.obscat",
  "jsky.app.ot.shared.progstate",
  "jsky.app.ot.shared.spModel.util",
  "jsky.app.ot.shared.vcs")

