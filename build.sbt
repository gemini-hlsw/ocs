import OcsKeys._

name := "ocs"

organization in Global := "edu.gemini.ocs"

ocsVersion in ThisBuild := OcsVersion("2016A", true, 1, 1, 1)

pitVersion in ThisBuild := OcsVersion("2016B", true, 2, 1, 0)

// Bundles by default use the ocsVersion; this is overridden in bundles used only by the PIT
version in ThisBuild := ocsVersion.value.toOsgiVersion

scalaVersion in ThisBuild := "2.11.7"

// Note that this is not a standard setting; it's used for building IDEA modules.
javaVersion in ThisBuild := {
  val expected = "1.8" 
  val actual   = sys.props("java.version")
  if (!actual.startsWith(expected))
    println(s"""
      |***
      |***                   INCORRECT JAVA RUNTIME VERSION 
      |***
      |***  The build expects version $expected, but you are running $actual.
      |***  Change the VM you're using to run sbt to avoid confusion and strange behavior.
      |***
    """.stripMargin)
  expected
}

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",  // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:reflectiveCalls", // TODO: turn this off
  "-language:postfixOps",      // TODO: turn this off
  "-target:jvm-1.7",
  "-unchecked",
  // "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args"
  // "-Ywarn-all", C.Q. Not supported on scala 2.11
  // "-Ywarn-dead-code"        // N.B. doesn't work well with bottom
  // "-Ywarn-numeric-widen",   
  // "-Ywarn-value-discard"   
)

javacOptions in ThisBuild ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
  "-Xlint:all,-serial,-path,-deprecation,-unchecked,-fallthrough" // TOOD: turn all on except maybe -serial and -path
)

// Use managed dependencies for tests; everyone gets JUnit, ScalaCheck, and Specs2
libraryDependencies in ThisBuild ++= Seq(
  "junit"           % "junit"           % "4.11"   % "test",
  "com.novocode"    % "junit-interface" % "0.9"    % "test",
  "org.scalacheck" %% "scalacheck"      % "1.11.0" % "test",
  "org.specs2"     %% "specs2-core"     % "3.6.6"  % "test",
  "org.specs2"     %% "specs2-matcher-extra" % "3.6.6" % "test",
  "org.specs2"     %% "specs2-scalacheck"    % "3.6.6" % "test",
  "org.scalatest"  %% "scalatest"  % "2.2.5"    % "test"
)

// Required for specs2
scalacOptions in Test ++= Seq("-Yrangepos")

// Don't build scaladoc (for now)
publishArtifact in (ThisBuild, packageDoc) := false

// Don't build package source (for now)
publishArtifact in (ThisBuild, packageSrc) := false

publishTo in Global := {
    val repo = if (isSnapshot.value) {
      "libs-snapshot-local"
    } else {
      "libs-release-local"
    }
    Some("Gemini Artifactory" at s"http://sbfosxdev-mp1.cl.gemini.edu:8081/artifactory/$repo")
  }

// No poms
publishMavenStyle in ThisBuild := false

// > dash -s List
commands += {
  import scala.sys.process._
  import complete.DefaultParsers._
  val stuff = Seq(("-6", "java6",  "Java SE6"),
                  ("-7", "java7",  "Java SE7"),
                  ("-8", "java8",  "Java SE8"),
                  ("-s", "scala",  "Scala"),
                  ("-z", "scalaz", "scalaz"))
  val option = stuff.map { case (o, d, _) => o ^^^ d } .reduceLeft(_ | _)
  val parser = token(Space ~> option) ~ token(Space ~> StringBasic)
  val help = Help.briefDetail(stuff.map { case (o, _, t) => (s"$o <word>", s"Search in $t") })
  Command("dash", help)(_ => parser) { case (state, (set, topic)) =>
    s"/usr/bin/open dash://$set:$topic".!
    state
  }
}
