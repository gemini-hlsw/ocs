import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.gsa.query"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/argonaut_2.11-6.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-core_2.11-1.1.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-macro_2.11-1.1.0.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.6",
  "org.scalaz" %% "scalaz-concurrent" % "7.1.6")

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.gsa.query")

OsgiKeys.privatePackage := Seq()
