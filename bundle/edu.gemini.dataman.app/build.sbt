import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.dataman.app"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/argonaut_2.10-6.0.4.jar"),
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-concurrent_2.10-7.0.5.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.dataman.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.datasetfile",
  "edu.gemini.datasetfile.impl",
  "edu.gemini.dirmon",
  "edu.gemini.dirmon.util",
  "edu.gemini.datasetrecord",
  "edu.gemini.datasetrecord.impl",
  "edu.gemini.datasetrecord.impl.trigger",
  "edu.gemini.datasetrecord.impl.store",
  "edu.gemini.datasetrecord.util")


OsgiKeys.privatePackage := Seq(
  "edu.gemini.dataman.*",
  "edu.gemini.datasetfile.*",
  "edu.gemini.datasetrecord.*",
  "edu.gemini.dirmon.*"
  )
