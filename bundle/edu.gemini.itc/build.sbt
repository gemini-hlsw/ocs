import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.itc"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org.osgi.enterprise-5.0.0.jar")
)

libraryDependencies ++= Seq(
  "org.scala-lang" %  "scala-compiler" % scalaVersion.value,
  "org.typelevel"  %% "squants"  % "1.4.0"
  )

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.itc.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.exportPackage := Seq(
  "edu.gemini.itc.service",
  "edu.gemini.itc.acqcam",
  "edu.gemini.itc.altair",
  "edu.gemini.itc.base",
  "edu.gemini.itc.flamingos2",
  "edu.gemini.itc.gems",
  "edu.gemini.itc.gmos",
  "edu.gemini.itc.gnirs",
  "edu.gemini.itc.gsaoi",
  "edu.gemini.itc.michelle",
  "edu.gemini.itc.nifs",
  "edu.gemini.itc.niri",
  "edu.gemini.itc.operation",
  "edu.gemini.itc.parameters",
  "edu.gemini.itc.shared",
  "edu.gemini.itc.trecs"
)

OsgiKeys.importPackage := Seq(
  "!com.google.inject.cglib.asm.util.*",
  "!com.lowagie.text.*",
  "!com.opensymphony.*",
  "!javax.portlet.*",
  "!javax.servlet.jsp.el.*",
  "!javax.servlet.jsp.jstl.*",
  "!junit.*",
  "!nu.xom.*",
  "!org.aopalliance.*",
  "!org.apache.avalon.*",
  "!org.apache.log.*",
  "!org.apache.log4j.*",
  "!org.apache.struts.*",
  "!org.apache.taglibs.*",
  "!org.apache.tapestry.*",
  "!org.apache.tools.*",
  "!org.apache.velocity.*",
  "!org.apache.xml.*",
  "!org.apache.xpath.*",
  "!org.jdom.*",
  "!org.mozilla.*",
  "!org.python.*",
  "!org.springframework.*",
  "!org.testng.*",
  "!uk.ltd.getahead.*",
  "!org.w3c.dom.traversal",
  "!sun.reflect.generics.reflectiveObjects.*",
  "*")
