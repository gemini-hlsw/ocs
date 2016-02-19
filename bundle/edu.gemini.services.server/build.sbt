import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.services.server"

// version set in ThisBuild

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "2.0.0-M2",
  "org.scalaz" %% "scalaz-core" % "7.1.6",
  "org.scalaz" %% "scalaz-effect" % "7.1.6")

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.services.server.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.importPackage := Seq("!org.apache.avalon.*,!org.apache.commons.codec.binary.*,!org.apache.log.*,!org.apache.log4j.*,*")
        
