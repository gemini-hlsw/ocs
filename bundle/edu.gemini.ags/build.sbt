import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.ags"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/nom-tam-fits_2.10-0.99.3.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-logging_2.10-1.1.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/breeze_2.11-0.12.jar"),
  new File(baseDirectory.value, "../../lib/bundle/breeze-macros_2.11-0.12.jar"),
  new File(baseDirectory.value, "../../lib/bundle/fommil-core.1.1.2.jar"),
  new File(baseDirectory.value, "../../lib/bundle/arpack_combined_all-0.1.jar")
)

libraryDependencies ++= Seq(
  "org.apache.commons" %  "commons-math3" % "3.2" % "test",
  "com.chuusai"        %% "shapeless"     % "2.3.2")


osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.ags.gems.*",
  "edu.gemini.ags.api.*",
  "edu.gemini.ags.conf.*",
  "edu.gemini.ags.impl.*")

parallelExecution in Test := false
