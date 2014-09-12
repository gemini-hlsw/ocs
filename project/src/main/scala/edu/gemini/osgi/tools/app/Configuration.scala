package edu.gemini.osgi.tools.app

import java.io.File

object Configuration {
  object Distribution extends Enumeration {
    val Test, MacOS, Windows, Linux32, Linux64 /*, Solaris */ = Value
  }
  type Distribution = this.Distribution.Value
}

case class Configuration(
  id:String,
  distribution: List[Configuration.Distribution] = Nil,
  args: List[String] = Nil,
  vmargs: List[String] = Nil,
  bundles: List[BundleSpec] = Nil,
  props: Map[String, String] = Map.empty,
  icon: Option[File] = None,
  log: Option[String] = None,
  script: Option[File] = None,
  extending: List[Configuration] = Nil) {

  // Default; this can be overridden in instances
  def startLevel(b: BundleSpec): Int =
    b.name.split("\\.").toList match {
      case "edu" :: "gemini" :: "osgi" :: "main" :: Nil => 0
      case "org" :: "osgi"       :: _ => 5
      case "org" :: "scala-lang" :: _ => 10
      case "org" :: "scalaz"     :: _ => 15
      case "edu" :: "gemini"     :: _ => 50
      case "org" :: "jsky"       :: _ => 50
      case _                          => 40
    }

  def extending(c: Configuration) = copy(
    args = c.args ++ args,
    vmargs = c.vmargs ++ vmargs,
    bundles = c.bundles ++ bundles,
    props = c.props ++ props,
    log = log.orElse(c.log),
    extending = extending :+ c
  )

  def extending(cs: List[Configuration]): Configuration = 
    (this /: cs)(_ extending _)


  lazy val nonFrameworkBundles = 
    bundles.filter(startLevel(_) != 0)

  lazy val framework =
    Framework.forBundle(bundles.find(startLevel(_) == 0).getOrElse(sys.error("No framework bundle in " + this)))

  def vmargsWithApp(s:String) = vmargs :+ s"-Dedu.gemini.osgi.main.app=${s}_$id"

}

