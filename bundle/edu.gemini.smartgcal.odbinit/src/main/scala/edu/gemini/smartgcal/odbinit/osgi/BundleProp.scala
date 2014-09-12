package edu.gemini.smartgcal.odbinit.osgi

import org.osgi.framework.BundleContext
import java.util.logging.{Level, Logger}

// TODO: this is sort of an abomination, sorry.
// Figure out how to use Validation and do this correctly.

object BundleProp {
  private val Log = Logger.getLogger(getClass.getName)

  val toPositiveInt: String => Int = { s =>
    val i = s.toInt
    if (i >= 0) i
    else throw new IllegalArgumentException(s"Expected a positive integer, not $i")
  }
}

import BundleProp._

case class BundleProp(ctx: BundleContext) {

  private val bundleName = ctx.getBundle.getSymbolicName

  def requiredString(n: String): Either[String, String] = required(n)(identity)
  def withDefaultString(n: String)(d: => String): Either[String, String] = withDefault(n)(d)(identity)
  def optionalString(n: String): Either[String, Option[String]] = optional(n)(identity)

  def required[T](n: String)(f: String => T): Either[String, T] =
    optional(n)(f).right.flatMap(_.toRight(s"Bundle $bundleName, missing value for property '$n'"))

  def withDefault[T](n: String)(d: => T)(f: String => T): Either[String, T] =
    optional(n)(f).right map {
      _.fold {
        Log.info(s"Bundle $bundleName could not find property '$n', using default value")
        d
      } (identity)
    }

  def optional[T](n: String)(f: String => T): Either[String, Option[T]] =
    try {
      Right(Option(ctx.getProperty(n)).map(f))
    } catch {
      case ex: Exception =>
        val msg = s"Bundle $bundleName could not parse property '$n' with value '${ctx.getProperty(n)}'"
        Log.log(Level.WARNING, msg, ex)
        Left(msg)
    }
}
