import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.p1monitor"

version := pitVersion.value.toOsgiVersion

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"))

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % "2.11.7",
  "org.scalaz" %% "scalaz-core" % "7.1.6",
  "org.scalaz" %% "scalaz-concurrent" % "7.1.6",
  "org.scalaz" %% "scalaz-effect" % "7.1.6")


ocsBundleSettings // defined in top-level project/ folder

osgiSettings // from the sbt-osgi plugin

OsgiKeys.bundleActivator := Some("edu.gemini.p1monitor.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq()
