package edu.gemini.osgi.tools.idea

import java.io.File
import edu.gemini.osgi.tools.app.Configuration.Distribution.Test

import IdeaApp._
import edu.gemini.osgi.tools._
import app.{Configuration, Application}

/**
 * Defines an application for the purpose of creating or updating an Idea
 * project.
 */
class IdeaApp(bundleDir: String => File, libRoot: File, app: Application, configIdOpt: Option[String]) {

  val id      = app.id
  val version = app.version
  val config  = getConfig(app, configIdOpt)
  val props   = config.props
  val vmargs  = config.vmargs

  val srcBundles: Set[BundleLoc] =
    (for {
      b <- config.bundles
      dir = bundleDir(b.name) if dir.exists()
    } yield BundleLoc(b, dir)).toSet

  val libBundles: Set[BundleLoc] = {
    val all = new LibraryBundles(libRoot)
    val bs  = config.bundles.toSet
    for {
      b   <- bs
      jar <- all.bundleMap.get(BundleKey(b.name, b.version))
    } yield BundleLoc(b, jar)
  }

  // val startBlocks: List[(Int, Set[BundleLoc])] = {
  //   val m = (srcBundles ++ libBundles).groupBy(_.startLevel).map {
  //     case (level, bundles) => (level, bundles)
  //   }
  //   m.toList.sortBy(_._1)
  // }

}

object IdeaApp {

  private def namedConfig(app: Application, id: String): Option[Configuration] = {
    val c = app.configs.find(_.id == id)
    if (!c.isDefined) println("Couldn't find configuration '%s', will use default test app config".format(id))
    c
  }

  def getConfig(app: Application, idOpt: Option[String]): Configuration =
    idOpt.flatMap(i => namedConfig(app, i)) orElse 
    app.configs.find(isIdeaConfig) getOrElse 
    sys.error("Test app config not found.")
 
  // TODO: Make Idea distribution
  private def isIdeaConfig(c: Configuration): Boolean =
    c.distribution.headOption.exists(_ == Test)

}