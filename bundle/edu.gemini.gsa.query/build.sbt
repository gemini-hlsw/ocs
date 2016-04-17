import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.gsa.query"

// version set in ThisBuild

libraryDependencies ++= Seq(
  "org.scalaz"  %% "scalaz-core"       % ScalaZVersion,
  "org.scalaz"  %% "scalaz-concurrent" % ScalaZVersion,
  "io.argonaut" %% "argonaut"          % "6.2-M1")

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.gsa.query")

OsgiKeys.privatePackage := Seq()
