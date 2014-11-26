import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.obslog"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/pax-web-jetty-bundle-1.1.13.jar"),
  new File(baseDirectory.value, "../../lib/bundle/pax-web-jsp-1.1.13.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.obslog.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.exportPackage := Seq()

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
  "*")

OsgiKeys.additionalHeaders +=
  ("Web-ContextPath" -> "obslog")

