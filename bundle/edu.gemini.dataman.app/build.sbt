import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.dataman.app"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/argonaut_2.11-6.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-core_2.11-1.2.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/monocle-macro_2.11-1.2.1.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-concurrent" % ScalaZVersion)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.dataman.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()


OsgiKeys.privatePackage := Seq(
  "edu.gemini.dataman.*"
  )
