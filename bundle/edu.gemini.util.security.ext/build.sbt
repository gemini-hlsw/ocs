import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.security.ext"

// version set in ThisBuild

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "2.0.0-M2"
  )

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.util.security.ext.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("*")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.util.security.ext.auth.ui",
  "edu.gemini.util.security.ext.osgi",
  "edu.gemini.util.security.ext.mail")

