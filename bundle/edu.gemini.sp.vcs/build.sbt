import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.sp.vcs"

// version set in ThisBuild

unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-actors_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org.scala-lang.scala-library_2.10.1.v20130302-092018-VFINAL-33e32179fd.jar"),
  new File(baseDirectory.value, "../../lib/bundle/scalaz-core_2.10-7.0.5.jar"),
  new File(baseDirectory.value, "../../lib/bundle/org-dom4j_2.10-1.5.1.jar")
)

osgiSettings

ocsBundleSettings

OsgiKeys.bundleActivator := None

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.dynamicImportPackage := Seq("*")

OsgiKeys.exportPackage := Seq(
  "edu.gemini.sp.vcs")

//
// Tests in this project monkey with the VM's security manager and cannot be
// run in parallel.  This bit makes these tests run sequentially though most
// likely in parallel with tests in other projects.  So, this is a half-assed
// solution that will likely only make problems less likely to appear.
//
// Not for the faint of heart, but there may be a real solution here:
//
// http://www.scala-sbt.org/0.13.2/docs/Detailed-Topics/Parallel-Execution.html
//
// or, who knows, maybe here:
//
// http://www.scala-sbt.org/release/docs/Detailed-Topics/Testing#application-to-parallel-execution
//

// parallelExecution in Test := false        
