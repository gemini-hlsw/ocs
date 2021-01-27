import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.model.p1.pdf"

version := pitVersion.value.toOsgiVersion

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
  )

osgiSettings

ocsBundleSettings

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.model.p1.pdf")

fork in run := true

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-logging_2.10-1.1.0.jar")
)
