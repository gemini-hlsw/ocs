import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.horizons.api"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % ScalaZVersion,
  "org.scalaz" %% "scalaz-effect" % ScalaZVersion)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.horizons.api",
  "edu.gemini.horizons.server.backend"
)

initialCommands :=
  """
    |import edu.gemini.spModel.core._
    |import edu.gemini.horizons.server.backend.HorizonsService2._
  """.stripMargin
