import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.seqexec.web.shared"

version := ocsVersion.value.toOsgiVersion

libraryDependencies ++= Seq(
  "org.scalaz"  %% "scalaz-core" % "7.1.6"
)

osgiSettings

ocsBundleSettings