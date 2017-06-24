import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.itc.shared"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.core-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org.osgi.enterprise-5.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-jfree_2.10-1.0.14.jar")
)

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "2.0.0-M2"
  )

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.itc.shared.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.exportPackage := Seq(
  "edu.gemini.itc.service",
  "edu.gemini.itc.shared"
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

