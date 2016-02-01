import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.horizons.server"

// version set in ThisBuild

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.horizons.server.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.horizons.server.backend"
)

initialCommands :=
  """
    |import edu.gemini.spModel.core._
    |import edu.gemini.horizons.api._
    |import edu.gemini.horizons.server.backend._
  """.stripMargin
