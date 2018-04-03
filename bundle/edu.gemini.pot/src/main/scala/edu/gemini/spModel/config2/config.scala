package edu.gemini.spModel.config2

import scala.reflect.ClassTag

import scalaz._
import Scalaz._

/** A bit of syntax to simplify working with Config.
  */
trait ConfigSyntax {
  implicit final def configSyntax(c: Config): ConfigOps =
    new ConfigOps(c)
}

object ConfigSyntax extends ConfigSyntax

final class ConfigOps(val c: Config) extends AnyVal {
  private def unsafe[A](e: String \/ A): A =
    e.fold(s => sys.error(s), identity)


  def extractOption[A](k: ItemKey)(implicit ev: ClassTag[A]): String \/ Option[A] =
    Option(c.getItemValue(k)).fold(Option.empty[A].right[String]) { a =>
      \/.fromTryCatchNonFatal {
        Some(ev.runtimeClass.cast(a).asInstanceOf[A])
      }.leftMap(_ => s"Expected '$k' to have type ${ev.runtimeClass.getName}")
    }

  def extract[A](k: ItemKey)(implicit ev: ClassTag[A]): String \/ A =
    extractOption[A](k).flatMap(_.fold(s"Missing key '$k'".left[A])(a => a.right))

  def unsafeExtract[A](k: ItemKey)(implicit ev: ClassTag[A]): A =
    unsafe(extract[A](k))

  def unsafeExtractOption[A](k: ItemKey)(implicit ev: ClassTag[A]): Option[A] =
    unsafe(extractOption[A](k))


  /** Extracts and unboxes a java.lang.Integer to Int. */
  def javaInt(k: ItemKey): String \/ Int =
    extract[java.lang.Integer](k).map(_.intValue)

  /** Extracts and unboxes a java.lang.Integer to Int, throwing an exception if
    * missing or not a java.lang.Integer.
    */
  def unsafeJavaInt(k: ItemKey): Int =
    unsafe(javaInt(k))


  /** Extracts and unboxes a java.lang.Boolean to Boolean. */
  def javaBoolean(k: ItemKey): String \/ Boolean =
    extract[java.lang.Boolean](k).map(_.booleanValue)

  /** Extracts and unboxes a java.lang.Boolean to Boolean, throwing an exception
    * if missing or not a java.lang.Boolean.
    */
  def unsafeJavaBoolean(k: ItemKey): Boolean =
    unsafe(javaBoolean(k))


  /** Extracts and unboxes a java.lang.Double to Double. */
  def javaDouble(k: ItemKey): String \/ Double =
    extract[java.lang.Double](k).map(_.doubleValue)

  /** Extracts and unboxes a java.lang.Double to Double, throwing an exception
    * if missing ornot a java.lang.Double.
    */
  def unsafeJavaDouble(k: ItemKey): Double =
    unsafe(javaDouble(k))
}
