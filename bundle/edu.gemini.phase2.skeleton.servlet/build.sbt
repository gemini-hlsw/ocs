import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.phase2.skeleton.servlet"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-actors_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-library_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.phase2.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.privatePackage := Seq(
  "edu.gemini.phase2.*"
)

OsgiKeys.exportPackage := Seq(
  "edu.gemini.phase2.template.factory.api",
  "edu.gemini.phase2.skeleton.auxfile",
  "edu.gemini.phase2.skeleton.factory")

OsgiKeys.importPackage := Seq("!javax.portlet.*,*")

