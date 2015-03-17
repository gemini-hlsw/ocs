import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.itc"

// version set in ThisBuild

// TODO: once everything is split properly the dependency on javax-servlet can go away
unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org.osgi.enterprise-5.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.itc.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

// TODO: for now export all packages
// TODO: once we have a clearer separation of everything we shouldn't need to export everything here anymore
OsgiKeys.exportPackage := Seq(
  "edu.gemini.itc.acqcam",
  "edu.gemini.itc.altair",
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
