import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.sp.vcs.log"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/scala-reflect-2.10.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-concurrent_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-effect_2.10-7.1.6.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-stream_2.10-0.7.2a.jar"),
  new File(baseDirectory.value, "../../lib/bundle/doobie-core_2.10-0.2.4-SNAPSHOT.jar"),
  new File(baseDirectory.value, "../../lib/bundle/shapeless_2.10-2.2.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scodec-bits_2.10-1.0.9.jar"),
  new File(baseDirectory.value, "../../lib/bundle/quasiquotes_2.10-2.0.1.jar"),
  new File(baseDirectory.value, "../../lib/bundle/h2-1.3.170.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.6",
  "org.scalaz" %% "scalaz-effect" % "7.1.6")
osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := Some("edu.gemini.sp.vcs.log.osgi.Activator")

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.sp.vcs.log")

// required for shapeless on 2.10.5
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)

// thread safety issue for object initialization in 2.10 ... this is a workaround
parallelExecution in Test := false
