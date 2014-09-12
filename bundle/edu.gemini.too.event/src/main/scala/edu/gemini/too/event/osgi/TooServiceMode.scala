package edu.gemini.too.event.osgi

import org.osgi.framework.BundleContext

import scalaz._
import Scalaz._

/**
 * The edu.gemini.too.service bundle is used in both `Client` and `Service`
 * modes. The client includes the bundle in `Client` mode to use the API while
 * in the ODB the bundle runs in `Service` mode to monitor for ToO observation
 * status transitions.
 */
 // The alternative would be to split the bundle in three (one with just the
 // API, one with just the client code, and one with OSGI and service impl but
 // that seems a bit excessive maybe).
sealed trait TooServiceMode {
  def name: String
}

object TooServiceMode {
  case object Client extends TooServiceMode {
    val name = "client"
  }

  case object Service extends TooServiceMode {
    val name = "service"
  }

  val Property = "edu.gemini.too.event.mode"
  val All = List(Client, Service)

  def apply(ctx: BundleContext): TooServiceMode =
    Option(ctx.getProperty(Property)).flatMap(s => All.find(_.name === s)) | Client
}
