package edu.gemini.ags.client.impl.osgi

import edu.gemini.ags.client.api.AgsClient
import org.osgi.framework.{ServiceRegistration, BundleActivator, BundleContext}
import edu.gemini.ags.client.impl.AgsHttpClient
import java.util.logging.{Level, Logger}
import Level.{INFO, WARNING}

import Activator._
import java.util.Hashtable

/**
 * Registers the AgsHttpClient as an AgsClient service implementation.
 */
class Activator extends BundleActivator {
  private var reg: Option[ServiceRegistration[AgsClient]] = None

  def start(context: BundleContext) {
    val (client, level, logmsg) = Props.extract(context) match {
      case Left(msg)    =>
        (None, WARNING, "Could not register AgsHttpClient: " + msg)
      case Right(props) =>
        (Some(AgsHttpClient(props.host, props.port)), INFO, "AgsHttpClient registered")
    }

    reg = client map { c => context.registerService(SERVICE_NAME, c, new Hashtable[String, Any]) }
    LOG.log(level, logmsg)
  }

  def stop(context: BundleContext) {
    reg foreach { _.unregister() }
    reg = None
  }
}

object Activator {
  val LOG          = Logger.getLogger(classOf[Activator].getName)
  val SERVICE_NAME = classOf[AgsClient]
}

/**
 * Extracts the AGS host and port from the bundle context, if available and
 * legally defined.
 */
private case class Props(host: String, port: Int)
private object Props {
  val HOST_PROPERTY = "edu.gemini.ags.host"
  val PORT_PROPERTY = "edu.gemini.ags.port"

  def extract(ctx: BundleContext): Either[String, Props] =
    for {
      host <- prop(ctx, HOST_PROPERTY).right
      pstr <- prop(ctx, PORT_PROPERTY).right
      port <- parsePort(pstr).right
    } yield Props(host, port)

  private def prop(ctx: BundleContext, name: String): Either[String, String] =
    Option(ctx.getProperty(name)).toRight("Missing '%s' property".format(name))

  private def parsePort(portStr: String): Either[String, Int] =
    if (portStr.forall(_.isDigit))
      Right(portStr.toInt)
    else
      Left("Could not parse port value '%s'".format(portStr))
}