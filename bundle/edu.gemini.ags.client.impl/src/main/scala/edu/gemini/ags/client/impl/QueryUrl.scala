package edu.gemini.ags.client.impl

import edu.gemini.model.p1.immutable.Observation
import java.net.{URLEncoder, URL}

/**
 * AGS service query URL factory.
 */
case class QueryUrl(host: String, port: Int) {
  require(port > 0, "illegal port " + port)

  val protocol = port match {
    case 8443 => "https"
    case 443  => "https"
    case _    => "http"
  }
  private val prefix = "%s://%s:%s/ags".format(protocol, host,port)

  private def formatArgs(args: Seq[(String, String)]): String =
    args map {
      case (name, value) => "%s=%s".format(name, URLEncoder.encode(value, "UTF-8"))
    } mkString("&")

  def format(obs:Observation, time: Long): Either[String, URL] =
    QueryArgs.all(obs, time).right map {
      args => new URL("%s?%s".format(prefix, formatArgs(args)))
    }
}