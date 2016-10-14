import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "jsky.app.ot"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/osgi.cmpn-4.3.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/com-jgoodies-looks_2.10-2.4.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/nom-tam-fits_2.10-0.99.3.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-apache-commons-httpclient_2.10-2.0.0.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-jfree_2.10-1.0.14.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/argonaut_2.11-6.2.jar")
)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % "2.11.7",
  "org.scalaz" %% "scalaz-core" % ScalaZVersion,
  "org.scalaz" %% "scalaz-effect" % ScalaZVersion)

osgiSettings

ocsBundleSettings

OsgiKeys.privatePackage := Seq(
  "edu.gemini.ags.gui.*",
  "edu.gemini.mascot.gui.*",
  "edu.gemini.mask.*",
  "edu.gemini.shared.cat.*",
  "edu.gemini.catalog.ui.*",
  "jsky.app.jskycat.*",
  "jsky.app.ot.vcs.*",
  "jsky.app.ot",
  "jsky.app.ot.ags",
  "jsky.app.ot.itc",
  "jsky.app.ot.editor.*",
  "jsky.app.ot.osgi.*",
  "jsky.app.ot.gemini.*",
  "jsky.app.ot.log.*",
  "jsky.app.ot.modelconfig.*",
  "jsky.app.ot.nsp.*",
  "jsky.app.ot.plot",
  "jsky.app.ot.progadmin.*",
  "jsky.app.ot.scilib.*",
  "jsky.app.ot.session.*",
  "jsky.app.ot.too.*",
  "jsky.app.ot.tpe.*",
  "jsky.app.ot.ui.*",
  "jsky.app.ot.userprefs.*",
  "jsky.app.ot.util.*",
  "jsky.app.ot.viewer.*",
  "jsky.catalog.gui.*",
  "jsky.graphics.*",
  "jsky.html.*",
  "jsky.image.*",
  "jsky.navigator.*",
  "jsky.science.*",
  "jsky.timeline.*",
  "jsky.util.java2d.*"
)

OsgiKeys.bundleActivator := Some("jsky.app.ot.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.additionalHeaders +=
  ("Import-Package" -> "!Acme.JPM.Encoders,!com.sun.*.jpeg,!sun.*,*")

OsgiKeys.dynamicImportPackage := Seq("*") // hmm

OsgiKeys.exportPackage := Seq(
  "resources",
  "resources/conf",
  "resources/fonts",
  "resources/images",
  "resources/images/bullet",
  "resources/images/eclipse",
  "resources/images/pit",
  "resources/images/vcs",
  "resources/messages",
  "resources/sounds",
  "colormaps",
  "jsky/app",
  "jsky/app/i18n",
  "jsky/ot",
  "jsky/catalog/gui/i18n",
  "jsky/image/graphics/gui/i18n",
  "jsky/image/gui/i18n",
  "jsky/navigator/i18n",
  "jsky/navigator/i18n",
  "edu.gemini.ags.gui",
  "edu.gemini.mascot.gui.contour",
  "edu.gemini.shared.cat",
  "edu.gemini.shared.cat.assistant",
  "edu.gemini.shared.cat.assistant.uiedu.gemini.shared.catalog",
  "jsky.catalog.gui",
  "jsky.navigator",
  "jsky.util.java2d",
  "jsky.html",
  "jsky.graphics",
  "jsky.image.fits.gui",
  "jsky.image.graphics",
  "jsky.image.graphics.gui",
  "jsky.image.gui",
  "diva.apps",
  "diva.canvas.connector",
  "diva.canvas.demo",
  "diva.canvas.event",
  "diva.canvas.interactor",
  "diva.canvas.toolbox",
  "diva.canvas.tutorial",
  "diva.canvas",
  "diva.graph.basic",
  "diva.graph.layout",
  "diva.graph.modular",
  "diva.graph.schematic",
  "diva.graph.toolbox",
  "diva.graph.tutorial",
  "diva.graph",
  "diva.gui.toolbox",
  "diva.gui.tutorial",
  "diva.gui",
  "diva.pod.lwgraph",
  "diva.pod",
  "diva.resource",
  "diva.sketch.classification",
  "diva.sketch.demo",
  "diva.sketch.features",
  "diva.sketch.parser2d",
  "diva.sketch.rcl",
  "diva.sketch.recognition",
  "diva.sketch.toolbox",
  "diva.sketch",
  "diva.surfaces.trace",
  "diva.surfaces.tutorial",
  "diva.surfaces",
  "diva.util.aelfred",
  "diva.util.gui",
  "diva.util.java2d",
  "diva.util.jester.demo",
  "diva.util.jester",
  "diva.util.xml",
  "diva.util",
  "diva.whiteboard",
  "jsky.image",
  "jsky.image.fits",
  "jsky.image.fits.codec",
  "jsky.image.operator",
  "com.sun.media.jai.codec",
  "javax.media",
  "javax.media.jai",
  "javax.media.jai.operator",
  "javax.media.jai.registry",
  "javax.media.jai.remote",
  "javax.media.jai.tilecodec",
  "javax.media.jai.util",
  "javax.media.jai.widget",
  "jsky.timeline",
  "jsky.science",
  "jsky.science.util",
  "jsky.science.util.gui")

fork := true
