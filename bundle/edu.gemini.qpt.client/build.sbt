import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.qpt.client"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org-apache-velocity_2.10-1.4.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-jdesktop-swingx_2.10-1.6.4.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz"     %% "scalaz-concurrent" % ScalaZVersion
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.qpt.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.privatePackage := Seq(
  "edu.gemini.qpt.core.*",
  "edu.gemini.qpt.osgi.*",
  "edu.gemini.qpt.ui.*"
)

OsgiKeys.exportPackage := Seq(
  "edu.gemini.qpt.ui.html")

OsgiKeys.additionalHeaders +=
  ("Import-Package" -> "!javax.mail.util,edu.gemini.spModel.gemini.altair,edu.gemini.spModel.target.obsComp,*")
