import OcsKeys._

name := "edu.gemini.epics.acm"

sourceGenerators in Compile += Def.task {
  import scala.sys.process._
  val pkg = "edu.gemini.epics.acm.generated"
  val log = state.value.log
  val gen = (sourceManaged in Compile).value
  val out = (gen /: pkg.split("\\."))(_ / _)
  val xsd = sourceDirectory.value / "main" / "resources" / "CaSchema.xsd"
  val cmd = List("xjc",
    "-d", gen.getAbsolutePath,
    "-p", pkg,
    xsd.getAbsolutePath)
  val mod = xsd.getParentFile.listFiles.map(_.lastModified).max
  val cur = if (out.exists && out.listFiles.nonEmpty) out.listFiles.map(_.lastModified).min else Int.MaxValue
  if (mod > cur) {
    out.mkdirs
    val err = cmd.run(ProcessLogger(log.info(_), log.error(_))).exitValue
    if (err != 0) sys.error("xjc failed")
  }
  out.listFiles.toSeq
}.taskValue

libraryDependencies ++= Seq(
  "xmlunit" % "xmlunit" % "1.5"
)

fork in test := true

parallelExecution in test := false

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("")

OsgiKeys.exportPackage := Seq(
  "edu.geminu.epics.acm"
)
