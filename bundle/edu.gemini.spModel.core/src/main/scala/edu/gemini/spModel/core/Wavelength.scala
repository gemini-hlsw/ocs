package edu.gemini.spModel.core

import squants.Length
import squants.space.Nanometers

import scalaz.{Monoid, Order}

/** Representation of wavelengths. */
final case class Wavelength(length: Length) extends AnyVal with Serializable {

  /** The wavelength as nanometers.
    * @group Conversions
    */
  def toNanometers: Double = length.toNanometers

  /** The wavelength as microns or micrometers.
    * @group Conversions
    */
  def toMicrons: Double = length.toMicrons

  /**
   * Addition.
   * @group Operations
   */
  def +(a: Wavelength): Wavelength = Wavelength(length + a.length)

  /**
   * Subtraction.
   * @group Operations
   */
  def -(a: Wavelength): Wavelength = Wavelength(length - a.length)

  /**
   * Scalar multiplication.
   * @group Operations
   */
  def *(factor: Double): Wavelength = Wavelength(length * factor)

  /**
   * Scalar division.
   * @group Operations
   */
  def /(factor: Double): Wavelength = Wavelength(length / factor)

}

object Wavelength {

  /**
   * The zero `Wavelength`.
   * @group Constructors
   */
  lazy val zero = Wavelength(Nanometers(0))

  /**
   * Additive monoid for `Wavelength`.
   * @group Typeclass Instances
   */
  implicit val WavelengthMonoid: Monoid[Wavelength] =
    new Monoid[Wavelength] {
      val zero = Wavelength.zero
      def append(a: Wavelength, b: => Wavelength): Wavelength = a + b
    }

  /** @group Typeclass Instances */
  implicit val WavelengthOrder: Order[Wavelength] =
    Order.orderBy(_.toNanometers)

  /** @group Typeclass Instances */
  implicit val WavelengthOrdering: scala.Ordering[Wavelength] =
    scala.Ordering.by(_.toNanometers)

  /** @group Implicit Conversions */
  implicit def fromLength(l: Length): Wavelength = {
    require(l.value >= 0, "wavelength must be >= 0")
    Wavelength(l)
  }
}

