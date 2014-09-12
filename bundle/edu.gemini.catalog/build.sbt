import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.catalog"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.catalog.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.privatePackage := Seq(
  "edu.gemini.catalog.*",
  "jsky.catalog.*"
)

OsgiKeys.exportPackage := Seq(
  "edu.gemini.catalog.api",
  "edu.gemini.catalog.impl",
  "edu.gemini.catalog.skycat",
  "edu.gemini.catalog.skycat.binding.adapter",
  "edu.gemini.catalog.skycat.binding.skyobj",
  "edu.gemini.catalog.skycat.table",
  "jsky.catalog",
  "jsky.catalog.astrocat",
  "jsky.catalog.skycat",
  "jsky.catalog.util"
)


