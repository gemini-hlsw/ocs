import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.shared.gui"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/com-jgoodies-looks_2.10-2.4.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-jdesktop-swingx_2.10-1.6.4.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-swing_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"))

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.shared.gui",
  "edu.gemini.shared.gui.bean",
  "edu.gemini.shared.gui.calendar",
  "edu.gemini.shared.gui.dialog",
  "edu.gemini.shared.gui.dialog.canned",
  "edu.gemini.shared.gui.textComponent",
  "edu.gemini.shared.gui.goodies",
  "edu.gemini.shared.gui.monthview",
  "edu.gemini.shared.gui.propertyCtrl",
  "edu.gemini.shared.gui.text")

        
