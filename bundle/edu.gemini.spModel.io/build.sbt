import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.spModel.io"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % ScalaZVersion)

osgiSettings

ocsBundleSettings

fork in Test := true

OsgiKeys.bundleActivator := Some("edu.gemini.spModel.io.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.spModel.io",
  "edu.gemini.spModel.io.updater",
  "edu.gemini.spModel.io.app",
  "edu.gemini.spModel.io.ocs3")
