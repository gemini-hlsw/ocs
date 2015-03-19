import OcsKeys._

name := "ocs"

ocsVersion in ThisBuild := OcsVersion("2015B", true, 1, 1, 2)

pitVersion in ThisBuild := OcsVersion("2015B", false, 2, 1, 0)

// Bundles by default use the ocsVersion; this is overridden in bundles used only by the PIT
version in ThisBuild := ocsVersion.value.toOsgiVersion

scalaVersion in ThisBuild := "2.10.4"

// Note that this is not a standard setting; it's used for building IDEA modules.
javaVersion in ThisBuild := "1.7" 

scalacOptions in ThisBuild ++= Seq(
  // "-deprecation",
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
  "-Yno-adapted-args", 
  "-Ywarn-all"
  // "-Ywarn-dead-code"        // N.B. doesn't work well with bottom
  // "-Ywarn-numeric-widen",   
  // "-Ywarn-value-discard"   
)

javacOptions in ThisBuild ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:all,-serial,-path,-deprecation,-unchecked,-fallthrough" // TOOD: turn all on except maybe -serial and -path
)

// Use managed dependencies for tests; everyone gets JUnit, ScalaCheck, and Specs2
libraryDependencies in ThisBuild ++= Seq(
  "junit"           % "junit"           % "4.11"   % "test",
  "com.novocode"    % "junit-interface" % "0.9"    % "test",
  "org.scalacheck" %% "scalacheck"      % "1.10.1" % "test",
  "org.specs2"     %% "specs2"          % "1.12.3" % "test",
  "org.scalatest"   % "scalatest_2.10"  % "2.0"    % "test"
)

// Don't build scaladoc (for now)
publishArtifact in (ThisBuild, packageDoc) := false

// Don't build package source (for now)
publishArtifact in (ThisBuild, packageSrc) := false

// No poms
publishMavenStyle in ThisBuild := false

// > dash -s List
commands += {
  import scala.sys.process._
  import complete.DefaultParsers._
  val stuff = Seq(("-6", "java6",  "Java SE6"),
                  ("-7", "java7",  "Java SE7"),
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
