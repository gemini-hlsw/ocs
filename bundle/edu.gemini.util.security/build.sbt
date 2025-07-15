import OcsKeys._

// note: inter-project dependencies are declared at the top, in projects.sbt

name := "edu.gemini.util.security"

// version set in ThisBuild
unmanagedJars in Compile ++= Seq(
  new File(baseDirectory.value, "../../lib/bundle/javax-servlet_2.10-2.5.0.jar")
)

libraryDependencies ++= Seq(
  "org.scalaz"             %% "scalaz-core" % ScalaZVersion,
  "org.scalaz"             %% "scalaz-effect" % ScalaZVersion,
  "org.scalaz"             %% "scalaz-concurrent" % ScalaZVersion,
  "org.tpolecat"           %% "doobie-core" % "0.3.0a",
  "org.scala-lang"         %  "scala-reflect" % scalaVersion.value,
  "org.scala-lang"         %  "scala-compiler" % scalaVersion.value
  )

osgiSettings

ocsBundleSettings

OsgiKeys.bundleSymbolicName := name.value

OsgiKeys.exportPackage := Seq(
  "edu.gemini.util.security.permission",
  "edu.gemini.util.security.principal",
  "edu.gemini.util.security.auth",
  "edu.gemini.util.security.policy",
  "org.h2",
  "org.h2.api",
  "org.h2.command",
  "org.h2.command.ddl",
  "org.h2.command.dml",
  "org.h2.constant",
  "org.h2.constraint",
  "org.h2.engine",
  "org.h2.expression",
  "org.h2.index",
  "org.h2.jmx",
  "org.h2.message",
  "org.h2.mvstore",
  "org.h2.mvstore.cache",
  "org.h2.mvstore.type",
  "org.h2.mvstore.rtree",
  "org.h2.store.fs",
  "org.h2.res",
  "org.h2.result",
  "org.h2.schema",
  "org.h2.security",
  "org.h2.store",
  "org.h2.table",
  "org.h2.tools",
  "org.h2.server",
  "org.h2.server.pg",
  "org.h2.server.web",
  "org.h2.compress",
  "org.h2.bnf",
  "org.h2.jdbc",
  "org.h2.upgrade",
  "org.h2.util",
  "org.h2.value",
  "org.h2.store",
  "org.h2.table",
  "edu.gemini.util.security.auth.keychain")

initialCommands := """
  import edu.gemini.util.security.auth.keychain._
  import scalaz._, Scalaz._, scalaz.effect._
  import doobie.imports._
  import java.io.File
  import edu.gemini.util.security.principal._
"""
